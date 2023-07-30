package org.hwyl.sexytopo.control;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import androidx.documentfile.provider.DocumentFile;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.apache.commons.text.StringEscapeUtils;

import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.io.IoUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * SexyTopo's Logger. This is a kind of proxy for the standard Android Log so we can
 * capture some of the data for in-app reporting.
 */
@SuppressWarnings("ALL")
public class Log {

    @SuppressLint("StaticFieldLeak") // only use ApplicationContext to avoid memory leak
    private static Context context;
    private static LocalBroadcastManager broadcastManager;
    private static boolean hasFilesystemAccess = false;

    private static final int MAX_DEVICE_LOG_SIZE = 100;
    private static Queue<Message> deviceLog = new LinkedList<>();

    private static final int MAX_SYSTEM_LOG_SIZE = 1000;
    private static Queue<Message> systemLog = new LinkedList<>();

    public enum LogType {
        SYSTEM,
        DEVICE
    }


    public static void setContext(Context context) {
        Log.context = context.getApplicationContext();
    }

    public static void setHasFilesystemAccess(boolean hasFilesystemAccess) {
        Log.hasFilesystemAccess = hasFilesystemAccess;
    }

    public static synchronized void device(int stringId, String ... args) {
        device(context.getString(stringId, args));
    }

    public static synchronized void device(String message) {

        if (deviceLog.size() >= MAX_DEVICE_LOG_SIZE) {
            deviceLog.remove();
        }
        deviceLog.add(new Message(message));
        broadcast(SexyTopoConstants.DEVICE_LOG_UPDATED_EVENT);
        android.util.Log.i(SexyTopoConstants.TAG, message);
    }

    public static String formatDebugString(String value) {
        return '"' + StringEscapeUtils.escapeJava(value) + '"';
    }

    public static synchronized void systemLog(String message, boolean isError) {

        while (systemLog.size() >= MAX_SYSTEM_LOG_SIZE) {
            systemLog.remove();
        }
        systemLog.add(new Message(message, isError));
        broadcast(SexyTopoConstants.SYSTEM_LOG_UPDATED_EVENT);
        save(LogType.SYSTEM);
    }

    public static void d(int stringId, Object ... args) {
        d(context.getString(stringId, args));
    }
    public static void d(String message) {
        android.util.Log.d(SexyTopoConstants.TAG, message);
        systemLog(message, false);
    }

    public static void e(int stringId, Object ... args) {
        e(context.getString(stringId, args));
    }

    public static void e(String message) {
        android.util.Log.e(SexyTopoConstants.TAG, "" + message);
        systemLog(message, true);
    }


    public static void e(Throwable throwable) {
        e("" + throwable.getMessage());
        e(android.util.Log.getStackTraceString(throwable));
    }

    public static void i(int stringId, Object ... args) {
        i(context.getString(stringId, args));
    }

    public static void i(String message) {
        android.util.Log.i(SexyTopoConstants.TAG, message);
        systemLog(message, false);
    }


    public static void broadcast(String event) {
        Intent intent = new Intent(event);

        if (context == null) {
            return; // can't broadcast yet!
        }

        if (broadcastManager == null) {
            broadcastManager = LocalBroadcastManager.getInstance(context);
        }
        broadcastManager.sendBroadcast(intent);

    }


    public static synchronized List<Message> getLog(LogType logType) {
        try {
            switch(logType) {
                case SYSTEM:
                    return new ArrayList<>(systemLog);
                case DEVICE:
                    return new ArrayList<>(deviceLog);
                default:
                    return new ArrayList<>();
            }

        } catch (Exception e) {
            return new ArrayList<>();
        }
    }


    public static void clearDeviceLog() {
        deviceLog.clear();
        broadcast(SexyTopoConstants.DEVICE_LOG_UPDATED_EVENT);
    }


    public static void save(LogType logType) {
        if (hasFilesystemAccess) {
            new SaveLogTask().execute(logType);
        }
    }

    public static void load(LogType logType) {
        try {
            DocumentFile logFile = getLogFile(logType);
            if (logFile.exists()) {
                String content = IoUtils.slurpFile(context, logFile);
                unmarshal(logType, content);
            }
        } catch (Exception exception) {
            Log.e("Error loading " + logType + ": " + exception);
        }
    }


    public static DocumentFile getLogFile(LogType logType) {
        String filename = logType.toString().toLowerCase() + ".log.json";
        File logFile = new File(context.getFilesDir(), filename);
        return DocumentFile.fromFile(logFile);
    }


    public static JSONArray marshal(LogType logType) {
        JSONArray marshalled = new JSONArray();
        List<Message> messages = getLog(logType);
        for (Message message : messages) {
            marshalled.put(message.marshal());
        }
        return marshalled;
    }


    public static void unmarshal(LogType logType, String text) throws ParseException, JSONException {
        JSONArray array = new JSONArray(text);
        Queue<Message> log = new LinkedList<>();

        for (int i = 0; i < array.length(); i++) {
            log.add(Message.unmarshal(array.getJSONObject(i)));
        }

        setLog(logType, log);

    }

    private static void setLog(LogType logType, Queue<Message> log) {
        switch(logType) {
            case SYSTEM:
                systemLog = log;
                break;
            case DEVICE:
                deviceLog = log;
                break;
        }
    }


    public final static class Message {

        @SuppressLint("SimpleDateFormat")
        private static final SimpleDateFormat FORMAT =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

        private final Date timestamp;
        private final String text;
        private final boolean isError;

        public Message(String message, boolean isError) {
            this(new Date(), message, isError);
        }

        public Message(String message) {
            this(message, false);
        }


        public Message(Date timestamp, String message, boolean isError) {
            this.text = message;
            this.timestamp = timestamp;
            this.isError = isError;
        }

        public String getText() {
            return text;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public boolean isError() {
            return isError;
        }

        public JSONObject marshal() {
            Map<String, String> map = new HashMap<>();
            map.put("timestamp", FORMAT.format(timestamp));
            map.put("isError", Boolean.toString(isError));
            map.put("text", text);
            return new JSONObject(map);
        }

        public static Message unmarshal(JSONObject object) throws ParseException, JSONException {
            Date timestamp = FORMAT.parse(object.getString("timestamp"));
            boolean isError = Boolean.parseBoolean(object.getString("isError"));
            String text = object.getString("text");
            return new Message(timestamp, text, isError);
        }
    }

    private static class SaveLogTask extends AsyncTask<LogType, Void, Void> {

        @Override
        protected Void doInBackground(LogType... logTypes) {

            if (context == null) {
                return null;
            }

            try {
                LogType logType = logTypes[0];
                JSONArray marshalled = marshal(logType);
                String content = marshalled.toString(4);
                DocumentFile logFile = getLogFile(logType);
                IoUtils.saveToFile(context, logFile, content);
            } catch (Exception exception) {
                FirebaseCrashlytics.getInstance().recordException(exception);
            }
            return null;
        }
    }


}

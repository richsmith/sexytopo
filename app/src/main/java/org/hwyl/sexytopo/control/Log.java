package org.hwyl.sexytopo.control;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.hwyl.sexytopo.SexyTopo;

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
public class Log {

    private static Context context;
    private static LocalBroadcastManager broadcastManager;

    private static final int MAX_DEVICE_LOG_SIZE = 100;
    private static Queue<Message> deviceLog = new LinkedList<>();

    private static final int MAX_SYSTEM_LOG_SIZE = 500;
    private static Queue<Message> systemLog = new LinkedList<>();

    public enum LogType {
        SYSTEM,
        DEVICE
    };


    public static void setContext(Context context) {
        Log.context = context;
    }

    public static synchronized void device(String message) {

        if (deviceLog.size() >= MAX_DEVICE_LOG_SIZE) {
            deviceLog.remove();
        }
        deviceLog.add(new Message(message));
        broadcast(SexyTopo.DEVICE_LOG_UPDATED_EVENT);
        android.util.Log.i(SexyTopo.TAG, message);
    }

    public static synchronized void systemLog(String message, boolean isError) {

        if (systemLog.size() >= MAX_SYSTEM_LOG_SIZE) {
            systemLog.remove();
        }
        systemLog.add(new Message(message, isError));
        broadcast(SexyTopo.SYSTEM_LOG_UPDATED_EVENT);
    }

    public static void d(String message) {
        android.util.Log.d(SexyTopo.TAG, message);
        systemLog(message, false);
    }

    public static void e(String message) {
        android.util.Log.e(SexyTopo.TAG, message);
        systemLog(message, true);
    }


    public static void e(Throwable throwable) {
        e(throwable.getMessage());
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
        broadcast(SexyTopo.DEVICE_LOG_UPDATED_EVENT);
    }

    public List<Map<String, String>> marshal(LogType logType) {
        List<Map<String, String>> marshalled = new LinkedList<>();
        List<Message> messages = getLog(logType);
        for (Message message : messages) {
            marshalled.add(message.marshal());
        }
        return marshalled;
    }

    public void unmarshal(LogType logType, List<Map<String, String>> data) {
        Queue<Message> log = new LinkedList<>();
        for (Map<String, String> entry : data) {
            //log.add(Message.unmarshal(entry));
        }

        switch(logType) {
            case SYSTEM:
                systemLog = log;
            case DEVICE:
                deviceLog = log;
        }
    }


    public final static class Message {

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

        public Map<String, String> marshal() {
            Map<String, String> map = new HashMap<>();
            map.put("timestamp", timestamp.toString());
            map.put("isError", Boolean.toString(isError));
            map.put("text", text);
            return map;
        }

        public static Message unmarshal(Map<String, String> map) throws ParseException{
            Date timestamp = FORMAT.parse(map.get("timestamp"));
            boolean isError = Boolean.valueOf(map.get("isError"));
            String text = map.get("text");
            return new Message(timestamp, text, isError);
        }
    }

}

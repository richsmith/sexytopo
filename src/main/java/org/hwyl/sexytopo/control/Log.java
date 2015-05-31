package org.hwyl.sexytopo.control;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.hwyl.sexytopo.SexyTopo;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
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

    public static void d(String message) {
        android.util.Log.d(SexyTopo.TAG, message);
    }

    @Deprecated
    public static void d(String tag, String message) {
        android.util.Log.d(SexyTopo.TAG, message);
    }

    public static void e(String message) {
        android.util.Log.e(SexyTopo.TAG, message);
    }

    @Deprecated
    public static void e(String tag, String message) {
        android.util.Log.e(SexyTopo.TAG, message);
    }

    public static void broadcast(String event) {
        Intent intent = new Intent(event);

        if (broadcastManager == null) {
            broadcastManager = LocalBroadcastManager.getInstance(context);
        }
        broadcastManager.sendBroadcast(intent);

    }

    public static synchronized List<Message> getDeviceLog() {


        try {
            return new ArrayList<>(deviceLog);
            //return (List)deviceLog;
        } catch (Exception e) {
            int ignore = 0;
            return new ArrayList<>();
        }
    }

    public static void clearDeviceLog() {
        deviceLog.clear();
        broadcast(SexyTopo.DEVICE_LOG_UPDATED_EVENT);
    }


    public final static class Message {
        private final String text;
        private final Date timestamp;
        public Message(String message) {
            this.text = message;
            this.timestamp = new Date();
        }

        public String getText() {
            return text;
        }

        public Date getTimestamp() {
            return timestamp;
        }

    }



}

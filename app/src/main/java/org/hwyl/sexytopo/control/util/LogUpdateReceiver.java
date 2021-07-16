package org.hwyl.sexytopo.control.util;

import android.annotation.SuppressLint;
import android.text.Html;
import android.widget.ScrollView;
import android.widget.TextView;

import org.hwyl.sexytopo.control.Log;

import java.text.SimpleDateFormat;


public class LogUpdateReceiver {

    @SuppressLint("SimpleDateFormat")
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("HH:mm");

    public static void update(Log.LogType logType, ScrollView scrollView, TextView logView) {
        String text = buildLogText(logType);
        logView.setText(Html.fromHtml(text));
    }

    private static String buildLogText(Log.LogType logType) {
        StringBuilder logText = new StringBuilder();
        for (Log.Message message : Log.getLog(logType)) {
            if (message.isError()) {
                logText.append("<font color='red'>");
            }
            logText.append(TIMESTAMP_FORMAT.format(message.getTimestamp()));
            logText.append(" ");
            logText.append(message.getText());
            if (message.isError()) {
                logText.append("</font>");
            }
            logText.append("<br/>");
        }

        return logText.toString();
    }


}

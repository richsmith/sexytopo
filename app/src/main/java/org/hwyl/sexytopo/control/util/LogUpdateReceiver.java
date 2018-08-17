package org.hwyl.sexytopo.control.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.widget.ScrollView;
import android.widget.TextView;

import org.hwyl.sexytopo.control.Log;

import java.text.SimpleDateFormat;
import java.util.List;


public class LogUpdateReceiver extends BroadcastReceiver {

    private final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("HH:mm");

    private ScrollView scrollView;
    private TextView logView;
    private Log.LogType logType;

    public LogUpdateReceiver(ScrollView scrollView, TextView logView, Log.LogType logType) {
        this.scrollView = scrollView;
        this.logView = logView;
        this.logType = logType;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        update();
    }

    public void update() {
        List<Log.Message> messages = Log.getLog(this.logType);
        String text = buildLogText(messages);
        logView.setText(Html.fromHtml(text));
        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
        scrollView.invalidate();

    }

    private String buildLogText(List<Log.Message> messages) {
        StringBuilder logText = new StringBuilder();
        for (Log.Message message : Log.getLog(logType)) {
            String timestamp = TIMESTAMP_FORMAT.format(message.getTimestamp());
            String row = timestamp + " " + message.getText();
            if (message.isError()) {
                logText.append("<font color='red'>");
                logText.append(row);
                logText.append("</font>");
            } else {
                logText.append(row);
            }
            logText.append("<br/>");
        }

        return logText.toString();
    }


}

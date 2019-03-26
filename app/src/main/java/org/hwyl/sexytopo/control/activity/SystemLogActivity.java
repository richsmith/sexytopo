package org.hwyl.sexytopo.control.activity;

import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.ScrollView;
import android.widget.TextView;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.util.LogUpdateReceiver;


public class SystemLogActivity extends SexyTopoActivity {

    private LogUpdateReceiver logUpdateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_debug_log);

        setupLogView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (logUpdateReceiver != null) {
            logUpdateReceiver.update();
        }
    }

    private void setupLogView() {
        if (logUpdateReceiver == null) {
            TextView logView = findViewById(R.id.debugLog);
            ScrollView scrollView = findViewById(R.id.scrollView);
            logUpdateReceiver = new LogUpdateReceiver(scrollView, logView, Log.LogType.SYSTEM);

            LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
            IntentFilter logFilter = new IntentFilter();
            logFilter.addAction(SexyTopo.SYSTEM_LOG_UPDATED_EVENT);
            broadcastManager.registerReceiver(logUpdateReceiver, logFilter);
        }
    }
}

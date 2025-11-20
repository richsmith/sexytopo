package org.hwyl.sexytopo.control.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.util.LogUpdateReceiver;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public class SystemLogActivity extends SexyTopoActivity {

    private SystemLogUpdateReceiver logUpdateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_debug_log);
        setupMaterialToolbar();

        applyEdgeToEdgeInsets(R.id.rootLayout, true, true);

        logUpdateReceiver = new SystemLogUpdateReceiver();
    }


    @Override
    protected void onResume() {

        super.onResume();

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter logFilter = new IntentFilter();
        logFilter.addAction(SexyTopoConstants.SYSTEM_LOG_UPDATED_EVENT);
        broadcastManager.registerReceiver(logUpdateReceiver, logFilter);
        logUpdateReceiver.update();
    }


    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.unregisterReceiver(logUpdateReceiver);
    }


    private class SystemLogUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            update();
        }

        public void update() {
            TextView logView = findViewById(R.id.logText);
            LogUpdateReceiver.update(Log.LogType.SYSTEM, null, logView);
        }
    }

}

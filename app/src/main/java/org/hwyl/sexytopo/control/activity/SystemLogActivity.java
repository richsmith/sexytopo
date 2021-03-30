package org.hwyl.sexytopo.control.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.widget.ScrollView;
import android.widget.TextView;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.util.LogUpdateReceiver;


public class SystemLogActivity extends SexyTopoActivity {

    private SystemLogUpdateReceiver logUpdateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_debug_log);

        logUpdateReceiver = new SystemLogUpdateReceiver();
    }


    @Override
    protected void onResume() {

        super.onResume();

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter logFilter = new IntentFilter();
        logFilter.addAction(SexyTopo.SYSTEM_LOG_UPDATED_EVENT);
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
            TextView logView = findViewById(R.id.debugLog);
            final ScrollView scrollView = findViewById(R.id.scrollView);
            LogUpdateReceiver.update(Log.LogType.SYSTEM, scrollView, logView);
            scrollView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            },1000);
        }
    }

}

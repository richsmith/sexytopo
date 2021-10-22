package org.hwyl.sexytopo.control;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Vibrator;


import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.util.PreferenceHelper;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class NewStationNotificationService extends Service {

    public static final int VIBRATE_FOR_MS = 200;

    BroadcastReceiver receiver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                update();
            }
        };

        try {
            LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
            broadcastManager.registerReceiver(
                    receiver, new IntentFilter(SexyTopo.NEW_STATION_CREATED_EVENT));
        } catch (Exception exception) {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.recordException(exception);
            Log.e(exception);
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.unregisterReceiver(receiver);
    }


    private void update() {
        if (PreferenceHelper.vibrateOnNewStation()) {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.vibrate(VIBRATE_FOR_MS);
            }
        }
    }

}

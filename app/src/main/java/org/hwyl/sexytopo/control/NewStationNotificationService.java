package org.hwyl.sexytopo.control;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Vibrator;

import org.hwyl.sexytopo.SexyTopo;

public class NewStationNotificationService extends Service {

    public static final int VIBRATE_FOR_MS = 200;


    public NewStationNotificationService() {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                update();
            }
        };
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(receiver, new IntentFilter(SexyTopo.NEW_STATION_CREATED_EVENT));
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void update() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean vibrateOnNewStation =
                preferences.getBoolean("pref_vibrate_on_new_station", false);

        if (vibrateOnNewStation) {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_FOR_MS);
        }
    }

}

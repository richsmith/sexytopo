package org.hwyl.sexytopo.control;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import org.hwyl.sexytopo.SexyTopo;

public class AudioService extends Service {

    private MediaPlayer mediaPlayer;

    public AudioService() {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                update();
            }
        };
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(receiver, new IntentFilter(SexyTopo.SURVEY_UPDATED_EVENT));

        //mediaPlayer
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void update() {

    }


    public void onPrepared(MediaPlayer player) {
        player.start();
    }

}

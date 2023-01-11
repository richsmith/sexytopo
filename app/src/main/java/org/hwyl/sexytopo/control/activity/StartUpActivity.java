package org.hwyl.sexytopo.control.activity;

import android.content.Intent;
import android.os.Bundle;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.NewStationNotificationService;


public class StartUpActivity extends SexyTopoActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hasStarted = true;

        setContentView(R.layout.activity_start_up);

        SexyTopo.context = this.getApplicationContext();

        Log.setContext(this.getApplicationContext());
        Log.load(Log.LogType.SYSTEM);
        Log.i("--------------------");
        Log.i("Starting up");

        try {
            Intent serviceIntent = new Intent(this, NewStationNotificationService.class);
            startService(serviceIntent);

        } catch (Exception exception) {
            // Not mission-critical, so carry on if this doesn't work
            // (some users seem to have had problems according to the log)
            Log.e("Unable to start new station notification intent");
            Log.e(exception);
        }

        initialiseData();

        Intent intent = new Intent(this, DeviceActivity.class);
        startActivity(intent);
    }











}

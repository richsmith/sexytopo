package org.hwyl.sexytopo.comms.sap6;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.comms.Communicator;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.activity.DeviceActivity;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.model.survey.Leg;

import java.util.HashMap;
import java.util.Map;

import kotlin.Unit;

public class SAP6Communicator implements Communicator {

    private final DeviceActivity activity;

    private final CaveBLE caveBLE;

    private final SurveyManager datamanager;

    private boolean userRequestedDisconnect = false;
    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());
    private static final long RECONNECT_DELAY_MS = 3000;

    private static final int START_CALIBRATION_ID = View.generateViewId();
    private static final int STOP_CALIBRATION_ID = View.generateViewId();
    private static final int LASER_ON_ID = View.generateViewId();
    private static final int SHOT_ID = View.generateViewId();
    private static final int LASER_OFF_ID = View.generateViewId();
    private static final int DEVICE_OFF_ID = View.generateViewId();


    private static final Map<Integer, Integer> CUSTOM_COMMANDS = new HashMap<>();

    static {
        CUSTOM_COMMANDS.put(START_CALIBRATION_ID, R.string.device_sap_command_calibration_start);
        CUSTOM_COMMANDS.put(STOP_CALIBRATION_ID, R.string.device_sap_command_calibration_stop);
        CUSTOM_COMMANDS.put(LASER_ON_ID, R.string.device_sap_command_laser_on);
        CUSTOM_COMMANDS.put(SHOT_ID, R.string.device_sap_command_take_shot);
        CUSTOM_COMMANDS.put(LASER_OFF_ID, R.string.device_sap_command_laser_off);
        CUSTOM_COMMANDS.put(DEVICE_OFF_ID, R.string.device_sap_command_device_off);
    }

    public SAP6Communicator(DeviceActivity activity, BluetoothDevice bluetoothDevice) {
        this.activity = activity;
        this.caveBLE = new CaveBLE(bluetoothDevice, activity, this::legCallback, this::statusCallback);
        this.datamanager = activity.getSurveyManager();
    }

    @Override
    public boolean isConnected() {
        return caveBLE.isConnected();
    }

    @Override
    public void requestConnect() {
        userRequestedDisconnect = false;
        caveBLE.connect();
    }

    @Override
    public void requestDisconnect() {
        userRequestedDisconnect = true;
        reconnectHandler.removeCallbacksAndMessages(null);
        caveBLE.disconnect();
    }

    @Override
    public void forceStop() {
        reconnectHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public Map<Integer, Integer> getCustomCommands() {
        return CUSTOM_COMMANDS;
    }

    @Override
    public boolean handleCustomCommand(int viewId) {
        if (viewId == START_CALIBRATION_ID) {
            caveBLE.startCal();
            return true;
        } else if (viewId == LASER_ON_ID) {
            caveBLE.laserOn();
            return true;
        } else if (viewId == LASER_OFF_ID) {
            caveBLE.laserOff();
            return true;
        } else if (viewId == SHOT_ID) {
            caveBLE.takeShot();
            return true;
        } else if (viewId == DEVICE_OFF_ID) {
            caveBLE.deviceOff();
            return true;
        } else {
            return false;
        }
    }

    public Unit legCallback(float azimuth, float inclination, float roll, float distance) {
        Log.device(String.format("Leg received: %05.1f %+04.1f %.3fm", azimuth, inclination, distance));
        Leg leg = new Leg(distance, azimuth, inclination);
        datamanager.updateSurvey(leg);
        return Unit.INSTANCE;
    }

    public Unit statusCallback(int status, String msg) {
        switch (status) {
            case CaveBLE.CONNECTED:
                Log.device("Connected");
                break;
            case CaveBLE.DISCONNECTED:
                Log.device("Disconnected");
                activity.runOnUiThread(activity::updateConnectionStatus);
                if (!userRequestedDisconnect && GeneralPreferences.isAutoReconnectOn()) {
                    Log.device(R.string.device_ble_auto_reconnecting, "SAP6");
                    reconnectHandler.postDelayed(this::requestConnect, RECONNECT_DELAY_MS);
                }
                break;
            case CaveBLE.CONNECTION_FAILED:
                Log.device("Communication error: "+msg);
                activity.runOnUiThread(activity::updateConnectionStatus);
                if (!userRequestedDisconnect && GeneralPreferences.isAutoReconnectOn()) {
                    Log.device(R.string.device_ble_auto_reconnecting, "SAP6");
                    reconnectHandler.postDelayed(this::requestConnect, RECONNECT_DELAY_MS);
                }
                break;
        }
        return Unit.INSTANCE;
    }
}

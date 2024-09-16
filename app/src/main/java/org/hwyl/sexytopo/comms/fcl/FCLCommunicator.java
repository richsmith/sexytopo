package org.hwyl.sexytopo.comms.fcl;

import android.bluetooth.BluetoothDevice;
import android.view.View;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.comms.Communicator;
import org.hwyl.sexytopo.comms.fcl.FCLBLE;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.activity.DeviceActivity;
import org.hwyl.sexytopo.model.survey.Leg;

import java.util.HashMap;
import java.util.Map;

import kotlin.Unit;

public class FCLCommunicator implements Communicator {

    private final DeviceActivity activity;

    private final FCLBLE fclBLE;

    private final SurveyManager datamanager;

    private static final int START_CALIBRATION_ID = View.generateViewId();
    private static final int STOP_CALIBRATION_ID = View.generateViewId();
    private static final int LASER_ON_ID = View.generateViewId();
    private static final int SHOT_ID = View.generateViewId();
    private static final int LASER_OFF_ID = View.generateViewId();
    private static final int DEVICE_OFF_ID = View.generateViewId();


    private static final Map<Integer, Integer> CUSTOM_COMMANDS = new HashMap<>();

    static {
        CUSTOM_COMMANDS.put(LASER_ON_ID, R.string.device_sap_command_laser_on);
        CUSTOM_COMMANDS.put(SHOT_ID, R.string.device_sap_command_take_shot);
        CUSTOM_COMMANDS.put(LASER_OFF_ID, R.string.device_sap_command_laser_off);
        CUSTOM_COMMANDS.put(DEVICE_OFF_ID, R.string.device_sap_command_device_off);
    }

    public FCLCommunicator(DeviceActivity activity, BluetoothDevice bluetoothDevice) {
        this.activity = activity;
        this.fclBLE = new FCLBLE(bluetoothDevice, activity, this::legCallback, this::statusCallback);
        this.datamanager = activity.getSurveyManager();
    }

    @Override
    public boolean isConnected() {
        return fclBLE.isConnected();
    }

    @Override
    public void requestConnect() {
        fclBLE.connect();
    }

    @Override
    public void requestDisconnect() {
        fclBLE.disconnect();
    }

    @Override
    public Map<Integer, Integer> getCustomCommands() {
        return CUSTOM_COMMANDS;
    }

    @Override
    public boolean handleCustomCommand(int viewId) {
        if (viewId == LASER_ON_ID) {
            fclBLE.laserOn();
            return true;
        } else if (viewId == LASER_OFF_ID) {
            fclBLE.laserOff();
            return true;
        } else if (viewId == SHOT_ID) {
            fclBLE.takeShot();
            return true;
        } else if (viewId == DEVICE_OFF_ID) {
            fclBLE.deviceOff();
            return true;
        } else {
            return false;
        }
    }

    public Unit legCallback(float azimuth, float inclination, float distance) {
        Log.device(String.format("Leg received: %05.1f %+04.1f %.3fm", azimuth, inclination, distance));
        Leg leg = new Leg(distance, azimuth, inclination);
        datamanager.updateSurvey(leg);
        return Unit.INSTANCE;
    }

    public Unit statusCallback(int status, String msg) {
        switch (status) {
            case FCLBLE.CONNECTED:
                Log.device(R.string.device_connection_connected);
                break;
            case FCLBLE.DISCONNECTED:
                Log.device(R.string.device_connection_closed);
                activity.setConnectionStopped();
                activity.updateConnectionStatus();
                break;
            case FCLBLE.CONNECTION_FAILED:
                Log.device(R.string.connection_error, msg);
                activity.setConnectionStopped();
                activity.updateConnectionStatus();
        }
        return Unit.INSTANCE;
    }
}

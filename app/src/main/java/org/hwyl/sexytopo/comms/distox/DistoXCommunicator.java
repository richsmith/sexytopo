
package org.hwyl.sexytopo.comms.distox;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.view.View;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.activity.DeviceActivity;
import org.hwyl.sexytopo.control.activity.DistoXCalibrationActivity;
import org.hwyl.sexytopo.control.activity.SexyTopoActivity;

import java.util.HashMap;
import java.util.Map;


public class DistoXCommunicator implements DistoXStyleCommunicator {

    private final DeviceActivity activity;
    private final BluetoothDevice bluetoothDevice;

    private static final int CALIBRATION_ID = View.generateViewId();
    private static final int LASER_ON_ID = View.generateViewId();
    private static final int SHOT_ID = View.generateViewId();
    private static final int LASER_OFF_ID = View.generateViewId();
    private static final int DISTO_X_OFF_ID = View.generateViewId();


    private static final Map<Integer, Integer> CUSTOM_COMMANDS = new HashMap<>();

    static {
        CUSTOM_COMMANDS.put(CALIBRATION_ID, R.string.device_distox_command_calibration);
        CUSTOM_COMMANDS.put(LASER_ON_ID, R.string.device_command_laser_on);
        CUSTOM_COMMANDS.put(SHOT_ID, R.string.device_command_take_shot);
        CUSTOM_COMMANDS.put(LASER_OFF_ID, R.string.device_command_laser_off);
        CUSTOM_COMMANDS.put(DISTO_X_OFF_ID, R.string.device_command_device_off);
    }

    protected DistoXThread thread;

    public DistoXCommunicator(
            DeviceActivity activity, BluetoothDevice bluetoothDevice) {
        this.activity = activity;
        this.bluetoothDevice = bluetoothDevice;

    }

    @Override
    public boolean isConnected() {
        return thread != null && thread.isAlive() && thread.isConnected();
    }

    @Override
    public void requestConnect() {

        if (thread == null || thread.getState() == Thread.State.TERMINATED) {
            thread = getNewCommsThread(activity);
        }

        Thread.State commsState = thread.getState();
        if (commsState == Thread.State.NEW) {
           thread.requestStart(DistoXThread.Mode.MEASUREMENT);
       } else {
           thread.setMode(DistoXThread.Mode.MEASUREMENT);
       }
    }

    protected DistoXThread getNewCommsThread(SexyTopoActivity activity) {
        return new DistoXThread(activity, activity.getSurveyManager());
    }

    @Override
    public void requestDisconnect() {
        thread.requestStop();
    }

    @Override
    public void forceStop() {
        //noinspection deprecation
        thread.stop();
    }

    @Override
    public Map<Integer, Integer> getCustomCommands() {
        return CUSTOM_COMMANDS;
    }

    @Override
    public boolean handleCustomCommand(int eventId) {
        // can't use a switch statement here because the IDs are not known at compile time
        if (eventId == CALIBRATION_ID) {
            startCalibrationActivity();
            return true;
        } else if (eventId == LASER_ON_ID) {
            thread.laserOn();
            return true;
        } else if (eventId == SHOT_ID) {
            thread.takeShot();
            return true;
        } else if (eventId == LASER_OFF_ID) {
            thread.laserOff();
            return true;
        } else if (eventId == DISTO_X_OFF_ID) {
            thread.distoXOff();
            return true;
        } else {
            return false;
        }
    }

    private void startCalibrationActivity() {
        Intent intent = new Intent(activity, DistoXCalibrationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(intent);
    }

    public void startCalibration() {
        thread.startCalibration();
    }

    public void stopCalibration() {
        thread.stopCalibration();
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public WriteCalibrationProtocol writeCalibration(Byte[] coeffs) {
        WriteCalibrationProtocol writeCalibrationProtocol = thread.writeCalibration(coeffs);
        return writeCalibrationProtocol;
    }



}
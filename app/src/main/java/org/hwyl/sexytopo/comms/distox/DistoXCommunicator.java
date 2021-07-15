
package org.hwyl.sexytopo.comms.distox;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.view.View;

import org.hwyl.sexytopo.comms.Communicator;
import org.hwyl.sexytopo.control.activity.CalibrationActivity;
import org.hwyl.sexytopo.control.activity.DeviceActivity;
import org.hwyl.sexytopo.control.activity.SexyTopoActivity;

import java.util.HashMap;
import java.util.Map;


public class DistoXCommunicator extends Communicator {

    private final DeviceActivity activity;
    private final BluetoothDevice bluetoothDevice;

    private static final int CALIBRATION_ID = View.generateViewId();
    private static final int LASER_ON_ID = View.generateViewId();
    private static final int SHOT_ID = View.generateViewId();
    private static final int LASER_OFF_ID = View.generateViewId();
    private static final int DISTO_X_OFF_ID = View.generateViewId();


    private static final Map<Integer, String> CUSTOM_COMMANDS = new HashMap<>();

    static {
        CUSTOM_COMMANDS.put(CALIBRATION_ID, "Calibration…");
        CUSTOM_COMMANDS.put(LASER_ON_ID, "Laser on");
        CUSTOM_COMMANDS.put(SHOT_ID, "Take shot");
        CUSTOM_COMMANDS.put(LASER_OFF_ID, "Laser off");
        CUSTOM_COMMANDS.put(DISTO_X_OFF_ID, "DistoX off");
    }

    public enum DistoXType {
        A3(false),
        X310(true);

        public final boolean preferNonLinearCalibration;

        DistoXType(boolean preferNonLinearCalibration) {
            this.preferNonLinearCalibration = preferNonLinearCalibration;
        }
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
    public Map<Integer, String> getCustomCommands() {
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
        Intent intent = new Intent(activity, CalibrationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(intent);
    }

    public void startCalibration() {
        thread.startCalibration();
    }

    public void stopCalibration() {
        thread.stopCalibration();
    }

    public WriteCalibrationProtocol writeCalibration(byte[] coeffs) {
        WriteCalibrationProtocol writeCalibrationProtocol = thread.writeCalibration(coeffs);
        return writeCalibrationProtocol;
    }

    public boolean doesCurrentDistoPreferNonLinearCalibration() {
        String name = bluetoothDevice.getName();
        if (name.startsWith("DistoX-")) {
            return DistoXType.X310.preferNonLinearCalibration;
        } else if (name.startsWith("DistoX")) {
            return DistoXType.A3.preferNonLinearCalibration;
        } else {
            return false; // shouldn't get here but linear is safer as default?
        }
    }


}
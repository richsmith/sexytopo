
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

    private final SexyTopoActivity activity;
    private final BluetoothDevice bluetoothDevice;

    public static final int CALIBRATION_ID = View.generateViewId();

    private static final Map<Integer, String> CUSTOM_COMMANDS = new HashMap<>();

    static {
        CUSTOM_COMMANDS.put(CALIBRATION_ID, "Calibration");
    }

    public enum DistoXType {
        A3(false),
        X310(true);

        public final boolean preferNonLinearCalibration;

        DistoXType(boolean preferNonLinearCalibration) {
            this.preferNonLinearCalibration = preferNonLinearCalibration;
        }
    }

    private DistoXThread thread;

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
            thread = new DistoXThread(activity);
        }

        Thread.State commsState = thread.getState();
        if (commsState == Thread.State.NEW) {
           thread.requestStart(DistoXThread.Protocol.MEASUREMENT);
       } else {
           thread.setProtocol(DistoXThread.Protocol.MEASUREMENT);
       }
    }

    @Override
    public void requestDisconnect() {
        thread.requestStop();
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
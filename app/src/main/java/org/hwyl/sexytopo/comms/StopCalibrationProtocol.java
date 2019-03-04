package org.hwyl.sexytopo.comms;

import android.bluetooth.BluetoothDevice;

import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.activity.SexyTopoActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;


public class StopCalibrationProtocol extends DistoXProtocol {

    private static final byte STOP_CALIBRATION = 0b00110000;

    public StopCalibrationProtocol(
            SexyTopoActivity activity, BluetoothDevice bluetoothDevice, SurveyManager dataManager) {
        super(activity, bluetoothDevice, dataManager);
    }

    @Override
    public void go(DataInputStream inStream, DataOutputStream outStream) throws Exception {
        byte[] commandPacket = new byte[] {STOP_CALIBRATION};
        writeCommandPacket(outStream, commandPacket);
    }


}

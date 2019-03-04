package org.hwyl.sexytopo.comms;

import android.bluetooth.BluetoothDevice;

import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.activity.SexyTopoActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;


public class StartCalibrationProtocol extends DistoXProtocol {


    private static final byte START_CALIBRATION = 0b00110001;

    public StartCalibrationProtocol(
            SexyTopoActivity activity, BluetoothDevice bluetoothDevice, SurveyManager dataManager) {
        super(activity, bluetoothDevice, dataManager);
    }

    @Override
    public void go(DataInputStream inStream, DataOutputStream outStream) throws Exception {
        byte[] commandPacket = new byte[] {START_CALIBRATION};
        writeCommandPacket(outStream, commandPacket);
    }


}

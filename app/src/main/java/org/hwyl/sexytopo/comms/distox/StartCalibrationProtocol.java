package org.hwyl.sexytopo.comms.distox;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;


public class StartCalibrationProtocol extends DistoXProtocol {


    private static final byte START_CALIBRATION = 0b00110001;

    public StartCalibrationProtocol(
            Context context, BluetoothDevice bluetoothDevice, SurveyManager dataManager) {
        super(context, bluetoothDevice, dataManager);
    }

    @Override
    public void go(DataInputStream inStream, DataOutputStream outStream) throws Exception {
        Log.device("Writing start calibration command packet");
        byte[] commandPacket = new byte[] {START_CALIBRATION};
        writeCommandPacket(outStream, commandPacket);
    }


}

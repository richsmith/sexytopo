package org.hwyl.sexytopo.comms.distox;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;


public class StopCalibrationProtocol extends DistoXProtocol {

    private static final byte STOP_CALIBRATION = 0b00110000;

    public StopCalibrationProtocol(
            Context context, BluetoothDevice bluetoothDevice, SurveyManager dataManager) {
        super(context, bluetoothDevice, dataManager);
    }

    @Override
    public void go(DataInputStream inStream, DataOutputStream outStream) throws Exception {
        Log.device("Writing stop calibration command packet");
        byte[] commandPacket = new byte[] {STOP_CALIBRATION};
        writeCommandPacket(outStream, commandPacket);
    }


}

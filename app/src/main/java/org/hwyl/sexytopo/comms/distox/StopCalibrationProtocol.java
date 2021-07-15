package org.hwyl.sexytopo.comms.distox;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import org.hwyl.sexytopo.control.SurveyManager;


public class StopCalibrationProtocol extends CommandProtocol {

    private static final byte STOP_CALIBRATION = 0b00110000;

    public StopCalibrationProtocol(
            Context context, BluetoothDevice bluetoothDevice, SurveyManager dataManager) {
        super(context, bluetoothDevice, dataManager, STOP_CALIBRATION);
    }

}

package org.hwyl.sexytopo.comms.distox;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import org.hwyl.sexytopo.control.SurveyManager;


public class StartCalibrationProtocol extends CommandProtocol {


    private static final byte START_CALIBRATION = 0b00110001;

    public StartCalibrationProtocol(
            Context context, BluetoothDevice bluetoothDevice, SurveyManager dataManager) {
        super(context, bluetoothDevice, dataManager, START_CALIBRATION);
    }

}

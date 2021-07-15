package org.hwyl.sexytopo.comms.distox;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import org.hwyl.sexytopo.control.SurveyManager;


public class LaserOnProtocol extends CommandProtocol {

    private static final byte LASER_ON = 0x36;

    public LaserOnProtocol(
            Context context, BluetoothDevice bluetoothDevice, SurveyManager dataManager) {
        super(context, bluetoothDevice, dataManager, LASER_ON);
    }

}

package org.hwyl.sexytopo.comms.distox;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import org.hwyl.sexytopo.control.SurveyManager;


public class LaserOffProtocol extends CommandProtocol {

    private static final byte LASER_OFF = 0x37;

    public LaserOffProtocol(
            Context context, BluetoothDevice bluetoothDevice, SurveyManager dataManager) {
        super(context, bluetoothDevice, dataManager, LASER_OFF);
    }

}

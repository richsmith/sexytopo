package org.hwyl.sexytopo.comms.distox;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import org.hwyl.sexytopo.control.SurveyManager;


public class TakeShotProtocol extends CommandProtocol {

    private static final byte TAKE_SHOT = 0x38;

    public TakeShotProtocol(
            Context context, BluetoothDevice bluetoothDevice, SurveyManager dataManager) {
        super(context, bluetoothDevice, dataManager, TAKE_SHOT);
    }

}

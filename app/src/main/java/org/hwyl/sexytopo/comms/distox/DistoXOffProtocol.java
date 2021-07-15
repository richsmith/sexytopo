package org.hwyl.sexytopo.comms.distox;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import org.hwyl.sexytopo.control.SurveyManager;


public class DistoXOffProtocol extends CommandProtocol {

    private static final byte DISTO_OFF = 0x34;

    public DistoXOffProtocol(
            Context context, BluetoothDevice bluetoothDevice, SurveyManager dataManager) {
        super(context, bluetoothDevice, dataManager, DISTO_OFF);
    }

}

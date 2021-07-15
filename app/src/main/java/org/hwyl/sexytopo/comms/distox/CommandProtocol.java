package org.hwyl.sexytopo.comms.distox;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;


public class CommandProtocol extends DistoXProtocol {


    private final byte command;

    public CommandProtocol(
            Context context, BluetoothDevice bluetoothDevice, SurveyManager dataManager,
            byte command) {
        super(context, bluetoothDevice, dataManager);
        this.command = command;
    }

    @Override
    public void go(DataInputStream inStream, DataOutputStream outStream) throws Exception {
        Log.device("Writing command packet");
        byte[] commandPacket = new byte[] {command};
        writeCommandPacket(outStream, commandPacket);
    }


}

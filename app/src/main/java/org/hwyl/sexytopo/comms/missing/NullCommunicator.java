
package org.hwyl.sexytopo.comms.missing;

import android.bluetooth.BluetoothDevice;

import org.hwyl.sexytopo.comms.Communicator;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.activity.DeviceActivity;


public class NullCommunicator extends Communicator {

    private final static NullCommunicator INSTANCE = new NullCommunicator(null, null);

    public NullCommunicator(DeviceActivity activity, BluetoothDevice bluetoothDevice) {
        // do nothing
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void requestConnect() {
        Log.device("No communicator so can't do anything");
        // do nothing
    }

    @Override
    public void requestDisconnect() {
        // do nothing
    }

    public static NullCommunicator getInstance() {
        return INSTANCE;
    }
}
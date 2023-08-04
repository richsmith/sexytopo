
package org.hwyl.sexytopo.comms.bric4;

import android.bluetooth.BluetoothDevice;

import org.hwyl.sexytopo.comms.ble.BleCommunicator;
import org.hwyl.sexytopo.control.activity.DeviceActivity;


public class Bric4Communicator extends BleCommunicator {

    public Bric4Communicator(
            DeviceActivity activity, BluetoothDevice bluetoothDevice) {

        super(activity, bluetoothDevice, new Bric4Manager(activity, activity.getSurveyManager()));
    }

}
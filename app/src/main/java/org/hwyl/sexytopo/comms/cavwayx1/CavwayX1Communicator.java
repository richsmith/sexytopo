package org.hwyl.sexytopo.comms.cavwayx1;

import android.bluetooth.BluetoothDevice;

import org.hwyl.sexytopo.comms.ble.BleCommunicator;
import org.hwyl.sexytopo.control.activity.DeviceActivity;

public class CavwayX1Communicator extends BleCommunicator {

    public CavwayX1Communicator(
            DeviceActivity activity, BluetoothDevice bluetoothDevice) {
        super(
            activity, bluetoothDevice, new CavwayX1Manager(activity, activity.getSurveyManager()));
    }
}

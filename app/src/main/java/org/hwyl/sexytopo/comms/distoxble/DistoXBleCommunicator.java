
package org.hwyl.sexytopo.comms.distoxble;

import android.bluetooth.BluetoothDevice;

import org.hwyl.sexytopo.comms.ble.BleCommunicator;
import org.hwyl.sexytopo.comms.distox.DistoXStyleCommunicator;
import org.hwyl.sexytopo.comms.distox.WriteCalibrationProtocol;
import org.hwyl.sexytopo.control.activity.DeviceActivity;


public class DistoXBleCommunicator extends BleCommunicator implements DistoXStyleCommunicator {

    public DistoXBleCommunicator(
            DeviceActivity activity, BluetoothDevice bluetoothDevice) {

        super(
            activity, bluetoothDevice, new DistoXBleManager(activity, activity.getSurveyManager()));
    }

    private DistoXBleManager getManager() {
        return (DistoXBleManager) manager;
    }

    @Override
    public void startCalibration() {
        getManager().startCalibration();
    }

    @Override
    public void stopCalibration() {
        getManager().stopCalibration();
    }

    @Override
    public WriteCalibrationProtocol writeCalibration(Byte... bytes) {
        return getManager().writeCalibration(bytes);
    }
}

package org.hwyl.sexytopo.comms.sap5;

import android.bluetooth.BluetoothDevice;

import org.hwyl.sexytopo.comms.Communicator;
import org.hwyl.sexytopo.control.activity.DeviceActivity;


public class Sap5Communicator extends Communicator {

    private final DeviceActivity activity;

    private OldDistoXCommunicator oldDistoXCommunicator;

    public Sap5Communicator(DeviceActivity activity, BluetoothDevice ignored) {
        this.activity = activity;
    }


    @Override
    public boolean isConnected() {
        return oldDistoXCommunicator != null && oldDistoXCommunicator.isConnected();
    }


    @Override
    public void requestConnect() {
        oldDistoXCommunicator = new OldDistoXCommunicator(
                activity, activity.getSurveyManager());
        oldDistoXCommunicator.requestStart(OldDistoXCommunicator.Protocol.MEASUREMENT);
    }


    @Override
    public void requestDisconnect() {
        if (oldDistoXCommunicator != null) {
            oldDistoXCommunicator.requestStop();
        }
    }

    @Override
    public void forceStop() {
        //noinspection deprecation
        oldDistoXCommunicator.stop();
    }

}
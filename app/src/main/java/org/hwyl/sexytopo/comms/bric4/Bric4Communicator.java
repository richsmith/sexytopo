
package org.hwyl.sexytopo.comms.bric4;

import android.bluetooth.BluetoothDevice;

import org.hwyl.sexytopo.comms.Communicator;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.activity.DeviceActivity;

import androidx.annotation.NonNull;
import no.nordicsemi.android.ble.observer.ConnectionObserver;


public class Bric4Communicator extends Communicator implements ConnectionObserver {

    private final DeviceActivity activity;
    private final BluetoothDevice bluetoothDevice;
    private final Bric4Manager manager;


    public Bric4Communicator(
            DeviceActivity activity, BluetoothDevice bluetoothDevice) {
        this.activity = activity;
        this.bluetoothDevice = bluetoothDevice;

        manager = new Bric4Manager(activity, activity.getSurveyManager());
        manager.setConnectionObserver(this);
    }

    @Override
    public boolean isConnected() {
        return manager != null && manager.isConnected();
    }

    @Override
    public void requestConnect() {
        manager.connect(bluetoothDevice)
            .timeout(10000) // milliseconds
            .retry(3, 100)
            .enqueue();
    }

    @Override
    public void requestDisconnect() {
        manager.disconnect().enqueue();
    }



    @Override
    public void onDeviceConnecting(@NonNull BluetoothDevice device) {
        Log.device("Connecting to " + device.getName());
    }


    @Override
    public void onDeviceConnected(@NonNull BluetoothDevice device) {
        Log.device("Connected to " + device.getName());
    }


    @Override
    public void onDeviceFailedToConnect(@NonNull BluetoothDevice device, int reason) {
        Log.device("Failed to connect to " + device.getName());
        activity.setConnectionStopped();
        activity.updateConnectionStatus();
    }


    @Override
    public void onDeviceReady(@NonNull BluetoothDevice device) {
        Log.device(device.getName() + " ready :)");

    }


    @Override
    public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {
        Log.device(device.getName() + " disconnecting");
    }


    @Override
    public void onDeviceDisconnected(@NonNull BluetoothDevice device, int reason) {
        Log.device(device.getName() + " disconnected");
        activity.setConnectionStopped();
        activity.updateConnectionStatus();
    }

}
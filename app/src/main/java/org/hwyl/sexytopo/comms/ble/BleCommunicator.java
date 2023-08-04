package org.hwyl.sexytopo.comms.ble;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.comms.Communicator;
import org.hwyl.sexytopo.comms.InstrumentType;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.activity.DeviceActivity;

import java.util.Map;

import no.nordicsemi.android.ble.observer.ConnectionObserver;

/**
 * Provides a common interface for all BLE communicators using the
 * no.nordicsemi.android.ble library. To use this class, provide a
 * simple subclass that creates a manager that is a subclass of
 * SexyTopoBleManager. This manager should contain all the
 * device-specific logic.
 */
public abstract class BleCommunicator implements Communicator, ConnectionObserver {

    protected final SexyTopoBleManager manager;
    protected final BluetoothDevice bluetoothDevice;
    protected final DeviceActivity activity;

    public BleCommunicator(
            DeviceActivity activity, BluetoothDevice bluetoothDevice, SexyTopoBleManager manager) {
        this.activity = activity;
        this.bluetoothDevice = bluetoothDevice;
        this.manager = manager;
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
    public Map<Integer, Integer> getCustomCommands() {
        return manager.getCustomCommands();
    }

    public boolean handleCustomCommand(int viewId) {
        return manager.handleCustomCommand(viewId);
    }

    @Override
    public void onDeviceConnecting(@NonNull BluetoothDevice device) {
        String name = InstrumentType.describe(device);
        Log.device(R.string.device_ble_connecting_to, name);
    }


    @Override
    public void onDeviceConnected(@NonNull BluetoothDevice device) {
        String name = InstrumentType.describe(device);
        Log.device(R.string.device_ble_connected_to, name);
    }


    @Override
    public void onDeviceFailedToConnect(@NonNull BluetoothDevice device, int reason) {
        String name = InstrumentType.describe(device);
        Log.device(R.string.device_ble_failed_to_connect_to, name);
        activity.setConnectionStopped();
        activity.updateConnectionStatus();
    }


    @Override
    public void onDeviceReady(@NonNull BluetoothDevice device) {
        String name = InstrumentType.describe(device);
        Log.device(R.string.device_ble_device_ready, name);
    }


    @Override
    public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {
        String name = InstrumentType.describe(device);
        Log.device(R.string.device_ble_device_disconnecting, name);
    }


    @Override
    public void onDeviceDisconnected(@NonNull BluetoothDevice device, int reason) {
        String name = InstrumentType.describe(device);
        Log.device(R.string.device_ble_device_disconnected, name);
        activity.setConnectionStopped();
        activity.updateConnectionStatus();
    }
}

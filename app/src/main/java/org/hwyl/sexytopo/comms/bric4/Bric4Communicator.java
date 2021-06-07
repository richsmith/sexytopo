
package org.hwyl.sexytopo.comms.bric4;

import android.bluetooth.BluetoothDevice;
import android.view.View;

import org.hwyl.sexytopo.comms.Communicator;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.activity.DeviceActivity;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import no.nordicsemi.android.ble.observer.ConnectionObserver;


public class Bric4Communicator extends Communicator implements ConnectionObserver {

    private final DeviceActivity activity;
    private final BluetoothDevice bluetoothDevice;
    private final Bric4Manager manager;

    public static final int TAKE_SHOT_ID = View.generateViewId();
    public static final int TOGGLE_LASER_ID = View.generateViewId();
    public static final int POWER_OFF_ID = View.generateViewId();
    public static final int CLEAR_MEMORY_ID = View.generateViewId();


    private static final Map<Integer, String> CUSTOM_COMMANDS = new HashMap<>();


    public Bric4Communicator(
            DeviceActivity activity, BluetoothDevice bluetoothDevice) {
        this.activity = activity;
        this.bluetoothDevice = bluetoothDevice;

        manager = new Bric4Manager(activity, activity.getSurveyManager());
        manager.setConnectionObserver(this);

        CUSTOM_COMMANDS.put(TAKE_SHOT_ID, "Take shot");
        CUSTOM_COMMANDS.put(TOGGLE_LASER_ID, "Toggle laser");
        CUSTOM_COMMANDS.put(POWER_OFF_ID, "Power off");
        CUSTOM_COMMANDS.put(CLEAR_MEMORY_ID, "Clear memory");
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

    public Map<Integer, String> getCustomCommands() {
        return CUSTOM_COMMANDS;
    }

    @Override
    public boolean handleCustomCommand(int eventId) {
        // can't use a switch statement here because the IDs are not known at compile time
        if (eventId == TAKE_SHOT_ID) {
            manager.sendCustomCommand(Bric4Manager.CustomCommand.TAKE_SHOT);
            return true;
        } else if (eventId == TOGGLE_LASER_ID) {
            manager.sendCustomCommand(Bric4Manager.CustomCommand.LASER_TOGGLE);
            return true;
        } else if (eventId == POWER_OFF_ID) {
            manager.sendCustomCommand(Bric4Manager.CustomCommand.POWER_OFF);
            return true;
        } else if (eventId == CLEAR_MEMORY_ID) {
            manager.sendCustomCommand(Bric4Manager.CustomCommand.CLEAR_MEMORY);
            return true;
        } else {
            return false;
        }
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
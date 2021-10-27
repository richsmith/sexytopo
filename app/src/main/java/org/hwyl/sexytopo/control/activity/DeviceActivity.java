package org.hwyl.sexytopo.control.activity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.comms.Communicator;
import org.hwyl.sexytopo.comms.Instrument;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.util.LogUpdateReceiver;

import java.lang.reflect.Method;
import java.util.Set;

import androidx.appcompat.widget.SwitchCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public class DeviceActivity extends SexyTopoActivity {


    public static final String DISTO_X_PREFIX = "DistoX";
    public static final String SHETLAND_PREFIX = "Shetland";

    private static final BluetoothAdapter BLUETOOTH_ADAPTER = BluetoothAdapter.getDefaultAdapter();

    private IntentFilter logFilter;
    private IntentFilter statusFilter;
    private IntentFilter scanFilter;
    private DeviceLogUpdateReceiver logUpdateReceiver;
    private StateChangeReceiver stateChangeReceiver;
    private ScanReceiver scanReceiver;

    private boolean isConnectionStartingOrStarted = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        setupSwitchListeners();
        updateStatuses();

        scanFilter = new IntentFilter();
        scanFilter.addAction(BluetoothDevice.ACTION_FOUND);
        scanFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        scanReceiver = new ScanReceiver();

        statusFilter = new IntentFilter();
        statusFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        statusFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        stateChangeReceiver = new StateChangeReceiver();

        logFilter = new IntentFilter();
        logFilter.addAction(SexyTopo.DEVICE_LOG_UPDATED_EVENT);


        logUpdateReceiver = new DeviceLogUpdateReceiver();
    }


    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onResume() {
        super.onResume();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        logUpdateReceiver.update();
        updateStatuses();

        registerReceiver(scanReceiver, scanFilter);
        registerReceiver(stateChangeReceiver, statusFilter);

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(logUpdateReceiver, logFilter);

        if (!requestComms().isConnected()) {
            updateComms();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(stateChangeReceiver);
            unregisterReceiver(scanReceiver);
            LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
            broadcastManager.unregisterReceiver(logUpdateReceiver);
        } catch (Exception exception) { // this shouldn't ever happen
            Log.e("Error unregistering receiver: " + exception.getMessage());
        }
    }


    private void updateStatuses() {
        updateBluetooth();
        updatePairedStatus();
        updateConnectionStatus();
        updateComms();
    }

    private void setupSwitchListeners() {
        SwitchCompat bluetoothSwitch = findViewById(R.id.bluetoothSwitch);
        bluetoothSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> toggleBluetooth(buttonView));

        SwitchCompat connectionSwitch = findViewById(R.id.connectionSwitch);
        connectionSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> toggleConnection(buttonView));
    }


    public void startConnection() {
        try {
            updateComms();
            requestComms().requestConnect();

        } catch (Exception exception) {
            showException(exception);
        }

    }

    public void stopConnection() {
        Log.device(getString(R.string.device_log_stopping));

        try {
            requestComms().requestDisconnect();
        } catch (Exception e) {
            Log.device("Error stopping thread:\n" + e.getMessage());
        }
    }

    public void setConnectionStopped() {
        isConnectionStartingOrStarted = false;
    }


    @SuppressWarnings("RedundantIfStatement")
    private void updateBluetooth() {

        if (BLUETOOTH_ADAPTER == null) {
            Toast.makeText(getApplicationContext(), "No Bluetooth capabilities detected",
                    Toast.LENGTH_SHORT).show();
        }

        SwitchCompat bluetoothSwitch = findViewById(R.id.bluetoothSwitch);

        if (BLUETOOTH_ADAPTER == null) {
            bluetoothSwitch.setChecked(false);
            bluetoothSwitch.setEnabled(false);
        } else if (BLUETOOTH_ADAPTER.isEnabled()) {
            bluetoothSwitch.setChecked(true);
        } else {
            bluetoothSwitch.setChecked(false);
        }

        findViewById(R.id.pairButton).setEnabled(bluetoothSwitch.isChecked());
        findViewById(R.id.unpairButton).setEnabled(bluetoothSwitch.isChecked());
    }


    public void toggleBluetooth(View view) {
        SwitchCompat bluetoothSwitch = (SwitchCompat)view;
        if (bluetoothSwitch.isChecked()) {
            BluetoothAdapter.getDefaultAdapter().enable();
        } else {
            BluetoothAdapter.getDefaultAdapter().disable();
        }
        updateBluetooth();
    }


    public void toggleConnection(View view) {
        SwitchCompat connectionSwitch = (SwitchCompat)view;
        if (connectionSwitch.isChecked()) {
            Log.device(getString(R.string.device_log_connection_requested));
            isConnectionStartingOrStarted = true;
            startConnection();
        } else {
            Log.device(getString(R.string.device_log_connection_stop_requested));
            isConnectionStartingOrStarted = false;
            stopConnection();
        }
    }


    // TODO need some way of calling this
    public void clearLog(View view) {
        Log.clearDeviceLog();
    }


    public void requestPair(View view) {

        if (BLUETOOTH_ADAPTER.isDiscovering()) {
            Log.device("Cancelling...");
            BLUETOOTH_ADAPTER.cancelDiscovery();

        } else {
            boolean started = BLUETOOTH_ADAPTER.startDiscovery();
            if (started) {
                Log.device("Scanning...");
            } else {
                Log.device("Unable to scan (bluetooth failure?)");
            }
        }
    }


    private void updatePairedStatus() {

        BluetoothDevice device = getPairedDevice();
        boolean isPaired = (device != null);

        SwitchCompat bluetoothSwitch = findViewById(R.id.bluetoothSwitch);
        Button pairButton = findViewById(R.id.pairButton);
        Button unpairButton = findViewById(R.id.unpairButton);

        TextView deviceList = findViewById(R.id.deviceList);
        deviceList.setTextColor(device == null? Color.BLACK : Color.RED);
        if (isPaired) {
            pairButton.setEnabled(false);
            unpairButton.setEnabled(true);
            deviceList.setTextColor(Color.BLACK);
            deviceList.setText(device.getName());
        } else {
            pairButton.setEnabled(true);
            unpairButton.setEnabled(false);
            deviceList.setTextColor(Color.RED);
            deviceList.setText(R.string.no_device);
        }

        // Allow connections iff we have one connected DistoX
        SwitchCompat connectionSwitch = findViewById(R.id.connectionSwitch);
        if (isPaired && bluetoothSwitch.isChecked()) {
            connectionSwitch.setEnabled(true);
        } else {
            connectionSwitch.setChecked(false);
            connectionSwitch.setEnabled(false);
        }
    }

    public void updateConnectionStatus() {
        SwitchCompat connectionSwitch = findViewById(R.id.connectionSwitch);
        connectionSwitch.setChecked(isConnectionStartingOrStarted);
    }


    public void requestUnpair(View view) {
        BluetoothDevice device = getPairedDevice();
        stopConnection();
        unpair(device);
        updatePairedStatus();
    }


    private void unpair(BluetoothDevice device) {

        if (device == null) {
            return;
        }

        try {
            Log.device("Unpairing " + device.getName());
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[])null);
            updateComms();
            Log.device("Unpairing successful");
        } catch (Exception e) {
            Log.device("Error unpairing: " + e.getMessage());
        }
    }

    private void updateComms() {
        BluetoothDevice device = getPairedDevice();
        Instrument instrument = Instrument.byDevice(device);

        try {
            if (instrument != getInstrument()) {
                setInstrument(instrument);
                Communicator communicator = instrument.getNewCommunicator(this, device);
                setComms(communicator);
                invalidateOptionsMenu();
            }

        } catch (Exception exception) {
            Log.e(exception);
            String name = instrument.getName();
            Log.device("Failed to create communicator for " + name);
        }
    }


    private static void pair(BluetoothDevice device) {
        try {
            Log.device("Pairing with " + device.getName());
            Method m = device.getClass().getMethod("createBond", (Class[]) null);
            m.invoke(device, (Object[])null);
            Log.device("Pairing successful");
        } catch (Exception e) {
            Log.device("Error pairing: " + e.getMessage());
        }

    }

    private static BluetoothDevice getPairedDevice() {
        
        if (BLUETOOTH_ADAPTER == null) {
            return null;
        }

        Set<BluetoothDevice> allPairedDevices = BLUETOOTH_ADAPTER.getBondedDevices();
        for (BluetoothDevice device : allPairedDevices) {
            Instrument instrument = Instrument.byDevice(device);
            if (instrument != Instrument.OTHER && instrument != Instrument.NONE) {
                return device; // we should only be paired with one, so return the first
            }
        }

        return null;
    }


    private class StateChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateStatuses();
        }
    }

    private static class ScanReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device == null) {
                    Log.device("Error detecting instruments; please try again");
                    return;
                }

                String name = device.getName();
                Instrument instrument = Instrument.byName(name);
                if (instrument == Instrument.OTHER) {
                    Log.device("Incompatible device \"" + name + "\" detected");

                } else {
                    Log.device(instrument.getName() + " detected");
                    pair(device);
                    BLUETOOTH_ADAPTER.cancelDiscovery();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.device("Scan finished");
            }
        }
    }


    private class DeviceLogUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            update();
        }

        public void update() {
            TextView logView = findViewById(R.id.deviceLog);
            final ScrollView scrollView = findViewById(R.id.scrollView);
            LogUpdateReceiver.update(Log.LogType.DEVICE, scrollView, logView);
            scrollView.postDelayed(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN),1000);
        }
    }
}

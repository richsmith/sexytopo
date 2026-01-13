package org.hwyl.sexytopo.control.activity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.comms.Communicator;
import org.hwyl.sexytopo.comms.Instrument;
import org.hwyl.sexytopo.comms.InstrumentType;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.util.LogUpdateReceiver;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;


public class DeviceActivity extends SexyTopoActivity {


    public static final String DISTO_X_PREFIX = "DistoX";
    public static final String SHETLAND_PREFIX = "Shetland";

    private static BluetoothAdapter bluetoothAdapter;

    private IntentFilter logFilter;
    private IntentFilter statusFilter;
    private IntentFilter scanFilter;
    private DeviceLogUpdateReceiver logUpdateReceiver;
    private StateChangeReceiver stateChangeReceiver;
    private ScanReceiver scanReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        setupMaterialToolbar();

        applyEdgeToEdgeInsets(R.id.rootLayout, true, true);

        bluetoothAdapter = getBluetoothAdapter();

        setupSwitchListeners();
        try {
            updateStatuses();
        } catch (Exception exception) {
            Log.e(exception);
            Log.e("Error updating statuses: " + exception.getMessage());
        }

        scanFilter = new IntentFilter();
        scanFilter.addAction(BluetoothDevice.ACTION_FOUND);
        scanFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        scanReceiver = new ScanReceiver();

        statusFilter = new IntentFilter();
        statusFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        statusFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        stateChangeReceiver = new StateChangeReceiver();

        logFilter = new IntentFilter();
        logFilter.addAction(SexyTopoConstants.DEVICE_LOG_UPDATED_EVENT);


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
            showExceptionAndLog(exception);
        }

    }

    public void stopConnection() {
        Log.device(getString(R.string.device_connection_closing));

        try {
            requestComms().requestDisconnect();
        } catch (Exception e) {
            Log.device(R.string.device_connection_closing_error, e.getMessage());        }
    }


    private BluetoothAdapter getBluetoothAdapter() {
        if (bluetoothAdapter == null) {
            try {
                BluetoothManager bluetoothManager =
                        (BluetoothManager) (getSystemService(Context.BLUETOOTH_SERVICE));
                bluetoothAdapter = bluetoothManager.getAdapter();
            } catch (Exception exception) {
                Log.e(exception);
            }
        }
        return bluetoothAdapter;
    }


    @SuppressWarnings("RedundantIfStatement")
    private void updateBluetooth() {

        if (bluetoothAdapter == null) {
            bluetoothAdapter = getBluetoothAdapter();
        }

        if (bluetoothAdapter == null) {
            showSimpleToast(R.string.device_bluetooth_no_adapter);
        }

        SwitchCompat bluetoothSwitch = findViewById(R.id.bluetoothSwitch);

        if (bluetoothAdapter == null) {
            bluetoothSwitch.setChecked(false);
            bluetoothSwitch.setEnabled(false);
        } else if (bluetoothAdapter.isEnabled()) {
            bluetoothSwitch.setChecked(true);
        } else {
            bluetoothSwitch.setChecked(false);
        }

        findViewById(R.id.pairButton).setEnabled(bluetoothSwitch.isChecked());
        findViewById(R.id.unpairButton).setEnabled(bluetoothSwitch.isChecked());
    }


    public void toggleBluetooth(View view) throws SecurityException {
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
            Log.device(getString(R.string.device_connection_requested));
            startConnection();
        } else {
            Log.device(getString(R.string.device_connection_stop_requested));
            stopConnection();
        }
    }


    // TODO need some way of calling this
    public void clearLog(View view) {
        Log.clearDeviceLog();
    }


    public void requestPair(View view) throws SecurityException {

        bluetoothAdapter = getBluetoothAdapter();

        if (bluetoothAdapter == null) {
            showSimpleToast(R.string.device_bluetooth_no_adapter);
            return;
        }

        if (bluetoothAdapter.isDiscovering()) {
            Log.device(R.string.device_scan_cancel);
            bluetoothAdapter.cancelDiscovery();

        } else {
            boolean started = bluetoothAdapter.startDiscovery();
            if (started) {
                Log.device(R.string.device_scan_start);
            } else {
                Log.device(R.string.device_scan_failure);
            }
        }
    }



    public void requestUnpair(View view) {
        BluetoothDevice device = getPairedDevice();
        stopConnection();
        unpair(device);
        updatePairedStatus();
    }


    private void updatePairedStatus() throws SecurityException {

        BluetoothDevice device = getPairedDevice();
        boolean isPaired = (device != null);

        SwitchCompat bluetoothSwitch = findViewById(R.id.bluetoothSwitch);
        Button pairButton = findViewById(R.id.pairButton);
        Button unpairButton = findViewById(R.id.unpairButton);

        TextView deviceList = findViewById(R.id.deviceList);
        int textColor = device == null ? ContextCompat.getColor(this, android.R.color.darker_gray) : ContextCompat.getColor(this, R.color.red);
        deviceList.setTextColor(textColor);
        if (isPaired) {
            pairButton.setEnabled(false);
            unpairButton.setEnabled(true);
            deviceList.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            deviceList.setText(device.getName());
        } else {
            pairButton.setEnabled(true);
            unpairButton.setEnabled(false);
            deviceList.setTextColor(ContextCompat.getColor(this, R.color.red));
            deviceList.setText(R.string.device_no_device);
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
        boolean isActuallyConnected = requestComms().isConnected();
        connectionSwitch.setChecked(isActuallyConnected);
    }



    private void unpair(BluetoothDevice device) throws SecurityException {

        if (device == null) {
            return;
        }

        try {
            Log.device(R.string.device_pairing_unpairing_device, device.getName());
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[])null);
            updateComms();
            Log.device(R.string.device_pairing_unpairing_success);
        } catch (Exception e) {
            Log.device(R.string.device_pairing_unpairing_error, e.getMessage());
        }
    }

    private void updateComms() {
        BluetoothDevice device = getPairedDevice();
        InstrumentType instrumentType = InstrumentType.byDevice(device);

        Instrument instrument = getInstrument();

        boolean doWeNeedToUpdateInstrument = false;

        if (instrument == null && instrumentType.isUsable()) {
            doWeNeedToUpdateInstrument = true;
        } else if (instrument != null && instrumentType != instrument.getInstrumentType()) {
            doWeNeedToUpdateInstrument = true;
        }

        if (doWeNeedToUpdateInstrument) {
            instrument = new Instrument(device);
            setInstrument(instrument);

            try {
                Communicator communicator = instrumentType.getNewCommunicator(this, device);
                setComms(communicator);
                invalidateOptionsMenu();
            } catch (Exception exception) {
                Log.e(exception);
                Log.device(R.string.device_communicator_creation_error, exception.getMessage());
            }
        }
    }


    private static void pair(BluetoothDevice device) throws SecurityException {
        try {
            Log.device(R.string.device_pairing_attempt, device.getName());
            device.createBond();
            Log.device(R.string.device_pairing_successful);
        } catch (Exception e) {
            Log.device(R.string.device_pairing_error, e.getMessage());
        }

    }

    private BluetoothDevice getPairedDevice() {

        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        if (bluetoothAdapter == null) {
            return null;
        }

        Set<BluetoothDevice> allPairedDevices;
        try {
            allPairedDevices = bluetoothAdapter.getBondedDevices();
        } catch (SecurityException exception) {
            allPairedDevices = new HashSet<>(); // probably not got permissions yet
        }

        for (BluetoothDevice device : allPairedDevices) {
            InstrumentType instrumentType = InstrumentType.byDevice(device);
            if (instrumentType != InstrumentType.OTHER && instrumentType != InstrumentType.NONE) {
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
                    Log.device(R.string.device_scan_device_not_found);
                    return;
                }

                try {
                    String name = device.getName();
                    InstrumentType instrumentType = InstrumentType.byName(name);
                    if (instrumentType.isUsable()) {
                        Log.device(R.string.device_scan_detected, instrumentType.describe());
                        pair(device);
                        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    } else {
                        Log.device(R.string.device_pairing_incompatible, name);
                    }

                } catch (SecurityException e) {
                    Log.e(e);
                    Log.device(R.string.device_access_failure);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.device(R.string.device_scan_end);
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

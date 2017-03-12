package org.hwyl.sexytopo.control.activity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.comms.DistoXPoller;
import org.hwyl.sexytopo.control.Log;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeviceActivity extends SexyTopoActivity {


    public static final String DISTO_X_PREFIX = "DistoX";

    private DistoXPoller comms;

    private static final BluetoothAdapter BLUETOOTH_ADAPTER = BluetoothAdapter.getDefaultAdapter();

    private DeviceLogUpdateReceiver logUpdateReceiver;
    private StateChangeReceiver stateChangeReceiver = new StateChangeReceiver();

    private boolean doConnection = false;

    private IntentFilter pairFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
    private IntentFilter bluetoothFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setupSwitchListeners();

        setupLogView();

        updateStatuses();

        stateChangeReceiver = new StateChangeReceiver();

        registerReceiver(stateChangeReceiver, pairFilter);
        registerReceiver(stateChangeReceiver, bluetoothFilter);
    }


    @Override
    protected void onStart() {
        super.onStart();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(stateChangeReceiver);
        } catch (Exception exception) { // this shouldn't ever happen
            Log.e("Error unregistering receiver: " + exception.getMessage());
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        logUpdateReceiver.update();
        updateStatuses();
        registerReceiver(stateChangeReceiver, pairFilter);
        registerReceiver(stateChangeReceiver, bluetoothFilter);
    }

    private void updateStatuses() {
        updateBluetooth();
        updatePairedStatus();
        updateConnectionStatus();
    }

    private void setupLogView() {
        if (logUpdateReceiver == null) {
            TextView logView = (TextView)findViewById(R.id.deviceLog);
            ScrollView scrollView = (ScrollView)findViewById(R.id.scrollView);
            logUpdateReceiver = new DeviceLogUpdateReceiver(scrollView, logView);

            IntentFilter filter = new IntentFilter(SexyTopo.DEVICE_LOG_UPDATED_EVENT);
            LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
            broadcastManager.registerReceiver(logUpdateReceiver, filter);
        }
    }

    private void setupSwitchListeners() {
        Switch bluetoothSwitch = (Switch)(findViewById(R.id.bluetoothSwitch));
        bluetoothSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleBluetooth(buttonView);
            }
        });

        Switch connectionSwitch = (Switch)(findViewById(R.id.connectionSwitch));
        connectionSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleConnection(buttonView);
            }
        });
    }


    public void startConnection() {

        if (comms != null) {
            stopConnection();
        }

        try {
            assert comms == null;
            BluetoothDevice bluetoothDevice = getDistoX();
            comms = new DistoXPoller(this, bluetoothDevice, dataManager);
            comms.start();
        } catch (Exception e) {
            Log.device("Error starting thread:\n" + e.getMessage());
        }
    }


    public void stopConnection() {

        if (comms == null || !comms.isAlive()) {
            return;
        }

        Log.device(getString(R.string.device_log_stopping));

        try {
            comms.kill();
            comms.join();
            comms = null;
            Log.device(getString(R.string.device_log_stopped));
        } catch (Exception e) {
            Log.device("Error stopping thread:\n" + e.getMessage());
        }
    }


    private void updateBluetooth() {

        if (BLUETOOTH_ADAPTER == null) {
            Toast.makeText(getApplicationContext(), "No Bluetooth capabilities detected",
                    Toast.LENGTH_SHORT).show();
        }

        Switch bluetoothSwitch = (Switch)(findViewById(R.id.bluetoothSwitch));

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
        Switch bluetoothSwitch = (Switch)view;
        if (bluetoothSwitch.isChecked()) {
            BluetoothAdapter.getDefaultAdapter().enable();
        } else {
            BluetoothAdapter.getDefaultAdapter().disable();
        }
        updateBluetooth();
    }


    public void toggleConnection(View view) {
        Switch connectionSwitch = (Switch)view;
        if (connectionSwitch.isChecked()) {
            Log.device(getString(R.string.device_log_connection_requested));
            doConnection = true;
            startConnection();
        } else {
            Log.device(getString(R.string.device_log_connection_stop_requested));
            doConnection = false;
            stopConnection();
        }
    }


    public void clearLog(View view) {
        Log.clearDeviceLog();
    }


    public void requestPair(View view) {

        Log.device("Pairing requested (please wait)");

        final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    if (isDistoX(device)) {
                        Log.device("DistoX detected");
                        pair(device);
                    } else {
                        String name = device.getName();
                        Log.device("Device \"" + name + "\"detected but it may not be a DistoX");
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, filter);
        BLUETOOTH_ADAPTER.startDiscovery();
    }


    private void updatePairedStatus() {

        Set<BluetoothDevice> distos = getPairedDistos();

        Switch bluetoothSwitch = (Switch)(findViewById(R.id.bluetoothSwitch));

        TextView deviceList = (TextView)(findViewById(R.id.deviceList));
        deviceList.setTextColor(distos.size() == 1? Color.BLACK : Color.RED);
        deviceList.setText(describeNDevices(distos.size()));


        // Allow connections iff we have one connected DistoX
        Switch connectionSwitch = (Switch)(findViewById(R.id.connectionSwitch));
        if (distos.size() == 1 && bluetoothSwitch.isChecked()) {
            connectionSwitch.setEnabled(true);
        } else {
            connectionSwitch.setChecked(false);
            connectionSwitch.setEnabled(false);
            stopConnection();
        }
    }

    private void updateConnectionStatus() {
        Switch connectionSwitch = (Switch)(findViewById(R.id.connectionSwitch));
        connectionSwitch.setChecked(doConnection);
    }


    public void requestUnpair(View view) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);


        Set<BluetoothDevice> set = getPairedDistos();
        final List<BluetoothDevice> devices = new ArrayList<>(set);
        List<String> names = new ArrayList<>();
        for (BluetoothDevice device : devices) {
            names.add(device.getName());
        }

        final Set<BluetoothDevice> selected = new HashSet<>();

        builder.setMultiChoiceItems(names.toArray(new String[]{}), null,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        selected.add(devices.get(which));
                    }
                });
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.setPositiveButton(getString(R.string.unpair_command),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for (BluetoothDevice device : selected) {
                            unpair(device);
                        }
                        updatePairedStatus();
                    }
                });

        builder.show();


    }


    private void unpair(BluetoothDevice device) {
        try {
            Log.device("Unpairing " + device.getName());
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[])null);
            Log.device("Unpairing successful");
        } catch (Exception e) {
            Log.device("Error unpairing: " + e.getMessage());
        }
    }


    private void pair(BluetoothDevice device) {
        try {
            Log.device("Pairing with " + device.getName());
            Method m = device.getClass().getMethod("createBond", (Class[]) null);
            m.invoke(device, (Object[])null);
        } catch (Exception e) {
            Log.device("Error pairing: " + e.getMessage());
        }

    }


    private String describeNDevices(int n) {
        return n + " " +
                getString(n == 1? R.string.device_singular : R.string.device_plural);
    }


    private static boolean isDistoX(BluetoothDevice device) {
        String name = device.getName();
        return name.toLowerCase().contains(DISTO_X_PREFIX.toLowerCase());
    }


    private static Set<BluetoothDevice> getPairedDistos() {
        
        if (BLUETOOTH_ADAPTER == null) {
            return new HashSet<>(0);
        }
        
        Set<BluetoothDevice> pairedDistoXes = new HashSet<>();
        Set<BluetoothDevice> pairedDevices = BLUETOOTH_ADAPTER.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (isDistoX(device)) {
                pairedDistoXes.add(device);
            }
        }
        
        return pairedDistoXes;
    }


    private static BluetoothDevice getDistoX() {
        Set<BluetoothDevice> distoXes = getPairedDistos();

        if (distoXes.size() != 1) {
            throw new IllegalStateException(distoXes.size() + " DistoXes paired");
        }

        return distoXes.toArray(new BluetoothDevice[]{})[0];
    }


    private class DeviceLogUpdateReceiver extends BroadcastReceiver {

        private final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("HH:mm");

        private ScrollView scrollView;
        private TextView logView;

        private DeviceLogUpdateReceiver(ScrollView scrollView, TextView logView) {
            this.scrollView = scrollView;
            this.logView = logView;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            update();
        }

        private void update() {

            StringBuilder logText = new StringBuilder();

            for (Log.Message message : Log.getDeviceLog()) {
                String timestamp = TIMESTAMP_FORMAT.format(message.getTimestamp());
                logText.append("\n" + timestamp + " " + message.getText());
            }

            logView.setText(logText);
            scrollView.fullScroll(ScrollView.FOCUS_DOWN);

            scrollView.invalidate();

        }

    }


    private class StateChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateStatuses();
        }
    }
}

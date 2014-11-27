package org.hwyl.sexytopo.control.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.comms.DistoXPoller;

import java.util.Set;

public class DeviceActivity extends SexyTopoActivity {


    DistoXPoller comms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    /*}


    @Override
    protected void onStart() {*/
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "No Bluetooth capabilities detected",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (! bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            int REQUEST_ENABLE_BT = 1; // fix this see http://stackoverflow.com/questions/8188277/error-checking-if-bluetooth-is-enabled-in-android-request-enable-bt-cannot-be-r
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        //bluetoothAdapter.startDiscovery();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                Toast.makeText(getApplicationContext(), "Paired: " + device.getName(), Toast.LENGTH_SHORT).show();
                // Add the name and address to an array adapter to show in a ListView
                //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                Object ignore = device;
            }
        }

        // Create a BroadcastReceiver for ACTION_FOUND
        final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                    String name = device.getName();
                    //if (isDistoX(name)) {
                        doConnection(device);
                    //}


                }
            }
        };

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, filter); // Don't forget to unregister during onDestroy
        bluetoothAdapter.startDiscovery();
    }


    public void doConnection(BluetoothDevice bluetoothDevice) {

        if (comms != null && comms.isAlive()) {
            return; // we don't need to start the thread if it's already running
        }

        comms = new DistoXPoller(bluetoothDevice, dataManager);
        comms.start();
    }


}

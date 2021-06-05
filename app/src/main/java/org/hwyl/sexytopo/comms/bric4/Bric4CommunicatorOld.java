
package org.hwyl.sexytopo.comms.bric4;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;

import org.hwyl.sexytopo.comms.SerialSocket;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.activity.SexyTopoActivity;
import org.hwyl.sexytopo.model.survey.Leg;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.UUID;


public class Bric4CommunicatorOld {


    private static final BluetoothAdapter BLUETOOTH_ADAPTER = BluetoothAdapter.getDefaultAdapter();
    private BluetoothDevice bluetoothDevice;
    private BluetoothGatt bluetoothGatt;
    private SerialSocket socket;

    private final SurveyManager dataManager;
    private final SexyTopoActivity activity;

    private boolean keepAlive;

    public final static UUID CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID MEASUREMENT_SYNC_SERVICE_UUID = UUID.fromString("000058d0-0000-1000-8000-00805f9b34fb");
    public static final UUID MEASUREMENT_PRIMARY_CHARACTERISTIC_UUID = UUID.fromString("000058d1-0000-1000-8000-00805f9b34fb");
    //public static final UUID MEASUREMENT_METADATA_CHARACTERISTIC = UUID.fromString("000058d2");
    //public static final UUID MEASUREMENT_ERRORS_CHARACTERISTIC =
            //UUID.fromString("000058d3");


    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.device("Connected to GATT server :)");
                Log.device("Looking for services");
                bluetoothGatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.device("Disconnected from GATT server");
            }
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.device("characteristic " + characteristic + " changed");

            //boolean result = gatt.readCharacteristic(characteristic);
            //Log.device("read result " + result);

            byte[] value = characteristic.getValue();
            float distance = getFloat(value, 8);
            float azimuth = getFloat(value, 12);
            float inclination = getFloat(value, 16);
            Leg leg = new Leg(distance, azimuth, inclination);
            Log.device(leg.toString());
            dataManager.updateSurvey(leg);

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.device("onCharacteristicRead status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] value = characteristic.getValue();
                byte[] x = value;
                float metres = 8 * 8 * 8 * x[8] + 8 * 8 * x[9] + 8 * x[10] + x[11];
                Log.device(metres + " metres?");

                metres = getFloat(value, 8);
                Log.device(metres + " metres?");

            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic chrt, int status) {
            Log.device("Characteristic write called");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.device("Descriptor write called");
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> services = bluetoothGatt.getServices();
                for (BluetoothGattService service : services) {
                    Log.device(service.toString());
                    //Log.device(service.getCharacteristics());
                }
                Log.device("onServicesDiscovered received: " + status);

                BluetoothGattService measurementSyncService =
                    gatt.getService(MEASUREMENT_SYNC_SERVICE_UUID);
                BluetoothGattCharacteristic measurement_primary_characteristic =
                    measurementSyncService.getCharacteristic(MEASUREMENT_PRIMARY_CHARACTERISTIC_UUID);
                boolean successfullySetNotifications =
                    gatt.setCharacteristicNotification(measurement_primary_characteristic, true);
                Log.device("Success set notification? " + successfullySetNotifications);

                BluetoothGattDescriptor descriptor =
                        measurement_primary_characteristic.getDescriptor(CCCD_UUID);
                writeDescriptor(gatt, descriptor, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);

            } else {
                Log.device("onServicesDiscovered received: " + status);
            }
        }

        public float getFloat( byte[] bytes, int offset ) {
            return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat( offset );
        }

        public void writeDescriptor(
                BluetoothGatt gatt, BluetoothGattDescriptor descriptor, byte[] payload) {
            boolean success = descriptor.setValue(payload);
            Log.device("descriptor setvalue result? " + success);
            success = gatt.writeDescriptor(descriptor);
            Log.device("writedescriptor result? " + success);
        }
    };


    public Bric4CommunicatorOld(SexyTopoActivity activity, SurveyManager dataManager, BluetoothDevice bluetoothDevice) {
        this.activity = activity;
        this.dataManager = dataManager;
        this.bluetoothDevice = bluetoothDevice;
    }

    public void requestConnect() {
        Log.device("Connecting to GATT");
        bluetoothGatt = bluetoothDevice.connectGatt(activity, false, gattCallback);
    }

    public void requestDisconnect() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
        }
    }

}
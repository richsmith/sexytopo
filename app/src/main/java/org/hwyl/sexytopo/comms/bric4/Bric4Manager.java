package org.hwyl.sexytopo.comms.bric4;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;
import android.widget.Toast;

import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.util.NumberTools;
import org.hwyl.sexytopo.model.survey.Leg;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.callback.profile.ProfileDataCallback;
import no.nordicsemi.android.ble.data.Data;


/**
 * See https://github.com/NordicSemiconductor/Android-BLE-Library
 * for documentation for the library we're using
 */
public class Bric4Manager extends BleManager {

    public enum CustomCommand {
        SCAN("scan"),
        TAKE_SHOT("shot"),
        LASER_TOGGLE("laser"),
        POWER_OFF("power off"),
        CLEAR_MEMORY("clear memory");

        private final String commandString;

        CustomCommand(String commandString) {
            this.commandString = commandString;
        }

        public String getCommandString() {
            return commandString;
        }

    }

    public void sendCustomCommand(CustomCommand command) {
        String commandString = command.getCommandString();
        byte[] bytes = commandString.getBytes(); // { (byte)0x6c, (byte)0x61, (byte)0x73, (byte)0x65, (byte)0x72 };
        writeCharacteristic(deviceControlCharacteristic, bytes)
                .done(device -> Log.device("Command sent: " + commandString))
                .enqueue();
    }


    final static UUID DEVICE_INFORMATION_SERVICE_UUID =
            UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb");

    final static UUID BATTERY_SERVICE_UUID =
            UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");

    final static UUID MEASUREMENT_SYNC_SERVICE_UUID =
            UUID.fromString("000058d0-0000-1000-8000-00805f9b34fb");
    final static UUID MEASUREMENT_PRIMARY_CHARACTERISTIC_UUID =
            UUID.fromString("000058d1-0000-1000-8000-00805f9b34fb");
    final static UUID MEASUREMENT_METADATA_CHARACTERISTIC_UUID =
            UUID.fromString("000058d2-0000-1000-8000-00805f9b34fb");
    final static UUID MEASUREMENT_ERRORS_CHARACTERISTIC_UUID =
            UUID.fromString("000058d3-0000-1000-8000-00805f9b34fb");
    final static UUID LAST_TIME_CHARACTERISTIC_UUID =
            UUID.fromString("000058d3-0000-1000-8000-00805f9b34fb");

    final static UUID DEVICE_CONTROL_SERVICE_UUID =
            UUID.fromString("000058e0-0000-1000-8000-00805f9b34fb");
    final static UUID DEVICE_CONTROL_CHARACTERISTIC_UUID =
            UUID.fromString("000058e1-0000-1000-8000-00805f9b34fb");

    // Client characteristics
    private BluetoothGattCharacteristic measurementPrimaryCharacteristic;
    private BluetoothGattCharacteristic measurementMetadataCharacteristic;
    private BluetoothGattCharacteristic measurementErrorsCharacteristic;
    private BluetoothGattCharacteristic lastTimeCharacteristic;
    private BluetoothGattCharacteristic deviceControlCharacteristic;


    private final SurveyManager dataManager;


    public Bric4Manager(@NonNull final Context context,
                        SurveyManager dataManager) {
        super(context);
        this.dataManager = dataManager;
    }

    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return new MyManagerGattCallback();
    }

    /**
     * BluetoothGatt callbacks object.
     */
    private class MyManagerGattCallback extends BleManagerGattCallback {

        // This method will be called when the device is connected and services are discovered.
        // You need to obtain references to the characteristics and descriptors that you will use.
        // Return true if all required services are found, false otherwise.
        @Override
        public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
            final BluetoothGattService measurementService =
                    gatt.getService(MEASUREMENT_SYNC_SERVICE_UUID);
            if (measurementService != null) {
                measurementPrimaryCharacteristic = measurementService.getCharacteristic(
                        MEASUREMENT_PRIMARY_CHARACTERISTIC_UUID);
                measurementMetadataCharacteristic = measurementService.getCharacteristic(
                        MEASUREMENT_METADATA_CHARACTERISTIC_UUID);
                measurementErrorsCharacteristic = measurementService.getCharacteristic(
                        MEASUREMENT_ERRORS_CHARACTERISTIC_UUID);
                lastTimeCharacteristic = measurementService.getCharacteristic(
                        LAST_TIME_CHARACTERISTIC_UUID);
            }

            final BluetoothGattService controlService =
                    gatt.getService(DEVICE_CONTROL_SERVICE_UUID);
            if (controlService != null) {
                deviceControlCharacteristic = controlService.getCharacteristic(
                        DEVICE_CONTROL_CHARACTERISTIC_UUID);
            }

            return measurementPrimaryCharacteristic != null &&
                    measurementMetadataCharacteristic != null &&
                    measurementErrorsCharacteristic != null;
        }

        // If you have any optional services, allocate them here. Return true only if
        // they are found.
        @Override
        protected boolean isOptionalServiceSupported(@NonNull final BluetoothGatt gatt) {
            return super.isOptionalServiceSupported(gatt);
        }

        // Initialize your device here. Often you need to enable notifications and set required
        // MTU or write some initial data. Do it here.
        @Override
        protected void initialize() {
            // You may enqueue multiple operations. A queue ensures that all operations are
            // performed one after another, but it is not required.
            DataHandler handler = new DataHandler();
            setIndicationCallback(measurementPrimaryCharacteristic).with(handler);
            setIndicationCallback(measurementMetadataCharacteristic).with(handler);
            setIndicationCallback(measurementErrorsCharacteristic).with(handler);

            beginAtomicRequestQueue()
                    .add(enableIndications(measurementPrimaryCharacteristic))
                    .add(enableIndications(measurementMetadataCharacteristic))
                    .add(enableIndications(measurementErrorsCharacteristic))
                    .enqueue();
        }

        @Override
        protected void onDeviceDisconnected() {
            measurementPrimaryCharacteristic = null;
            measurementMetadataCharacteristic = null;
            measurementErrorsCharacteristic = null;
        }
    }

    private enum State {
        MEASUREMENT,
        METADATA,
        ERRORS;

        private State advance() {
            int index = ordinal();
            int nextIndex = (index + 1) >= values().length? 0 : index + 1;
            return values()[nextIndex];
        }
    }

    private class DataHandler implements ProfileDataCallback {

        State state = State.MEASUREMENT;

        String currentRef = "?";
        Leg current = Leg.EMPTY_LEG;

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onDataReceived(@NonNull BluetoothDevice device, @NonNull Data data) {

            // Data gets received in three separate characteristics, received in order.
            // There doesn't seem to be any way to figure out which characteristic we
            // are currently receiving, so we just cycle between them.
            // There is a potential here for ST to get out of sync - hopefully this is
            // unlikely, but perhaps we could figure out a way to check the data is correct?

            byte[] bytes;

            switch(state) {

                case MEASUREMENT:
                    bytes = data.getValue();
                    if (bytes == null) {
                        return;
                    }

                    int year = NumberTools.getUint16(bytes, 0);
                    int month = NumberTools.getUint8(bytes, 2);
                    int day = NumberTools.getUint8(bytes, 3);
                    int hour = NumberTools.getUint8(bytes, 4);
                    int minute = NumberTools.getUint8(bytes, 5);
                    int seconds = NumberTools.getUint8(bytes, 6);
                    // int centiSeconds = NumberTools.getUint8(bytes, 7);
                    LocalDateTime timestamp =
                            LocalDateTime.of(year, month, day, hour, minute, seconds);

                    float distance = NumberTools.getFloat(bytes, 8);
                    float azimuth = NumberTools.getFloat(bytes, 12);
                    float inclination = NumberTools.getFloat(bytes, 16);

                    try {
                        current = new Leg(distance, azimuth, inclination);
                    } catch (IllegalArgumentException exception) {
                        // if an illegal leg has been shot this should be flagged as an error
                        // in the error characteristic so we don't need to record it here
                        current = Leg.EMPTY_LEG;
                    }
                    break;

                case METADATA:
                    bytes = data.getValue();
                    if (bytes == null) {
                        return;
                    }

                    currentRef = Integer.toString(NumberTools.getUint32(bytes, 0));

                    // currently unused:
                    // float dip = NumberTools.getFloat(bytes, 4);
                    // float roll = NumberTools.getFloat(bytes, 8);
                    // float tempDegC = NumberTools.getFloat(bytes, 12);
                    // int samplesMean = NumberTools.getUint16(bytes, 16);
                    // int measurementType = NumberTools.getUint8(bytes,18);

                    break;

                case ERRORS:
                    bytes = data.getValue();
                    if (bytes == null) {
                        return;
                    }
                    int error1Code = NumberTools.getUint8(bytes, 0);
                    float error1Data1 = NumberTools.getFloat(bytes, 1);
                    float error1Data2 = NumberTools.getFloat(bytes, 5);
                    int error2Code = NumberTools.getUint8(bytes, 9);
                    float error2Data1 = NumberTools.getFloat(bytes, 10);
                    float error2Data2 = NumberTools.getFloat(bytes, 14);

                    if (error1Code > 0 || error2Code > 0) {
                        if (error1Code > 0) {
                            reportError(error1Code, error1Data1, error1Data2, true);
                        }
                        if (error2Code > 0) {
                            reportError(error2Code, error2Data1, error2Data2, false);
                        }

                    } else { // final characteristic read: only update survey if no errors reported
                        String message = "Got #" + currentRef + ": " + current;
                        Log.device(message, true);
                        dataManager.updateSurvey(current);
                    }

                    break;
            }

            // after each tick, expect the next state
            state = state.advance();

        }

        private void reportError(int code, float data1, float data2, boolean showToUser) {
            Bric4Error error = Bric4Error.fromCode(code);
            String device = Objects.requireNonNull(getBluetoothDevice()).getName();
            String shortDescription = device + " error: " + error;
            Log.device(shortDescription);

            String longDescription = shortDescription +
                    " (code " + code + " data: " + data1 + "/" + data2 + ")";
            Log.e(longDescription);

            if (showToUser) {
                Toast.makeText(getContext(), shortDescription, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onInvalidDataReceived(@NonNull final BluetoothDevice device,
                                          @NonNull final Data data) {

            Log.device("invalid data received from " + device.getName() + ": " + data);
        }

    }
}

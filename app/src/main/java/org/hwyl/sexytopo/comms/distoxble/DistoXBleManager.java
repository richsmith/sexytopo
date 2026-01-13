package org.hwyl.sexytopo.comms.distoxble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.apache.commons.lang3.ArrayUtils;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.comms.ble.SexyTopoBleManager;
import org.hwyl.sexytopo.comms.ble.SexyTopoDataHandler;
import org.hwyl.sexytopo.comms.distox.CalibrationProtocol;
import org.hwyl.sexytopo.comms.distox.MeasurementProtocol;
import org.hwyl.sexytopo.comms.distox.WriteCalibrationProtocol;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.activity.DistoXCalibrationActivity;
import org.hwyl.sexytopo.model.calibration.CalibrationReading;
import org.hwyl.sexytopo.model.survey.Leg;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import no.nordicsemi.android.ble.data.Data;


public class DistoXBleManager extends SexyTopoBleManager {

    private enum MemoryRange {

        DATA_STORE(0x0000, 0x7FFFF),
        CALIBRATION_COEFFICIENTS(0x8010, 0x8043),
        FIRMWARE_VERSION(0xE000, 0xE003),
        HARDWARE_VERSION(0xE004, 0xE007),
        RAM(0xC000, 0xDFFF);
        private final int start;
        private final int end;

        MemoryRange(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public Byte[] asArray() {
            return intToBytes(start);
        }

        private static Byte[] intToBytes(int value) {
            Byte[] result = new Byte[2];
            result[0] = (byte) (value & 0xFF);
            result[1] = (byte) ((value >> 8) & 0xFF);
            return result;
        }
    }

    private static final UUID SERVICE_UUID =
            UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID WRITE_CHARACTERISTIC_UUID =
            UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID READ_CHARACTERISTIC_UUID =
            UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    private BluetoothGattCharacteristic writeCharacteristic;
    private BluetoothGattCharacteristic readCharacteristic;


    private static final int COMMAND_OPEN_CALIBRATION_VIEW = View.generateViewId();
    private static final int COMMAND_CALIBRATION_MODE_STOP = View.generateViewId();
    private static final int COMMAND_CALIBRATION_MODE_START = View.generateViewId();
    private static final int COMMAND_SILENT_MODE_STOP = View.generateViewId();
    private static final int COMMAND_SILENT_MODE_START = View.generateViewId();
    private static final int COMMAND_DEVICE_POWER_OFF = View.generateViewId();
    private static final int COMMAND_LASER_ON = View.generateViewId();
    private static final int COMMAND_LASER_OFF = View.generateViewId();
    private static final int COMMAND_LASER_TRIGGER = View.generateViewId();



    private static final Map<Integer, Integer> CUSTOM_COMMANDS = new HashMap<>();
    static {
        CUSTOM_COMMANDS.put(COMMAND_OPEN_CALIBRATION_VIEW, R.string.device_distox_command_calibration);
        CUSTOM_COMMANDS.put(COMMAND_LASER_ON, R.string.device_command_laser_on);
        CUSTOM_COMMANDS.put(COMMAND_LASER_TRIGGER, R.string.device_command_take_shot);
        CUSTOM_COMMANDS.put(COMMAND_LASER_OFF, R.string.device_command_laser_off);
        CUSTOM_COMMANDS.put(COMMAND_SILENT_MODE_START, R.string.device_distox_command_silent_mode_on);
        CUSTOM_COMMANDS.put(COMMAND_SILENT_MODE_STOP, R.string.device_distox_command_silent_mode_off);
        CUSTOM_COMMANDS.put(COMMAND_DEVICE_POWER_OFF, R.string.device_command_device_off);
    }

    private static final Map<Integer, Integer> CUSTOM_COMMAND_TO_COMMAND_BYTE = new HashMap<>();
    static {
        CUSTOM_COMMAND_TO_COMMAND_BYTE.put(COMMAND_CALIBRATION_MODE_STOP, 0x30);
        CUSTOM_COMMAND_TO_COMMAND_BYTE.put(COMMAND_CALIBRATION_MODE_START, 0x31);
        CUSTOM_COMMAND_TO_COMMAND_BYTE.put(COMMAND_SILENT_MODE_STOP, 0x32);
        CUSTOM_COMMAND_TO_COMMAND_BYTE.put(COMMAND_SILENT_MODE_START, 0x33);
        CUSTOM_COMMAND_TO_COMMAND_BYTE.put(COMMAND_DEVICE_POWER_OFF , 0x34);
        CUSTOM_COMMAND_TO_COMMAND_BYTE.put(COMMAND_LASER_ON, 0x36);
        CUSTOM_COMMAND_TO_COMMAND_BYTE.put(COMMAND_LASER_OFF, 0x37);
        CUSTOM_COMMAND_TO_COMMAND_BYTE.put(COMMAND_LASER_TRIGGER, 0x38);
    }

    private static final Byte[] WRITE_HEADER = new Byte[]{0x64, 0x61, 0x74, 0x61, 0x3a};
    private static final Byte[] WRITE_FOOTER = new Byte[]{0x0d, 0x0a};
    private static final Byte[] WRITE_MEMORY_PAYLOAD_HEADER = new Byte[]{0x3e};

    private final SurveyManager dataManager;


    public DistoXBleManager(@NonNull final Context context,
                            SurveyManager dataManager) {
        super(context);
        this.dataManager = dataManager;
    }

    @Override
    protected void initialize() {
        DataHandler handler = new DataHandler();
        setNotificationCallback(readCharacteristic).with(handler);
        enableNotifications(readCharacteristic).enqueue();
    }


    @Override
    public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
        final BluetoothGattService service = gatt.getService(SERVICE_UUID);
        if (service != null) {
            writeCharacteristic = service.getCharacteristic(WRITE_CHARACTERISTIC_UUID);
            readCharacteristic = service.getCharacteristic(READ_CHARACTERISTIC_UUID);
        }

        return readCharacteristic != null && writeCharacteristic != null;
    }

    public Map<Integer, Integer> getCustomCommands() {
        return CUSTOM_COMMANDS;
    }

    /** @noinspection DataFlowIssue*/
    @Override
    public boolean handleCustomCommand(Integer id) {

        if (CUSTOM_COMMAND_TO_COMMAND_BYTE.containsKey(id)) {
            int commandInt = CUSTOM_COMMAND_TO_COMMAND_BYTE.get(id);
            Byte commandByte = (byte)commandInt;
            Byte[] packet = createWriteCommandPacket(commandByte);

            Integer stringId;
            if (id == COMMAND_CALIBRATION_MODE_START) {
                stringId = R.string.device_distox_command_calibration_start;
            } else if (id == COMMAND_CALIBRATION_MODE_STOP) {
                stringId = R.string.device_distox_command_calibration_stop;
            } else {
                stringId = CUSTOM_COMMANDS.get(id);
            }
            writePacket(packet, stringId);
            return true;

        } else if (id == COMMAND_OPEN_CALIBRATION_VIEW) {
            startCalibrationActivity();
            return true;

        } else {
            return false;
        }

    }


    public void startCalibration() {
        handleCustomCommand(COMMAND_CALIBRATION_MODE_START);
    }

    public void stopCalibration() {
        handleCustomCommand(COMMAND_CALIBRATION_MODE_STOP);
    }

    public WriteCalibrationProtocol writeCalibration(Byte... bytes) {
        Byte[] calibrationWritePacket = createWriteMemoryPacket(
                MemoryRange.CALIBRATION_COEFFICIENTS, bytes);
        writePacket(calibrationWritePacket, R.string.device_distox_calibration_writing);

        // A bit hacky, but return null to be consistent with the interface
        // There must be a better way to do this, but would involve rewriting how the original Disto
        // handling code
        return null;
    }

    private void startCalibrationActivity() {
        Context context = getContext();
        Intent intent = new Intent(context, DistoXCalibrationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
    }


    public static Byte[] createWriteCommandPacket(Byte commandByte) {
        return createWritePacket(new Byte[]{commandByte});
    }


    private static Byte[] createWriteMemoryPacket(MemoryRange memoryRange, Byte[] payload) {
        Byte[] writePayload = createWriteMemoryPayload(memoryRange, payload);
        return createWritePacket(writePayload);
    }



    private static Byte[] createWritePacket(Byte[] payload) {



        Byte[] payloadLength = new Byte[]{(byte)(payload.length)};

        // This is a nice way of doing it, but it makes the app hang
        // for some reason - to be investigated some time
        // Byte[] packet = (Byte[]) ArrayUtils.addAll(
        //    WRITE_HEADER, payloadLength, payload, WRITE_FOOTER);

        // code contributed by Siwei Tian
        Byte[] packet = new Byte[payload.length + 8];
        System.arraycopy(WRITE_HEADER, 0, packet, 0, WRITE_HEADER.length);
        System.arraycopy(payloadLength, 0, packet, 5, 1);
        System.arraycopy(payload, 0, packet, 6, payload.length);
        System.arraycopy(WRITE_FOOTER, 0, packet, 6 + payload.length, 2);

        return packet;
    }


    private static Byte[] createWriteMemoryPayload(
            MemoryRange memoryRange, Byte[] payload) {
        Byte[] payloadLength = new Byte[]{(byte)payload.length};
        Byte[] payloadAddress = memoryRange.asArray();
        Byte[] packet = new Byte[payload.length + 4];
        System.arraycopy(WRITE_MEMORY_PAYLOAD_HEADER,0 ,packet,0,1);
        System.arraycopy(payloadAddress,0,packet,1,2);
        System.arraycopy(payloadLength,0,packet,3,1);
        System.arraycopy(payload,0,packet,4, payload.length);
        return packet;
    }


    private void writePacket(Byte[] packet, int stringId) {
        byte[] packetBytes = ArrayUtils.toPrimitive(packet);
        writeCharacteristic(
                writeCharacteristic, packetBytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            .done(device -> Log.device(stringId))
            .enqueue();
    }


    private class DataHandler extends SexyTopoDataHandler {

        final byte MEASUREMENT_IDENTIFIER = 0x01;
        final byte CALIBRATION_IDENTIFIER = 0x02;


        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onDataReceived(@NonNull BluetoothDevice device, @NonNull Data data) {

            byte[] packet = data.getValue();

            if (packet == null) { // impossible?
                Log.device(R.string.device_data_received_null);
                return;
            }

            if (packet.length == 0) { // impossible?
                Log.device(R.string.device_data_received_empty);
                return;
            }

            byte packetIdentifier = packet[0];

            if (packetIdentifier == MEASUREMENT_IDENTIFIER) {
                handleMeasurementPacket(packet);
                acknowledgePacket(packet);
            } else if (packetIdentifier == CALIBRATION_IDENTIFIER) {
                handleCalibrationPacket(packet);
                acknowledgePacket(packet);
            } else {
                Log.device(R.string.device_data_unknown_identifier);
            }
        }

        private void handleMeasurementPacket(byte[] packet) {
            byte[] data = Arrays.copyOfRange(packet,1,16);
            Leg leg = MeasurementProtocol.parseDataPacket(data);
            dataManager.updateSurvey(leg);
        }

        private void handleCalibrationPacket(byte[] packet) {
            CalibrationReading reading = new CalibrationReading();
            byte[] acceleration = Arrays.copyOfRange(packet, 1, 8);
            CalibrationProtocol.updateAccelerationSensorReading(acceleration, reading);
            byte[] magnetic = Arrays.copyOfRange(packet, 9, 16);
            CalibrationProtocol.updateMagneticSensorReading(magnetic, reading);
            dataManager.addCalibrationReading(reading);
        }

        private void acknowledgePacket(byte[] packet) {
            Byte[] acknowledgementPacket = createAcknowledgementPacket(packet);
            writePacket(acknowledgementPacket, R.string.device_data_acknowledged_packet);
        }

        private Byte[] createAcknowledgementPacket(byte[] packet) {
            Byte replyByte = (byte) (packet[1] & 0x80 | 0x55);
            return createWriteCommandPacket(replyByte);
        }

    }
}

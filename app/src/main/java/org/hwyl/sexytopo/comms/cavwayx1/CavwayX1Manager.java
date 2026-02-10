package org.hwyl.sexytopo.comms.cavwayx1;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.apache.commons.lang3.ArrayUtils;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.comms.ble.SexyTopoBleManager;
import org.hwyl.sexytopo.comms.ble.SexyTopoDataHandler;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.model.survey.Leg;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import no.nordicsemi.android.ble.data.Data;

public class CavwayX1Manager extends SexyTopoBleManager {

    // Service and Characteristic UUIDs from the Cavway X1 protocol
    private static final UUID SERVICE_UUID =
            UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID WRITE_CHARACTERISTIC_UUID =
            UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID READ_CHARACTERISTIC_UUID =
            UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    private BluetoothGattCharacteristic writeCharacteristic;
    private BluetoothGattCharacteristic readCharacteristic;

    // Custom Commands for the UI
    private static final int COMMAND_ENTER_CALIBRATION_MODE = View.generateViewId();
    private static final int COMMAND_QUIT_CALIBRATION_MODE = View.generateViewId();
    private static final int COMMAND_DEVICE_POWER_OFF = View.generateViewId();
    private static final int COMMAND_LASER_ON = View.generateViewId();
    private static final int COMMAND_LASER_OFF = View.generateViewId();
    private static final int COMMAND_LASER_TRIGGER = View.generateViewId();

    private static final Map<Integer, Integer> CUSTOM_COMMANDS = new HashMap<>();
    static {
        CUSTOM_COMMANDS.put(COMMAND_ENTER_CALIBRATION_MODE, R.string.device_cavway_x1_calibration_on);
        CUSTOM_COMMANDS.put(COMMAND_QUIT_CALIBRATION_MODE,
            R.string.device_cavway_x1_calibration_off);
        CUSTOM_COMMANDS.put(COMMAND_LASER_ON, R.string.device_command_laser_on);
        CUSTOM_COMMANDS.put(COMMAND_LASER_TRIGGER, R.string.device_command_take_shot);
        CUSTOM_COMMANDS.put(COMMAND_LASER_OFF, R.string.device_command_laser_off);
        CUSTOM_COMMANDS.put(COMMAND_DEVICE_POWER_OFF, R.string.device_command_device_off);
    }

    // Mapping from UI command to the actual byte command for the device
    private static final Map<Integer, Integer> CUSTOM_COMMAND_TO_COMMAND_BYTE = new HashMap<>();
    static {
        CUSTOM_COMMAND_TO_COMMAND_BYTE.put(COMMAND_QUIT_CALIBRATION_MODE, 0x30);
        CUSTOM_COMMAND_TO_COMMAND_BYTE.put(COMMAND_ENTER_CALIBRATION_MODE, 0x31);
        CUSTOM_COMMAND_TO_COMMAND_BYTE.put(COMMAND_DEVICE_POWER_OFF, 0x34);
        CUSTOM_COMMAND_TO_COMMAND_BYTE.put(COMMAND_LASER_ON, 0x36);
        CUSTOM_COMMAND_TO_COMMAND_BYTE.put(COMMAND_LASER_OFF, 0x37);
        CUSTOM_COMMAND_TO_COMMAND_BYTE.put(COMMAND_LASER_TRIGGER, 0x38);
    }

    private static final Byte[] WRITE_HEADER = new Byte[]{0x64, 0x61, 0x74, 0x61, 0x3a};
    private static final Byte[] WRITE_FOOTER = new Byte[]{0x0d, 0x0a};

    private final SurveyManager dataManager;

    public CavwayX1Manager(@NonNull final Context context, SurveyManager dataManager) {
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

    @Override
    public Map<Integer, Integer> getCustomCommands() {
        return CUSTOM_COMMANDS;
    }

    @Override
    public boolean handleCustomCommand(Integer id) {
        if (CUSTOM_COMMAND_TO_COMMAND_BYTE.containsKey(id)) {
            int commandInt = CUSTOM_COMMAND_TO_COMMAND_BYTE.get(id);
            Byte commandByte = (byte) commandInt;
            Byte[] packet = createWriteCommandPacket(commandByte);

            Integer stringId = CUSTOM_COMMANDS.get(id);
            writePacket(packet, stringId);
            return true;
        }
        return false;
    }

    private static Byte[] createWriteCommandPacket(Byte commandByte) {
        Byte[] payload = new Byte[]{commandByte};
        Byte[] payloadLength = new Byte[]{0x01};

        Byte[] packet = new Byte[WRITE_HEADER.length + payloadLength.length + payload.length + WRITE_FOOTER.length];
        System.arraycopy(WRITE_HEADER, 0, packet, 0, WRITE_HEADER.length);
        System.arraycopy(payloadLength, 0, packet, WRITE_HEADER.length, payloadLength.length);
        System.arraycopy(payload, 0, packet, WRITE_HEADER.length + payloadLength.length, payload.length);
        System.arraycopy(WRITE_FOOTER, 0, packet, WRITE_HEADER.length + payloadLength.length + payload.length, WRITE_FOOTER.length);

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
        private static final byte PACKET_TYPE_NORMAL = 0x01;
        private static final byte PACKET_TYPE_CALIBRATION = 0x02;
        private static final float ANGLE_SCALE = 360.0f / 65535.0f; // 360.0 / 0xFFFF

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onDataReceived(@NonNull BluetoothDevice device, @NonNull Data data) {
            byte[] packet = data.getValue();

            if (packet == null || packet.length < 64) {
                Log.device("Invalid Cavway-X1 data packet received.");
                return;
            }

            byte packetType = packet[0];
            byte flags = packet[1];

            if (packetType == PACKET_TYPE_NORMAL) {
                handleMeasurementPacket(packet);
                acknowledgePacket(flags);
            } else if (packetType == PACKET_TYPE_CALIBRATION) {
                Log.device("Cavway-X1 calibration packet received but not handled.");
                acknowledgePacket(flags);
            } else {
                Log.device(R.string.device_data_unknown_identifier);
            }
        }

        private void handleMeasurementPacket(byte[] packet) {
            // According to the protocol document
            // Byte 2: distance (16 - 23 bit)
            // Byte 3: distance (0 – 7 bit)
            // Byte 4: distance (8 – 15 bit)
            int dist_p3 = (packet[3] & 0xFF);
            int dist_p4 = (packet[4] & 0xFF) << 8;
            int dist_p2 = (packet[2] & 0xFF) << 16;
            float distance = (dist_p2 | dist_p4 | dist_p3) / 1000.0f;

            // Byte 5-6: Azimuth
            // Byte 7-8: Inclination
            // Byte 9-10: Roll
            float azimuth = parseAngle(packet, 5);
            float inclination = parseAngle(packet, 7);
            float roll = parseAngle(packet, 9);

            Leg leg = new Leg(distance, azimuth, inclination);
            dataManager.updateSurvey(leg);
        }

        private float parseAngle(byte[] data, int startIndex) {
            int lowByte = data[startIndex] & 0xFF;
            int highByte = data[startIndex + 1] & 0xFF;
            int combined = (highByte << 8) | lowByte;
            return combined * ANGLE_SCALE;
        }

        private void acknowledgePacket(byte flags) {
            // ACK value is fixed as Byte[1] | 0x55
            byte ackByte = (byte) (flags | 0x55);

            // The Cavway protocol for sending commands/acks seems to use the same header/footer structure
            // for everything except data queries. The ACK payload is a single byte.
            Byte[] payload = new Byte[]{ackByte};
            Byte[] payloadLength = new Byte[]{0x01};

            Byte[] packet = new Byte[
                WRITE_HEADER.length + payloadLength.length + payload.length + WRITE_FOOTER.length];
            System.arraycopy(WRITE_HEADER, 0, packet, 0, WRITE_HEADER.length);
            System.arraycopy(payloadLength, 0, packet, WRITE_HEADER.length, payloadLength.length);
            System.arraycopy(payload, 0, packet, WRITE_HEADER.length + payloadLength.length, payload.length);
            System.arraycopy(WRITE_FOOTER, 0, packet, WRITE_HEADER.length + payloadLength.length + payload.length, WRITE_FOOTER.length);

            writePacket(packet, R.string.device_data_acknowledged_packet);
        }
    }
}

package org.hwyl.sexytopo.comms.distox;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.model.survey.Leg;

import java.io.DataInputStream;
import java.io.DataOutputStream;


public class MeasurementProtocol extends DistoXProtocol {

    private static final int DISTANCE_LOW_BYTE = 1;
    private static final int DISTANCE_HIGH_BYTE = 2;
    private static final int AZIMUTH_LOW_BYTE = 3;
    private static final int AZIMUTH_HIGH_BYTE = 4;
    private static final int INCLINATION_LOW_BYTE = 5;
    private static final int INCLINATION_HIGH_BYTE = 6;
    // private static final int ROLL_ANGLE_HIGH_BYTE = 7; // currently not used

    public static final int DISTANCE_BIT_MASK = 0b01000000;


    protected byte[] previousPacket = new byte[]{};

    protected int duplicateCount = 0;

    public MeasurementProtocol(
            Context context, BluetoothDevice bluetoothDevice, SurveyManager dataManager) {
        super(context, bluetoothDevice, dataManager);
    }


    public static Leg parseDataPacket(byte[] packet) {

        int d0 = packet[ADMIN] & DISTANCE_BIT_MASK;
        int d1 = readByte(packet, DISTANCE_LOW_BYTE);
        int d2 = readByte(packet, DISTANCE_HIGH_BYTE);
        float distance = (d0 * 1024 + d2 * 256 + d1) / 1000.0f;

        float azimuth_reading =
                readDoubleByte(packet, AZIMUTH_LOW_BYTE, AZIMUTH_HIGH_BYTE);
        float azimuth = azimuth_reading * 180.0f / 32768.0f;

        float inclinationReading =
                readDoubleByte(packet, INCLINATION_LOW_BYTE, INCLINATION_HIGH_BYTE);
        float inclination = inclinationReading * 90.0f / 16384.0f;
        if (inclinationReading >= 32768) {
            inclination = (65536 - inclinationReading) * -90.0f / 16384.0f;
        }

        return new Leg(distance, azimuth, inclination);
    }


    public void go(DataInputStream inStream, DataOutputStream outStream) throws Exception {

        Log.device(context.getString(R.string.device_connection_ready));

        byte[] packet = readPacket(inStream);

        if (packet == null) {
            return;
        }

        acknowledge(outStream, packet);

        if (isDataPacket(packet)) {
            if (arePacketsTheSame(packet, previousPacket)) {
                Log.device("(Duplicated measurement #" + ++duplicateCount + ")");
            } else {
                duplicateCount = 0;
                Log.device(context.getString(R.string.device_data_received));
                Leg leg = parseDataPacket(packet);
                dataManager.updateSurvey(leg);
            }
        }

        previousPacket = packet;

    }

}

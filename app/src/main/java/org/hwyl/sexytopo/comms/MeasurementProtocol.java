package org.hwyl.sexytopo.comms;

import android.bluetooth.BluetoothDevice;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.activity.SexyTopoActivity;
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
    private static final int ROLL_ANGLE_HIGH_BYTE = 7;

    public static final int DISTANCE_BIT_MASK = 0b01000000;


    protected byte[] previousPacket = new byte[]{};

    protected int duplicateCount = 0;

    public MeasurementProtocol(
            SexyTopoActivity activity, BluetoothDevice bluetoothDevice, SurveyManager dataManager) {
        super(activity, bluetoothDevice, dataManager);
    }


    public static Leg parseDataPacket(byte[] packet) {

        int d0 = packet[ADMIN] & DISTANCE_BIT_MASK;
        int d1 = readByte(packet, DISTANCE_LOW_BYTE);
        int d2 = readByte(packet, DISTANCE_HIGH_BYTE);
        double distance = (d0 * 1024 + d2 * 256 + d1) / 1000.0;

        double azimuth_reading =
                readDoubleByte(packet, AZIMUTH_LOW_BYTE, AZIMUTH_HIGH_BYTE);
        double azimuth = azimuth_reading * 180.0 / 32768.0;

        double inclinationReading =
                readDoubleByte(packet, INCLINATION_LOW_BYTE, INCLINATION_HIGH_BYTE);
        double inclination = inclinationReading * 90.0 / 16384.0;
        if (inclinationReading >= 32768) {
            inclination = (65536 - inclinationReading) * -90.0 / 16384.0;
        }

        return new Leg(distance, azimuth, inclination);
    }


    public void go(DataInputStream inStream, DataOutputStream outStream) throws Exception {

        Log.device(activity.getString(R.string.device_log_ready_for_measurements));

        byte[] packet = readPacket(inStream);
        acknowledge(outStream, packet);

        if (isDataPacket(packet)) {
            if (arePacketsTheSame(packet, previousPacket)) {
                Log.device("(Duplicated measurement #" + ++duplicateCount + ")");
            } else {
                duplicateCount = 0;
                Log.device(activity.getString(R.string.device_log_received));
                Leg leg = parseDataPacket(packet);
                dataManager.updateSurvey(leg);
            }
        }

        previousPacket = packet;

    }


}

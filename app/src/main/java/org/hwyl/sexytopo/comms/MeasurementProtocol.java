package org.hwyl.sexytopo.comms;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.model.survey.Leg;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public class MeasurementProtocol extends DistoXProtocol {

    private static final int DISTANCE_LOW_BYTE = 1;
    private static final int DISTANCE_HIGH_BYTE = 2;
    private static final int AZIMUTH_LOW_BYTE = 3;
    private static final int AZIMUTH_HIGH_BYTE = 4;
    private static final int INCLINATION_LOW_BYTE = 5;
    private static final int INCLINATION_HIGH_BYTE = 6;
    private static final int ROLL_ANGLE_HIGH_BYTE = 7;

    protected byte[] previousPacket = new byte[]{};



    public MeasurementProtocol(
            Context context, BluetoothDevice bluetoothDevice, SurveyManager dataManager) {
        super(context, bluetoothDevice, dataManager);
    }


    public static Leg parseDataPacket(byte[] packet) {

        int d0 = packet[ADMIN] & 0x40;
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

        Leg leg = new Leg(distance, azimuth, inclination);
        return leg;
    }


    public void go(DataInputStream inStream, DataOutputStream outStream) throws Exception {

        Log.device(context.getString(R.string.device_log_ready_for_measurements));

        while(keepAlive()) {
            // at some point this may get expanded to consider other requests, such as turn on laser
            // but for now we just have one strategy
            readMeasurements(inStream, outStream);
            pauseForDistoXToCatchUp();
        }

    }


    public void readMeasurements(DataInputStream inStream, DataOutputStream outStream)
            throws IOException, InterruptedException {

        while (keepAlive()) {

            if (!isConnected()) {
                break;
            }

            byte[] packet = readPacket(inStream);
            sleep(100);
            acknowledge(outStream, packet);

            if (isDataPacket(packet)) {
                if (!arePacketsTheSame(packet, previousPacket)) {
                    Log.device(context.getString(R.string.device_log_received));
                    Leg leg = parseDataPacket(packet);
                    dataManager.updateSurvey(leg);
                }
            }

            previousPacket = packet;

            sleep(100);
        }

    }


}

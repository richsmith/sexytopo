package org.hwyl.sexytopo.comms.distox;

import android.bluetooth.BluetoothDevice;

import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.activity.SexyTopoActivity;
import org.hwyl.sexytopo.model.calibration.CalibrationReading;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public class CalibrationProtocol extends DistoXProtocol {

    public static final int ACCELERATION_ADMIN = 0;
    public static final int ACCELERATION_GX_LOW_BYTE = 1;
    public static final int ACCELERATION_GX_HIGH_BYTE = 2;
    public static final int ACCELERATION_GY_LOW_BYTE = 3;
    public static final int ACCELERATION_GY_HIGH_BYTE = 4;
    public static final int ACCELERATION_GZ_LOW_BYTE = 5;
    public static final int ACCELERATION_GZ_HIGH_BYTE = 6;
    public static final int ACCELERATION_NOT_USED = 7;

    public static final int MAGNETIC_ADMIN = 0;
    public static final int MAGNETIC_MX_LOW_BYTE = 1;
    public static final int MAGNETIC_MX_HIGH_BYTE = 2;
    public static final int MAGNETIC_MY_LOW_BYTE = 3;
    public static final int MAGNETIC_MY_HIGH_BYTE = 4;
    public static final int MAGNETIC_MZ_LOW_BYTE = 5;
    public static final int MAGNETIC_MZ_HIGH_BYTE = 6;
    public static final int MAGNETIC_NOT_USED = 7;

    private CalibrationReading calibrationReading = null;
    private int accelerationDuplicated = 0, magneticDuplicated = 0;


    public CalibrationProtocol(
            SexyTopoActivity activity, BluetoothDevice bluetoothDevice, SurveyManager dataManager) {
        super(activity, bluetoothDevice, dataManager);
    }


    @Override
    public void go(DataInputStream inStream, DataOutputStream outStream) throws Exception {

        if (calibrationReading == null) {
            calibrationReading = new CalibrationReading();
        }

        byte[] packet = readPacket(inStream);
        acknowledge(outStream, packet);


        PacketType packetType = PacketType.getType(packet);
        switch(packetType) {

            case CALIBRATION_ACCELERATION:
                if (calibrationReading.getState() ==
                        CalibrationReading.State.UPDATE_ACCELERATION) {
                    updateAccelerationSensorReading(packet, calibrationReading);
                    accelerationDuplicated = 0;
                } else {
                    Log.device("(Duplication " + ++accelerationDuplicated + ")");
                    checkExcessiveDuplication(accelerationDuplicated, inStream, outStream);
                }
                break;

            case CALIBRATION_MAGNETIC:
                if (calibrationReading.getState() ==
                        CalibrationReading.State.UPDATE_MAGNETIC) {
                    updateMagneticSensorReading(packet, calibrationReading);
                    magneticDuplicated = 0;
                } else {
                    Log.device("(Duplication " + ++magneticDuplicated + ")");
                    checkExcessiveDuplication(magneticDuplicated, inStream, outStream);
                }
                break;

            default:
                Log.device("(Not sure what this packet is)");
                break;
        }

        if (calibrationReading.getState() == CalibrationReading.State.COMPLETE) {
            Log.device("Completed cal reading :)");
            dataManager.addCalibrationReading(calibrationReading);
            calibrationReading = null;
        }
    }


    private void checkExcessiveDuplication(
            int count, DataInputStream inStream, DataOutputStream outStream)
            throws IOException{
        if (count >= 5) {
            inStream.close();
            outStream.close();
        }

    }


    private static void updateAccelerationSensorReading(byte[] packet, CalibrationReading reading) {
        int gx = readDoubleByte(packet, ACCELERATION_GX_LOW_BYTE, ACCELERATION_GX_HIGH_BYTE);
        int gy = readDoubleByte(packet, ACCELERATION_GY_LOW_BYTE, ACCELERATION_GY_HIGH_BYTE);
        int gz = readDoubleByte(packet, ACCELERATION_GZ_LOW_BYTE, ACCELERATION_GZ_HIGH_BYTE);
        reading.updateAccelerationValues(gx, gy, gz);
    }


    private static void updateMagneticSensorReading(byte[] packet, CalibrationReading reading) {
        int mx = readDoubleByte(packet, MAGNETIC_MX_LOW_BYTE, MAGNETIC_MX_HIGH_BYTE);
        int my = readDoubleByte(packet, MAGNETIC_MY_LOW_BYTE, MAGNETIC_MY_HIGH_BYTE);
        int mz = readDoubleByte(packet, MAGNETIC_MZ_LOW_BYTE, MAGNETIC_MZ_HIGH_BYTE);
        reading.updateMagneticValues(mx, my, mz);
    }


    public static int readDoubleByte(byte[] packet, int lowByteIndex, int highByteIndex) {
        int low = packet[lowByteIndex] & 0xff;
        int high = packet[highByteIndex] & 0xff;
        int combined = (high * 256) + low;

        if (combined > 32768)
            combined -= 65536;

        return combined;
    }

}

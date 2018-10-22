package org.hwyl.sexytopo.comms;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.model.calibration.CalibrationReading;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public class CalibrationProtocol extends DistoXProtocol {

    private static final int ACCELERATION_ADMIN = 0;
    private static final int ACCELERATION_GX_LOW_BYTE = 1;
    private static final int ACCELERATION_GX_HIGH_BYTE = 2;
    private static final int ACCELERATION_GY_LOW_BYTE = 3;
    private static final int ACCELERATION_GY_HIGH_BYTE = 4;
    private static final int ACCELERATION_GZ_LOW_BYTE = 5;
    private static final int ACCELERATION_GZ_HIGH_BYTE = 6;
    private static final int ACCELERATION_NOT_USED = 7;

    private static final int MAGNETIC_ADMIN = 0;
    private static final int MAGNETIC_MX_LOW_BYTE = 1;
    private static final int MAGNETIC_MX_HIGH_BYTE = 2;
    private static final int MAGNETIC_MY_LOW_BYTE = 3;
    private static final int MAGNETIC_MY_HIGH_BYTE = 4;
    private static final int MAGNETIC_MZ_LOW_BYTE = 5;
    private static final int MAGNETIC_MZ_HIGH_BYTE = 6;
    private static final int MAGNETIC_NOT_USED = 7;

    private static final byte START_CALIBRATION = 0b00110001;
    private static final byte STOP_CALIBRATION = 0b00110000;


    public CalibrationProtocol(
            Context context, BluetoothDevice bluetoothDevice, SurveyManager dataManager) {
        super(context, bluetoothDevice, dataManager);
    }



    @Override
    public void go(DataInputStream inStream, DataOutputStream outStream) throws Exception {

        CalibrationReading calibrationReading = new CalibrationReading();
        int accelerationDuplicated = 0, magneticDuplicated = 0;

        while(keepAlive()) {

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
                        Log.d("(Duplication " + ++accelerationDuplicated + ")");
                        checkExcessiveDuplication(accelerationDuplicated, inStream, outStream);
                    }
                    break;

                case CALIBRATION_MAGNETIC:
                    if (calibrationReading.getState() ==
                            CalibrationReading.State.UPDATE_MAGNETIC) {
                        updateMagneticSensorReading(packet, calibrationReading);
                        magneticDuplicated = 0;
                    } else {
                        Log.d("(Duplication " + ++magneticDuplicated + ")");
                        checkExcessiveDuplication(magneticDuplicated, inStream, outStream);
                    }
                    break;

                default:
                    Log.d("(Not sure what this packet is)");
                    break;
            }

            if (calibrationReading.getState() == CalibrationReading.State.COMPLETE) {
                Log.d("Completed cal reading :)");
                dataManager.addCalibrationReading(calibrationReading);
                calibrationReading = new CalibrationReading();
            }

            pauseForDistoXToCatchUp(); // seems to help

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


    public void startNewCalibration() throws Exception {
        dataManager.clearCalibrationReadings();
        writeCommandPacket(getStartCalibrationPacket());
    }


    public void cancelCalibration() throws Exception {
        dataManager.clearCalibrationReadings();
        writeCommandPacket(getStopCalibrationPacket());
    }


    public static byte[] getStartCalibrationPacket() {
        return new byte[] {START_CALIBRATION};
    }


    public static byte[] getStopCalibrationPacket() {
        return new byte[] {STOP_CALIBRATION};
    }


    public static void updateAccelerationSensorReading(byte[] packet, CalibrationReading reading)
            throws Exception {
        int gx = readDoubleByte(packet, ACCELERATION_GX_LOW_BYTE, ACCELERATION_GX_HIGH_BYTE);
        int gy = readDoubleByte(packet, ACCELERATION_GY_LOW_BYTE, ACCELERATION_GY_HIGH_BYTE);
        int gz = readDoubleByte(packet, ACCELERATION_GZ_LOW_BYTE, ACCELERATION_GZ_HIGH_BYTE);
        reading.updateAccelerationValues(gx, gy, gz);
    }

    public static void updateMagneticSensorReading(byte[] packet, CalibrationReading reading) {
        int mx = readDoubleByte(packet, MAGNETIC_MX_LOW_BYTE, MAGNETIC_MX_HIGH_BYTE);
        int my = readDoubleByte(packet, MAGNETIC_MY_LOW_BYTE, MAGNETIC_MY_HIGH_BYTE);
        int mz = readDoubleByte(packet, MAGNETIC_MZ_LOW_BYTE, MAGNETIC_MZ_HIGH_BYTE);
        reading.updateMagneticValues(mx, my, mz);
    }

}

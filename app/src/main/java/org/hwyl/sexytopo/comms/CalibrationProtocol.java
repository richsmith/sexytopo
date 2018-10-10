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
    private static final int ACCELERATION_ZERO_BYTE = 7;

    private static final int MAGNETIC_ADMIN = 0;
    private static final int MAGNETIC_MX_LOW_BYTE = 1;
    private static final int MAGNETIC_MX_HIGH_BYTE = 2;
    private static final int MAGNETIC_MY_LOW_BYTE = 3;
    private static final int MAGNETIC_MY_HIGH_BYTE = 4;
    private static final int MAGNETIC_MZ_LOW_BYTE = 5;
    private static final int MAGNETIC_MZ_HIGH_BYTE = 6;
    private static final int MAGNETIC_ZERO_BYTE = 7;

    private static final byte START_CALIBRATION = 0x31; // 00110001
    private static final byte STOP_CALIBRATION = 0x30; // 00110000

    private static final byte ACCELERATION_SENSOR = 0b0000010; // match first 7 bits // X0000010 assuming first bit is 0
    private static final byte MAGNETIC_SENSOR =     0b0000011; // match first 7 bits // X0000011 assuming first bit is 0
    private static final int IGNORE_LAST_BIT_MASK = 0b11111110;


    public CalibrationProtocol(
            Context context, BluetoothDevice bluetoothDevice, SurveyManager dataManager) {
        super(context, bluetoothDevice, dataManager);
    }

    @Override
    public void go(DataInputStream inStream, DataOutputStream outStream)
            throws IOException, Exception {
        while(keepAlive()) {
            readCalibration(inStream, outStream);
            sleep(100);
        }
    }

    private void readCalibration(DataInputStream inStream, DataOutputStream outStream)
            throws IOException, Exception {
        CalibrationReading calibrationReading = new CalibrationReading();

        try {
            getAccelerationReading(calibrationReading, inStream, outStream);
            getMagneticFieldReading(calibrationReading, inStream, outStream);

        } finally {
            if (calibrationReading.getState() == CalibrationReading.State.COMPLETE) {
                dataManager.addCalibrationReading(calibrationReading);
            }
        }

    }

    private void getAccelerationReading(CalibrationReading calibrationReading,
                                        DataInputStream inStream, DataOutputStream outStream)
            throws IOException, Exception {

        while(keepAlive()) {
            byte[] packet = readPacket(inStream);
            String packetString = Integer.toBinaryString(packet[0]);
            int readByte = readByte(packet, 0);
            String parsedPacketString = Integer.toBinaryString(readByte);

            Log.d("Received data: " + DistoXProtocol.describePacket(packet));
            Log.d("Received data: " + Integer.toBinaryString(packet[0]));

            if (isAccelerationSensorPacket(packet)) {
                if (calibrationReading.getState() == CalibrationReading.State.UPDATE_ACCELERATION) {
                    updateAccelerationSensorReading(packet, calibrationReading);
                }
                sleep(100);
                acknowledge(outStream, packet);

            } else if (isMagneticFieldSensorPacket(packet)) {
                if (calibrationReading.getState() == CalibrationReading.State.UPDATE_MAGNETIC) {
                    break; // don't acknowledge; we'll do that in the next step                } else {
                } else {
                    throw new IllegalStateException("Received calibration packet out of order (" +
                            parsedPacketString + ")");

                }
            } else {
                acknowledge(outStream, packet);
                // not sure if we need to do anything

            }

            sleep(100);

        }

    }

    private void getMagneticFieldReading(CalibrationReading calibrationReading,
                                         DataInputStream inStream, DataOutputStream outStream)
            throws IOException, Exception {

        while(keepAlive()) {
            byte[] packet = readPacket(inStream);
            String packetString = Integer.toBinaryString(packet[0]);
            int readByte = readByte(packet, 0);
            String parsedPacketString = Integer.toBinaryString(readByte);
            Log.d("Received data: " + MeasurementProtocol.describePacket(packet));

            if (isMagneticFieldSensorPacket(packet)) {
                if (calibrationReading.getState() == CalibrationReading.State.UPDATE_MAGNETIC) {
                    updateMagneticSensorReading(packet, calibrationReading);
                }
                acknowledge(outStream, packet);

            } else if (isAccelerationSensorPacket(packet) &&
                    calibrationReading.getState() == CalibrationReading.State.UPDATE_ACCELERATION) {
                break; // don't acknowledge; we'll do that in the next step

            } else {
                throw new IllegalStateException("Received calibration packet out of order (" +
                        parsedPacketString + ")");
            }
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


    public static boolean isAccelerationSensorPacket(byte[] packet) {
        // 0xFE = ignore last bit
        // 0x7F = ignore first bit
        return (readByte(packet, ACCELERATION_ADMIN) & IGNORE_LAST_BIT_MASK) == ACCELERATION_SENSOR;
    }

    public static boolean isMagneticFieldSensorPacket(byte[] packet) {
        return (readByte(packet, MAGNETIC_ADMIN) & IGNORE_LAST_BIT_MASK) == MAGNETIC_SENSOR;
    }


    public static void updateAccelerationSensorReading(byte[] packet, CalibrationReading reading)
            throws Exception {
        int gx = readDoubleByte(packet, ACCELERATION_GX_LOW_BYTE, ACCELERATION_GX_HIGH_BYTE);
        int gy = readDoubleByte(packet, ACCELERATION_GY_LOW_BYTE, ACCELERATION_GY_HIGH_BYTE);
        int gz = readDoubleByte(packet, ACCELERATION_GZ_LOW_BYTE, ACCELERATION_GZ_HIGH_BYTE);
        int zero = readByte(packet, ACCELERATION_ZERO_BYTE);
        /*
        if (zero != 1) {
            throw new Exception(
                "Something went wrong; malformed acceleration sensor packet (" +
                describePacket(packet) + ")");
        }*/
        reading.updateAccelerationValues(gx, gy, gz);
    }

    public static void updateMagneticSensorReading(byte[] packet, CalibrationReading reading)
            throws Exception {
        int mx = readDoubleByte(packet, MAGNETIC_MX_LOW_BYTE, MAGNETIC_MX_HIGH_BYTE);
        int my = readDoubleByte(packet, MAGNETIC_MY_LOW_BYTE, MAGNETIC_MY_HIGH_BYTE);
        int mz = readDoubleByte(packet, MAGNETIC_MZ_LOW_BYTE, MAGNETIC_MZ_HIGH_BYTE);
        int zero = readByte(packet, MAGNETIC_ZERO_BYTE);
        /*
        if (zero != 1) {
            throw new Exception(
                "Something went wrong; malformed magnetic sensor packet (" +
                describePacket(packet) + ")");
        }*/
        reading.updateMagneticValues(mx, my, mz);
    }

}

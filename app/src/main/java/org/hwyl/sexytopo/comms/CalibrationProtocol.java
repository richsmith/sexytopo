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
        writeCommandPacket(getStartCalibrationPacket());
    }


    public void stopCalibration() throws Exception {
        writeCommandPacket(getStopCalibrationPacket());
    }


    public static byte[] getStartCalibrationPacket() {
        return new byte[] {START_CALIBRATION};
    }


    public static byte[] getStopCalibrationPacket() {
        return new byte[] {STOP_CALIBRATION};
    }


    public static void updateAccelerationSensorReading(byte[] packet, CalibrationReading reading) {
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


    public boolean writeCalibration(byte[] coeff) throws Exception {

        Log.device("Attempting to write calibration data");

        oneOffConnect();

        DataInputStream inStream = new DataInputStream(socket.getInputStream());
        DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());

        try {
            int address = 0x8010;
            for (int i = 0; i < coeff.length; i += 4) {
                byte[] command = new byte[7];
                command[0] = 0x39;
                command[1] = (byte) address;
                command[2] = (byte) (address >> 8);
                command[3] = coeff[i];
                command[4] = coeff[i + 1];
                command[5] = coeff[i + 2];
                command[6] = coeff[i + 3];
                // send command and check reply
                Log.device("Writing calibration coefficient #" + i + "/" + coeff.length);
                outStream.write(command, 0, 7);

                byte[] reply = new byte[8];
                inStream.readFully(reply, 0, 8);
                checkCalibrationReply(address, reply);

                address += 4;
            }

            Log.device("Calibration writing successful");
            return true;

        } catch (Exception exception) {
            Log.device("Error writing calibration: " + exception.getMessage());
            return false;

        } finally {
            inStream.close();
            outStream.close();
        }
    }


    private void checkCalibrationReply(int address, byte[] reply) throws Exception {
        if (reply[0] != 0x38 ||
            (reply[1] != (byte)(address & 0xff)) ||
            (reply[2] != (byte)((address>>8) & 0xff))) {
            throw new Exception("Disto not happy with calibration writing attempt");
        }
    }

}

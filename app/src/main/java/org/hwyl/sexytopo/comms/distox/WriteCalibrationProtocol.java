package org.hwyl.sexytopo.comms.distox;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SexyTopo;
import org.hwyl.sexytopo.control.SurveyManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;


public class WriteCalibrationProtocol extends DistoXProtocol {

    private Byte[] coeff;

    private boolean isFinished = false;
    private boolean wasSuccessful = false;


    public WriteCalibrationProtocol(
            Context context, BluetoothDevice bluetoothDevice, SurveyManager dataManager) {
        super(context, bluetoothDevice, dataManager);
    }


    public void setCoeffToWrite(Byte[] coeff) {
        this.coeff = coeff;
    }


    public void go(DataInputStream inStream, DataOutputStream outStream) {
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
                String description = "#" + (i + 1) + "-" + (i + 4) + "/" + coeff.length;
                Log.device(R.string.device_distox_calibration_writing, description);
                outStream.write(command, 0, 7);

                byte[] reply = new byte[8];
                inStream.readFully(reply, 0, 8);
                checkCalibrationReply(address, reply);

                address += 4;
            }

            wasSuccessful = true;
            Log.device(R.string.device_distox_calibration_write_success);

        } catch (Exception exception) {
            wasSuccessful = false;
            Log.device(R.string.device_distox_calibration_write_error, exception.getMessage());

        } finally {
            isFinished = true;
        }

    }


    private void checkCalibrationReply(int address, byte[] reply) throws Exception {
        if (reply[0] != 0x38 ||
                (reply[1] != (byte)(address & 0xff)) ||
                (reply[2] != (byte)((address>>8) & 0xff))) {
            throw new Exception(
                    SexyTopo.staticGetString(R.string.device_distox_calibration_write_rejected));
        }
    }


    public boolean wasSuccessful() {
        return wasSuccessful;
    }

    public boolean isFinished() {
        return isFinished;
    }

}

package org.hwyl.sexytopo.comms;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.model.Leg;
import org.hwyl.sexytopo.control.SurveyManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by rls on 21/07/14.
 */
public class DistoXPoller extends Thread {

    private int POLLING_FREQUENCY = 5 * 1000;
    private int INTER_PACKET_DELAY = 1 * 100;

    private SurveyManager surveyManager;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket socket;


    private boolean isConnected = false;


    byte[] previousPacket = null;


    public DistoXPoller(BluetoothDevice bluetoothDevice, SurveyManager surveyManager) {
        this.surveyManager = surveyManager;
        this.bluetoothDevice = bluetoothDevice;
    }


    public void run() {
        while(true) {
            try {
                sleep(POLLING_FREQUENCY);

                if (! ensureConnection()) {
                    continue;
                }

                List<Leg> legs = slurpAllData(socket);
                surveyManager.updateSurvey(legs);

            } catch (Exception e) {
                Log.e(SexyTopo.TAG, "Error: " + e);
                disconnect();
            } finally {
                try {
                    socket.close();
                } catch (Exception e) {}
            }
        }



    }

    private boolean ensureConnection() {

        if (! isConnected) {
            try {
                socket = bluetoothDevice.createRfcommSocketToServiceRecord(SexyTopo.DISTO_X_UUID);
                socket.connect();
                isConnected = true;
            } catch (Exception e) {
                isConnected = false;
                Log.e(SexyTopo.TAG, "Error trying to connect to DistoX: " + e);
            }
        }

        return isConnected;
    }

    private void disconnect() {
        try {
            if (isConnected) {
                isConnected = false;
                socket.close();
            }
        } catch (Exception e) {
            Log.e(SexyTopo.TAG, "Error disconnecting from DistoX: " + e);
        }
    }



    public List<Leg> slurpAllData(BluetoothSocket socket) throws IOException, InterruptedException {



        Log.d(SexyTopo.TAG, "Checking if data are available...");

        List<Leg> legs = new ArrayList<>();


        DataInputStream inStream = new DataInputStream(socket.getInputStream());
        DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());



        while (true) {

            byte[] packet = new byte[8];
            inStream.readFully(packet, 0, 8);
            Log.d(SexyTopo.TAG, "Received data: " + DistoXProtocol.describeDataPacket(packet));

            //if (! DistoXProtocol.isDataPacket(dataPacket)) {
              //  continue;
            //}

            byte type = (byte)( packet[0] & 0x3f);
            Log.d(SexyTopo.TAG, "Type is " + type);

            byte[] acknowledgePacket = DistoXProtocol.createAcknowledgementPacket(packet);
            outStream.write(acknowledgePacket, 0, acknowledgePacket.length);
            Log.d(SexyTopo.TAG, "Sent Ack: " + DistoXProtocol.describeAcknowledgementPacket(acknowledgePacket));


            if (( packet[0] & 0x03) == 0) {
                Log.d(SexyTopo.TAG, "0x03 flag tripped (whatever that is...)");
                break;
            } else if (type != 0x01) {
                Log.d(SexyTopo.TAG, "packet not data type?");
                continue;
            }
            Log.d(SexyTopo.TAG, "packet does appear to be data type :)");

            if (previousPacket != null && arePacketsTheSame(packet, previousPacket)) {
                continue;

            } else {
                Leg leg = DistoXProtocol.parseDataPacket(packet);
                legs.add(leg);
                previousPacket = packet;
                break;
            }

            //pauseForDistoXToCatchUp();
        }

        int count = legs.size();
        count++;

        return legs;
    }

    private void pauseForDistoXToCatchUp() throws InterruptedException {
        sleep(INTER_PACKET_DELAY);
    }

    private boolean arePacketsTheSame(byte[] packet0, byte[] packet1) {
        for (int i = 0; i < packet0.length; i++) {
            if (packet0[i] != packet1[i]) {
                return false;
            }
        }
        return true;
    }






}

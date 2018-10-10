package org.hwyl.sexytopo.comms;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public abstract class DistoXProtocol extends Thread {


    private enum State {
        IDLE(false),
        RUNNING(true),
        STOP_REQUESTED(false),
        STOPPED(false);


        boolean active;

        State(boolean active) {
            this.active = active;
        }

        boolean isActive() {
            return active;
        }
    }

    protected volatile State state = State.IDLE;


    private static final int INTER_PACKET_DELAY = 1 * 12; // ms; DISTO repeats every 25 ms


    public static final int ADMIN = 0;
    public static final int SEQUENCE_BIT_MASK = 0b00000001; //0x80;
    public static final int ACKNOWLEDGEMENT_PACKET_BASE = 0b10101010; //0x55;

    protected Context context;
    private BluetoothDevice bluetoothDevice;

    protected SurveyManager dataManager;



    private BluetoothSocket socket;

    protected DistoXProtocol(
            Context context, BluetoothDevice bluetoothDevice, SurveyManager dataManager) {
        this.context = context;
        this.bluetoothDevice = bluetoothDevice;
        this.dataManager = dataManager;
    }


    protected void pauseForDistoXToCatchUp() throws InterruptedException {
        sleep(INTER_PACKET_DELAY);
    }


    /**
     * An acknowledgement packet consists of a single byte; bits 0-7 are 1010101 and bit 7 is the
     * same as the sequence bit of the packet being acknowledged.
     */
    public static byte[] createAcknowledgementPacket(byte[] packet) {
        byte sequenceBit = (byte)(packet[ADMIN] & SEQUENCE_BIT_MASK);
        byte[] acknowledgePacket = new byte[1];
        acknowledgePacket[0] = (byte)(sequenceBit | ACKNOWLEDGEMENT_PACKET_BASE);
        //acknowledgePacket[1] = (byte)0xfe;
        //acknowledgePacket[2] = (byte)0xff;
        System.out.print("ack packet is " + sequenceBit);

        return acknowledgePacket;
    }

    protected void acknowledge(DataOutputStream outStream, byte[] packet) throws IOException {
        byte[] acknowledgePacket = createAcknowledgementPacket(packet);
        outStream.write(acknowledgePacket, 0, acknowledgePacket.length);
        Log.d("Sent Ack: " + describeAcknowledgementPacket(acknowledgePacket));
    }


    protected void writeCommandPacket(byte[] packet) throws IOException, Exception {
        final int ATTEMPTS = 3;
        for (int i = 0; i < ATTEMPTS; i++) {
            tryToConnectIfNotConnected();
            if (!isConnected()) {
                sleep(100);
                continue;
            }
            DataOutputStream outStream = new DataOutputStream(socket.getOutputStream());
            outStream.write(packet, 0, packet.length);
            return;
        }

        throw new Exception("Couldn't send command");
    }


    protected static int readByte(byte[] packet, int index) {
        byte signed = packet[index];
        int unsigned = signed & 0xff; //
        /*if (data < 0) {
            data += 2^8;
        }*/
        return unsigned;
    }


    protected static int readDoubleByte(byte[] packet, int lowByteIndex, int highByteIndex) {
        int low = readByte(packet, lowByteIndex);
        int high = readByte(packet, highByteIndex);
        return (high * 2^8) + low;
    }


    public void run() {

        state = State.RUNNING;
        Log.device("Started communication thread");

        DataInputStream inStream = null;
        DataOutputStream outStream = null;

        while(keepAlive()) {
            try {
                tryToConnectIfNotConnected();

                if (!isConnected()) {
                    // FIXME sleep?
                    continue;
                }


                inStream = new DataInputStream(socket.getInputStream());
                outStream = new DataOutputStream(socket.getOutputStream());

                // pass control to subclass
                // (should only exit this state if requested by user or exception etc.)
                go(inStream, outStream);

            } catch(IOException e){
                if (e.getMessage().toLowerCase().contains("bt socket closed")) {
                    // this is common; probably don't need to bother the user with this..
                    disconnect();
                } else {
                    Log.device("Communication error: " + e.getMessage());
                    disconnect();
                }

            } catch(Exception exception){
                Log.device("General error: " + exception);
                disconnect();

            } finally {
                try {
                    inStream.close();
                    outStream.close();
                } catch (Exception e) {
                    // ignore any errors; they are expected if the socket has been closed
                }
            }
        }

        disconnect();

        state = State.STOPPED;
    }


    public abstract void go(DataInputStream inStream, DataOutputStream outStream) throws Exception;


    public void stopDoingStuff() {
        state = State.STOP_REQUESTED;
        interrupt();
    }

    protected boolean keepAlive() {
        return state.isActive();
    }


    protected boolean isConnected() {
        return (socket != null) && socket.isConnected();
    }


    public void tryToConnectIfNotConnected() {

        if (!isConnected()) {

            try {
                Log.device(context.getString(R.string.device_log_connecting));
                socket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(
                        SexyTopo.DISTO_X_UUID);
                socket.connect(); // blocks until connection is complete or fails with an exception

            } catch(Exception exception) {
                if (exception.getMessage().contains("socket might closed or timeout")) {
                    try {
                        Log.device(context.getString(R.string.device_trying_fallback));
                        socket = createFallbackSocket();
                        socket.connect();
                    } catch (Exception e) {
                        Log.device("Failed to create fallback socket: " + e.getMessage());
                    }
                } else {
                    Log.device("Error connecting: " + exception.getMessage());
                }

            } finally {
                if (socket.isConnected()) {
                    Log.device(context.getString(R.string.device_log_connected));
                } else {
                    Log.device(context.getString(R.string.device_log_not_connected));
                }
            }
        }
    }


    private BluetoothSocket createFallbackSocket() throws Exception {
        BluetoothSocket socket = (BluetoothSocket)
                bluetoothDevice.getClass()
                        .getMethod("createRfcommSocket", new Class[]{int.class})
                        .invoke(bluetoothDevice, 1);
        return socket;
    }


    private void disconnect() {
        try {
            if (socket != null && socket.isConnected()) {
                socket.close();
                Log.device(context.getString(R.string.device_log_stopped));
            }
        } catch (Exception e) {
            Log.device("Error disconnecting: " + e.getMessage());
        }
    }


    protected byte[] readPacket(DataInputStream inStream) throws IOException {
        byte[] packet = new byte[8];
        inStream.readFully(packet, 0, 8);
        Log.d("Read packet: " + describeAcknowledgementPacket(packet));

        return packet;
    }

    public static boolean arePacketsTheSame(byte[] packet0, byte[] packet1) {

        if (packet0 == null && packet1 == null) {
            return true; // not sure if we'll ever get in this situation, but technically true...?
        } else if (packet0 == null || packet1 == null) {
            return false;
        } else if (packet0.length != packet1.length) {
            return false;
        }


        for (int i = 0; i < packet0.length; i++) {
            if (packet0[i] != packet1[i]) {
                return false;
            }
        }

        return true;
    }

    protected static boolean isDataPacket(byte[] packet) {
        return (packet[0] & 0x3F) == 1;
    }


    public static String describePacket(byte[] packet) {
        String description = "[";
        for (int i = 0; i < packet.length; i++) {
            if (i == ADMIN) {
                description += Integer.toBinaryString(packet[i] & 0xFF);
            } else {
                description += ", " + packet[i];
            }
        }
        description += "]";
        return description;
    }


    public static String describeAcknowledgementPacket(byte[] acknowledgementPacket) {
        return "[" + Integer.toBinaryString(acknowledgementPacket[0] & 0xFF) + "]";
    }


}

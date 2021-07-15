package org.hwyl.sexytopo.comms.distox;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public abstract class DistoXProtocol {


    protected enum PacketType {
        MEASUREMENT(0b00000001),
        CALIBRATION_ACCELERATION(0b0000010),
        CALIBRATION_MAGNETIC(0b0000011),
        READ_REPLY(0b00111000),
        UNKNOWN(0b0);

        static int PACKET_TYPE_MASK = 0b00111111;
        private final int signature;
        PacketType(int signature) {
            this.signature = signature;
        }

        static PacketType getType(byte[] packet) {
            int signature = packet[ADMIN] & PACKET_TYPE_MASK;

            for (PacketType packetType : values()) {
                if (packetType.signature == signature) {
                    return packetType;
                }
            }

            return UNKNOWN;
        }
    }

    public static final int INTER_PACKET_DELAY = 100; // ms; (DISTO repeats every 25 ms for ref)
    public static final int WAIT_BETWEEN_CONNECTION_ATTEMPTS_MS = 5 * 1000;


    public static final int ADMIN = 0;

    public static final int SEQUENCE_BIT_MASK = 0b10000000;
    public static final int ACKNOWLEDGEMENT_PACKET_BASE = 0b01010101;

    protected Context context;
    protected SurveyManager dataManager;
    protected BluetoothDevice bluetoothDevice;

    protected DistoXProtocol(
            Context context, BluetoothDevice bluetoothDevice, SurveyManager dataManager) {
        this.context = context;
        this.bluetoothDevice = bluetoothDevice;
        this.dataManager = dataManager;
    }

    /**
     * An acknowledgement packet consists of a single byte; bits 0-7 are 1010101 and bit 7 is the
     * same as the sequence bit of the packet being acknowledged.
     */
    public static byte[] createAcknowledgementPacket(byte[] packet) {
        byte[] acknowledgePacket = new byte[1];
        acknowledgePacket[0] = (byte)
                ((packet[ADMIN] & SEQUENCE_BIT_MASK) | ACKNOWLEDGEMENT_PACKET_BASE);

        return acknowledgePacket;
    }

    protected void acknowledge(DataOutputStream outStream, byte[] packet) throws IOException {
        byte[] acknowledgePacket = createAcknowledgementPacket(packet);
        outStream.write(acknowledgePacket, 0, acknowledgePacket.length);
        outStream.flush();
        Log.device("Ack'd Packet: " + describeAcknowledgementPacket(acknowledgePacket));
    }


    protected void writeCommandPacket(DataOutputStream outStream, byte[] packet) throws Exception {
        try {
            outStream.write(packet, 0, packet.length);
        } finally {
            outStream.close();
        }

    }

    protected static int readByte(byte[] packet, int index) {
        byte signed = packet[index];
        int unsigned = signed & 0xff;
        return unsigned;
    }


    protected static int readDoubleByte(byte[] packet, int lowByteIndex, int highByteIndex) {
        int low = readByte(packet, lowByteIndex);
        int high = readByte(packet, highByteIndex);
        return (high * 256) + low;
    }



    public abstract void go(DataInputStream inStream, DataOutputStream outStream) throws Exception;



    protected byte[] readPacket(DataInputStream inStream) throws IOException {
        //if (inStream.available() < 1){
        //    return null;
        //}
        byte[] packet = new byte[8];
        inStream.readFully(packet, 0, 8);
        Log.device("Read packet: " + describePacket(packet));

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

    public static boolean isDataPacket(byte[] packet) {
        return PacketType.getType(packet) == PacketType.MEASUREMENT;
    }

    public static String describePacket(byte[] packet) {
        StringBuilder description = new StringBuilder("[");
        for (int i = 0; i < packet.length; i++) {
            if (i == ADMIN) {
                description.append(asBinaryString(packet[i] & 0xFF));
            } else {
                description.append(",\t").append(packet[i]);
            }
        }
        description.append("]");

        PacketType type = PacketType.getType(packet);
        description.append(" (").append(type).append(")");

        return description.toString();
    }

    public static String asBinaryString(int theByte) {
        return String.format("%8s", Integer.toBinaryString(theByte)).replace(' ', '0');
    }


    public static String describeAcknowledgementPacket(byte[] acknowledgementPacket) {
        return "[" + asBinaryString(acknowledgementPacket[0] & 0xFF) + "]";
    }


}

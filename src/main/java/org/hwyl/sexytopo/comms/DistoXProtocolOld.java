package org.hwyl.sexytopo.comms;

/* @file DistoXProtocolOld.java
 *
 * @author marco corvi
 * @date nov 2011
 *
 * @brief TopoDroidApp TopoDroidApp-DistoX BlueTooth connection
 * --------------------------------------------------------
 *  Copyright This sowftare is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 * CHANGES
 * 20120726 TopoDroidApp log
 */


import java.io.IOException;
import java.io.EOFException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.UUID;
// import java.Thread;
import java.nio.channels.ClosedByInterruptException;

// import android.bluetooth.BluetoothDevice;
// import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import org.hwyl.sexytopo.comms.TopoDroidApp;

public class DistoXProtocolOld
{
    // private DistoX mDistoX;
    // private BluetoothDevice  mDevice;
    private BluetoothSocket  mSocket = null;
    private DataInputStream  mIn;
    private DataOutputStream mOut;
    private byte[] m_head_tail;
    private byte[] m_addr_8000;
    private byte[] mAddress;   // request-reply address
    private byte[] mRequestBuffer;   // request buffer
    private byte[] mReplyBuffer;     // reply data
    private byte[] mAcknowledge;
    private byte[] mBuffer;
    private static final UUID MY_UUID = UUID.fromString( "00001101-0000-1000-8000-00805F9B34FB" );

    public static final int DISTOX_PACKET_NONE  = 0;
    public static final int DISTOX_PACKET_DATA  = 1;
    public static final int DISTOX_PACKET_G     = 2;
    public static final int DISTOX_PACKET_M     = 3;
    public static final int DISTOX_PACKET_REPLY = 4;

    public static final int DISTOX_ERR_HEADTAIL     = -1;
    public static final int DISTOX_ERR_HEADTAIL_IO  = -2;
    public static final int DISTOX_ERR_HEADTAIL_EOF = -3;
    public static final int DISTOX_ERR_CONNECTED    = -4;
    public static final int DISTOX_ERR_OFF          = -5; // distox has turned off

    private double mDistance;
    private double mBearing;
    private double mClino;
    private double mRoll;
    private long mGX, mGY, mGZ;
    private long mMX, mMY, mMZ;

    public double Distance() { return mDistance; }
    public double Compass()  { return mBearing; }
    public double Bearing()  { return mBearing; }
    public double Clino()    { return mClino; }
    public double Roll()     { return mRoll; }
    public long GX() { return mGX; }
    public long GY() { return mGY; }
    public long GZ() { return mGZ; }
    public long MX() { return mMX; }
    public long MY() { return mMY; }
    public long MZ() { return mMZ; }

    byte[] getAddress() { return mAddress; }
    byte[] getReply() { return mReplyBuffer; }

    boolean writtenCalib = false;
    public void setWrittenCalib( boolean b ) { writtenCalib = true; }

    public DistoXProtocolOld(BluetoothSocket socket /*, DistoX distox */)
    {

        // mDevice = device;
        mSocket = socket;
        // mDistoX = distox;

        m_head_tail = new byte[3];
        m_head_tail[0] = 0x38;
        m_head_tail[1] = 0x20; // address 0xC020
        m_head_tail[2] = (byte)0xC0;

        m_addr_8000 = new byte[3];
        m_addr_8000[0] = 0x38;
        m_addr_8000[1] = 0x00; // address 0x8000
        m_addr_8000[2] = (byte)0x80;

        mAddress = new byte[2];
        mReplyBuffer   = new byte[4];
        mRequestBuffer = new byte[8];

        mAcknowledge = new byte[1];
        // mAcknowledge[0] = ( b & 0x80 ) | 0x55;

        mBuffer = new byte[8];

        try {
            if ( mSocket != null ) {
                mIn  = new DataInputStream( mSocket.getInputStream() );
                mOut = new DataOutputStream( mSocket.getOutputStream() );
            }
        } catch ( IOException e ) {
            // NOTE socket is null there is nothing we can do
            TopoDroidApp.Log( TopoDroidApp.LOG_ERR, "Protocol cstr conn failed " + e.getMessage() );
        }
    }

    public int handlePacket( )
    {
        // StringWriter sw = new StringWriter();
        // PrintWriter pw = new PrintWriter( sw );
        // pw.format("%02x %02x %02x %02x %02x %02x %02x %02x", mBuffer[0], mBuffer[1], mBuffer[2],
        //     mBuffer[3], mBuffer[4], mBuffer[5], mBuffer[6], mBuffer[7] );
        // Log.v( TopoDroidApp.LOG_PROTO, "handlePacket " + sw.getBuffer().toString() );

        byte type = (byte)(mBuffer[0] & 0x3f);
        int high, low;
        switch ( type ) {
            case 0x01: // data
                int dhh = (int)( mBuffer[0] & 0x40 );
                int d1  = (int)(mBuffer[1] & 0xff); if ( d1 < 0 ) d1 += 256;
                int d2  = (int)(mBuffer[2] & 0xff); if ( d2 < 0 ) d2 += 256;
                // double d =  (((int)mBuffer[0]) & 0x40) * 1024.0 + (mBuffer[1] & 0xff) * 1.0 + (mBuffer[2] & 0xff) * 256.0;
                double d =  dhh * 1024.0 + d1 * 1.0 + d2 * 256.0;

                int b3 = (int)(mBuffer[3] & 0xff); if ( b3 < 0 ) b3 += 256;
                int b4 = (int)(mBuffer[4] & 0xff); if ( b4 < 0 ) b4 += 256;
                // double b = (mBuffer[3] & 0xff) + (mBuffer[4] & 0xff) * 256.0;
                double b = b3 + b4 * 256.0;

                int c5 = (int)(mBuffer[5] & 0xff); if ( c5 < 0 ) c5 += 256;
                int c6 = (int)(mBuffer[6] & 0xff); if ( c6 < 0 ) c6 += 256;
                // double c = (mBuffer[5] & 0xff) + (mBuffer[6] & 0xff) * 256.0;
                double c = c5 + c6 * 256.0;

                int r7 = (int)(mBuffer[7] & 0xff); if ( r7 < 0 ) r7 += 256;
                // double r = (mBuffer[7] & 0xff);
                double r = r7;

                mDistance = d / 1000.0;
                mBearing  = b * 180.0 / 32768.0; // 180/0x8000;
                mClino    = c * 90.0  / 16384.0; // 90/0x4000;
                if ( c >= 32768 ) { mClino = (65536 - c) * (-90.0) / 16384.0; }
                mRoll = r * 180.0 / 128.0;
                // pw.format(" %7.2f %6.1f %6.1f", mDistance, mBearing, mClino );
                // Log.v( TopoDroidApp.LOG_PROTO, "handlePacket " + sw.getBuffer().toString() );
                return DISTOX_PACKET_DATA;
            case 0x02: // g
                low  = (int)(mBuffer[1]&0xff); if ( low  < 0 ) low  += 256;
                high = (int)(mBuffer[2]&0xff); if ( high < 0 ) high += 256;
                mGX = low + high * 256;
                // mGX = (mBuffer[1]&0xff) + ((int)((mBuffer[2])&0xff) * 256 );

                low  = (int)(mBuffer[3]&0xff); if ( low  < 0 ) low  += 256;
                high = (int)(mBuffer[4]&0xff); if ( high < 0 ) high += 256;
                mGY = low + high * 256;
                // mGY = (mBuffer[3]&0xff) + ((int)((mBuffer[4])&0xff) * 256 );

                low  = (int)(mBuffer[5]&0xff); if ( low  < 0 ) low  += 256;
                high = (int)(mBuffer[6]&0xff); if ( high < 0 ) high += 256;
                mGZ = low + high * 256;
                // mGZ = (mBuffer[5]&0xff) + ((int)((mBuffer[6])&0xff) * 256 );

                if ( mGX > TopoDroidApp.ZERO ) mGX = mGX - TopoDroidApp.NEG;
                if ( mGY > TopoDroidApp.ZERO ) mGY = mGY - TopoDroidApp.NEG;
                if ( mGZ > TopoDroidApp.ZERO ) mGZ = mGZ - TopoDroidApp.NEG;
                // pw.format(" %createListOfLegsFromStation %createListOfLegsFromStation %createListOfLegsFromStation", mGX, mGY, mGZ );
                // Log.v( TopoDroidApp.LOG_PROTO, "handlePacket " + sw.getBuffer().toString() );
                return DISTOX_PACKET_G;
            case 0x03: // m
                low  = (int)(mBuffer[1]&0xff); if ( low  < 0 ) low  += 256;
                high = (int)(mBuffer[2]&0xff); if ( high < 0 ) high += 256;
                mMX = low + high * 256;
                // mMX = (mBuffer[1]&0xff) + ((int)((mBuffer[2])&0xff) * 256 );

                low  = (int)(mBuffer[3]&0xff); if ( low  < 0 ) low  += 256;
                high = (int)(mBuffer[4]&0xff); if ( high < 0 ) high += 256;
                mMY = low + high * 256;
                // mMY = (mBuffer[3]&0xff) + ((int)((mBuffer[4])&0xff) * 256 );

                low  = (int)(mBuffer[5]&0xff); if ( low  < 0 ) low  += 256;
                high = (int)(mBuffer[6]&0xff); if ( high < 0 ) high += 256;
                mMZ = low + high * 256;
                // mMZ = (mBuffer[5]&0xff) + ((int)((mBuffer[6])&0xff) * 256 );

                if ( mMX > TopoDroidApp.ZERO ) mMX = mMX - TopoDroidApp.NEG;
                if ( mMY > TopoDroidApp.ZERO ) mMY = mMY - TopoDroidApp.NEG;
                if ( mMZ > TopoDroidApp.ZERO ) mMZ = mMZ - TopoDroidApp.NEG;
                // pw.format(" %createListOfLegsFromStation %createListOfLegsFromStation %createListOfLegsFromStation", mMX, mMY, mMZ );
                // TopoDroidApp.Log( TopoDroidApp.LOG_PROTO, "handlePacket " + sw.getBuffer().toString() );
                return DISTOX_PACKET_M;
            case 0x38:
                mAddress[0] = mBuffer[1];
                mAddress[1] = mBuffer[2];
                mReplyBuffer[0] = mBuffer[3];
                mReplyBuffer[1] = mBuffer[4];
                mReplyBuffer[2] = mBuffer[5];
                mReplyBuffer[3] = mBuffer[6];
                // TopoDroidApp.Log( TopoDroidApp.LOG_PROTO, "handlePacket mReplyBuffer" );
                return DISTOX_PACKET_REPLY;
            // default:
            //   return DISTOX_PACKET_NONE;
        }
        return DISTOX_PACKET_NONE;
    }

    public int readPacket( )
    {
        try {
            mIn.readFully( mBuffer, 0, 8 );
            if ( (mBuffer[0] & 0x03) != 0 ) {
                mAcknowledge[0] = (byte)(( mBuffer[0] & 0x80 ) | 0x55);
                TopoDroidApp.Log( TopoDroidApp.LOG_PROTO, "readPacket byte ... writing ack");
                mOut.write( mAcknowledge, 0, 1 );
            }
            return handlePacket();
        } catch ( EOFException e ) {
            TopoDroidApp.Log( TopoDroidApp.LOG_PROTO, "readPacket EOFException" + e.toString() );
        } catch (ClosedByInterruptException e ) {
            TopoDroidApp.Log( TopoDroidApp.LOG_ERR, "readPacket ClosedByInterruptException" + e.toString() );
            // } catch (InterruptedException e ) {
            //   TopoDroidApp.Log( TopoDroidApp.LOG_ERR, "readPacket InterruptedException" + e.toString() );
        } catch (IOException e ) {
            // this is OK: the DistoX has been turned off
            TopoDroidApp.Log( TopoDroidApp.LOG_ERR, "readPacket IOException " + e.toString() + " OK distox turned off" );
            return DISTOX_ERR_OFF;
        }
        return DISTOX_PACKET_NONE;
    }

    public boolean sendCommand( byte cmd )
    {
        // StringWriter sw = new StringWriter();
        // PrintWriter pw = new PrintWriter( sw );
        // pw.format("Send command %02x", cmd );
        // TopoDroidApp.Log( TopoDroidApp.LOG_PROTO, "sendCommand " + sw.getBuffer().toString() );
        try {
            mRequestBuffer[0] = (byte)(cmd);
            mOut.write( mRequestBuffer, 0, 1 );
        } catch (IOException e ) {
            // TopoDroidApp.Log( TopoDroidApp.LOG_ERR, "sendCommand failed" );
            return false;
        }
        return true;
    }

    public int readToRead() // number of data-packet to read
    {
        try {
            mOut.write( m_head_tail, 0, 3 );
            mIn.readFully( mBuffer, 0, 8 );
            if ( mBuffer[0] != (byte)( 0x38 ) ) { return DISTOX_ERR_HEADTAIL; }
            if ( mBuffer[1] != m_head_tail[1] ) { return DISTOX_ERR_HEADTAIL; }
            if ( mBuffer[2] != m_head_tail[2] ) { return DISTOX_ERR_HEADTAIL; }
            int hlow = (int)(mBuffer[3]);
            int tlow = (int)(mBuffer[5]);
            if ( hlow < 0 ) { hlow += 256; }
            if ( tlow < 0 ) { tlow += 256; }
            int hhgh = (int)(mBuffer[4]);
            int thgh = (int)(mBuffer[6]);
            if ( hhgh < 0 ) { hhgh += 256; }
            if ( thgh < 0 ) { thgh += 256; }
            int head = hhgh * 256 + hlow;
            int tail = thgh * 256 + tlow;
            int ret = ( head >= tail )? (head-tail)/8 : ((0x8000 - tail) + head)/8;

            // DEBUG
            // StringWriter sw = new StringWriter();
            // PrintWriter pw = new PrintWriter( sw );
            // pw.format("%02x%02x-%02x%02x", mBuffer[4], mBuffer[3], mBuffer[6], mBuffer[5] );
            // TopoDroidApp.Log( TopoDroidApp.LOG_PROTO, "readToRead Head-Tail " + sw.getBuffer().toString() + " " + head + " - " + tail + " = " + ret);

            return ret;
        } catch ( EOFException e ) {
            TopoDroidApp.Log( TopoDroidApp.LOG_ERR, "readToRead Head-Tail read() failed" );
            return DISTOX_ERR_HEADTAIL_EOF;
        } catch (IOException e ) {
            TopoDroidApp.Log( TopoDroidApp.LOG_ERR, "readToRead Head-Tail read() failed" );
            int ignore = 0;
            int ingore = 1;
            return DISTOX_ERR_HEADTAIL_IO;
        }
    }

    public String readHeadTail()
    {
        try {
            mOut.write( m_head_tail, 0, 3 );
            mIn.readFully( mBuffer, 0, 8 );
            if ( mBuffer[0] != (byte)( 0x38 ) ) { return null; }
            if ( mBuffer[1] != m_head_tail[1] ) { return null; }
            if ( mBuffer[2] != m_head_tail[2] ) { return null; }
            // TODO value of m_head_tail in byte[3-7]
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter( sw );
            pw.format("%02x%02x-%02x%02x", mBuffer[4], mBuffer[3], mBuffer[6], mBuffer[5] );
            // TopoDroidApp.Log( TopoDroidApp.LOG_PROTO, "readHeadTail " + sw.getBuffer().toString() );
            return sw.getBuffer().toString();
        } catch ( EOFException e ) {
            TopoDroidApp.Log( TopoDroidApp.LOG_ERR, "readHeadTail read() failed" );
            return null;
        } catch (IOException e ) {
            TopoDroidApp.Log( TopoDroidApp.LOG_ERR, "readHeadTail read() failed" );
            return null;
        }
    }

    public boolean read8000( byte[] result )
    {
        try {
            mOut.write( m_addr_8000, 0, 3 );
            mIn.readFully( mBuffer, 0, 8 );
            if ( mBuffer[0] != (byte)( 0x38 ) ) { return false; }
            if ( mBuffer[1] != m_addr_8000[1] ) { return false; }
            if ( mBuffer[2] != m_addr_8000[2] ) { return false; }
            result[0] = mBuffer[3];
            result[1] = mBuffer[4];
            result[2] = mBuffer[5];
            result[3] = mBuffer[6];
        } catch ( EOFException e ) {
            TopoDroidApp.Log( TopoDroidApp.LOG_ERR, "read8000 read() EOF failed" );
            return false;
        } catch (IOException e ) {
            TopoDroidApp.Log( TopoDroidApp.LOG_ERR, "read8000 read() IO failed" );
            return false;
        }
        return true;
    }

    public boolean writeCalibration( byte[] calib )
    {
        long addr = 0x8010;
        long end  = addr + 48;
        try {
            int k = 0;
            while ( k < 48 ) {
                mBuffer[0] = 0x39;
                mBuffer[1] = (byte)( addr & 0xff );
                mBuffer[2] = (byte)( (addr>>8) & 0xff );
                mBuffer[3] = calib[k]; ++k;
                mBuffer[4] = calib[k]; ++k;
                mBuffer[5] = calib[k]; ++k;
                mBuffer[6] = calib[k]; ++k;
                mOut.write( mBuffer, 0, 7 );
                mIn.readFully( mBuffer, 0, 8 );
                // StringWriter sw = new StringWriter();
                // PrintWriter pw = new PrintWriter( sw );
                // pw.format("%02x %02x %02x %02x %02x %02x %02x %02x", mBuffer[0], mBuffer[1], mBuffer[2],
                //    mBuffer[3], mBuffer[4], mBuffer[5], mBuffer[6], mBuffer[7] );
                // TopoDroidApp.Log( TopoDroidApp.LOG_PROTO, "writeCalibration " + sw.getBuffer().toString() );
                if ( mBuffer[0] != 0x38 ) { return false; }
                if ( mBuffer[1] != (byte)( addr & 0xff ) ) { return false; }
                if ( mBuffer[2] != (byte)( (addr>>8) & 0xff ) ) { return false; }
                addr += 4;
            }
        } catch ( EOFException e ) {
            // TopoDroidApp.Log( TopoDroidApp.LOG_ERR, "writeCalibration EOF failed" );
            return false;
        } catch (IOException e ) {
            // TopoDroidApp.Log( TopoDroidApp.LOG_ERR, "writeCalibration IO failed" );
            return false;
        }
        return true;
    }

    public boolean readCalibration( byte[] calib )
    {
        int addr = 0x8010;
        int end  = addr + 48;
        try {
            int k = 0;
            while ( k < 48 ) {
                mBuffer[0] = 0x38;
                mBuffer[1] = (byte)( addr & 0xff );
                mBuffer[2] = (byte)( (addr>>8) & 0xff );
                mOut.write( mBuffer, 0, 3 );
                mIn.readFully( mBuffer, 0, 8 );
                if ( mBuffer[0] != 0x38 ) { return false; }
                if ( mBuffer[1] != (byte)( addr & 0xff ) ) { return false; }
                if ( mBuffer[2] != (byte)( (addr>>8) & 0xff ) ) { return false; }
                calib[k] = mBuffer[3]; ++k;
                calib[k] = mBuffer[4]; ++k;
                calib[k] = mBuffer[5]; ++k;
                calib[k] = mBuffer[6]; ++k;
                addr += 4;
            }
        } catch ( EOFException e ) {
            // TopoDroidApp.Log( TopoDroidApp.LOG_ERR, "readCalibration EOF failed" );
            return false;
        } catch (IOException e ) {
            // TopoDroidApp.Log( TopoDroidApp.LOG_ERR, "readCalibration IO failed" );
            return false;
        }
        return true;
    }

};

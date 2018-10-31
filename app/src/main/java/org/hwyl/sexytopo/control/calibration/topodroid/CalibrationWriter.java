package org.hwyl.sexytopo.control.calibration.topodroid;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;


public class CalibrationWriter {

    final private byte[] mBuffer = new byte[8];


    public boolean writeCalibration(byte[] calib, DataInputStream mIn, DataOutputStream mOut) {
        if ( calib == null ) return false;
        int  len  = calib.length;
        // Log.v("DistoX", "writeCalibration length " + len );
        long addr = 0x8010;
        // long end  = addr + len;
        try {
            int k = 0;
            while ( k < len ) {
                mBuffer[0] = 0x39;
                mBuffer[1] = (byte)( addr & 0xff );
                mBuffer[2] = (byte)( (addr>>8) & 0xff );
                mBuffer[3] = calib[k]; ++k;
                mBuffer[4] = calib[k]; ++k;
                mBuffer[5] = calib[k]; ++k;
                mBuffer[6] = calib[k]; ++k;
                mOut.write( mBuffer, 0, 7 );
                mIn.readFully( mBuffer, 0, 8 );
                // TDLog.Log( TDLog.LOG_PROTO, "writeCalibration " +
                //   String.format("%02x %02x %02x %02x %02x %02x %02x %02x", mBuffer[0], mBuffer[1], mBuffer[2],
                //   mBuffer[3], mBuffer[4], mBuffer[5], mBuffer[6], mBuffer[7] ) );
                if ( mBuffer[0] != 0x38 ) { return false; }
                if ( mBuffer[1] != (byte)( addr & 0xff ) ) { return false; }
                if ( mBuffer[2] != (byte)( (addr>>8) & 0xff ) ) { return false; }
                addr += 4;
            }
        } catch ( EOFException e ) {
            // TDLog.Error( "writeCalibration EOF failed" );
            return false;
        } catch (IOException e ) {
            // TDLog.Error( "writeCalibration IO failed" );
            return false;
        }
        return true;

    }

}

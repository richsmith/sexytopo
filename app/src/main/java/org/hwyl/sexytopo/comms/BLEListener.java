package org.hwyl.sexytopo.comms;

import org.hwyl.sexytopo.control.Log;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class BLEListener {
    private PipedInputStream readStream;
    private PipedOutputStream readStreamSource;

    public BLEListener() throws IOException {
        Log.d("BLEListener: readStream  = " + readStream);
        readStream = new PipedInputStream();
        readStreamSource = new PipedOutputStream(readStream);
        Log.d("BLEListener2: readStream  = " + readStream);
    }

    void onSerialConnect      () {
        Log.device("BLE Connected");
    }

    void onSerialConnectError (Exception e) {
        Log.e(e);
    }

    void onSerialRead         (byte[] data) {
        try {
            readStreamSource.write(data);
        } catch (IOException e) {
            Log.e(e);
        }
    }

    void onSerialIoError      (Exception e) {
        Log.e(e);
    }

    public PipedInputStream getInputStream() {
        return readStream;
    }


}

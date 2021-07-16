package org.hwyl.sexytopo.comms.sap5;

import org.hwyl.sexytopo.control.Log;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class BLEListener {
    private final PipedInputStream readStream;
    private final PipedOutputStream readStreamSource;

    public BLEListener() throws IOException {
        readStream = new PipedInputStream();
        readStreamSource = new PipedOutputStream(readStream);
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

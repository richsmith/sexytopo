package org.hwyl.sexytopo.comms.distox;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class DistoXSocket {

    private final BluetoothDevice bluetoothDevice;
    private final BluetoothSocket bluetoothSocket;
    private final InputStream inputStream;
    private final OutputStream outputStream;

    public DistoXSocket(BluetoothDevice device) throws Exception {
        bluetoothDevice = device;
        bluetoothSocket = connect();
        inputStream = bluetoothSocket.getInputStream();
        outputStream = bluetoothSocket.getOutputStream();
    }

    private BluetoothSocket connect() throws Exception{
        try {
            Log.device("Connecting...");
            BluetoothSocket bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(
                    SexyTopoConstants.DISTO_X_UUID);
            bluetoothSocket.connect(); // blocks until connection is complete or fails with an exception
            return bluetoothSocket;
        } catch(Exception exception) {
            String message = exception.getMessage();
            if (message != null && message.contains("socket might closed or timeout")) {
                Log.device("Unable to create classic socket, trying fallback...");
                BluetoothSocket bluetoothSocket = createFallbackSocket();
                bluetoothSocket.connect();
                return bluetoothSocket;
            } else {
                throw(exception);
            }

        }
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private BluetoothSocket createFallbackSocket() throws Exception {
        BluetoothSocket socket = (BluetoothSocket)
                bluetoothDevice.getClass()
                        .getMethod("createRfcommSocket", new Class[]{int.class})
                        .invoke(bluetoothDevice, 1);
        return socket;
    }

    public boolean isConnected() {
        return ((bluetoothSocket != null) && bluetoothSocket.isConnected());
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void close() throws IOException{
        bluetoothSocket.close();
    }
}

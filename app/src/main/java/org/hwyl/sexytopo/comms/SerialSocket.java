package org.hwyl.sexytopo.comms;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.hwyl.sexytopo.control.activity.DeviceActivity.DISTO_X_PREFIX;
import static org.hwyl.sexytopo.control.activity.DeviceActivity.SHETLAND_PREFIX;

public class SerialSocket {
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket classicSocket;
    private BLESocket bleSocket;
    private BLEListener bleListener;
    private InputStream inputStream;
    private OutputStream outputStream;

    public SerialSocket(BluetoothDevice device) throws Exception{
        bluetoothDevice = device;
        if (isBLE()) {
            BLEConnect();
            inputStream = bleListener.getInputStream();
            outputStream = new OutputStream() {
                @Override
                public void write(int i) throws IOException {
                    byte[] data = {(byte) i};
                    bleSocket.write(data);
                }
            };
            Log.d("BLE connection finished, streams are: " + inputStream + ", " + outputStream);
        } else {
            classicConnect();
            inputStream = classicSocket.getInputStream();
            outputStream = classicSocket.getOutputStream();
        }
    }

    protected boolean isBLE() {
        return (bluetoothDevice.getType() == BluetoothDevice.DEVICE_TYPE_LE);
    }

    protected void classicConnect() throws Exception{
        try {
            Log.device("Connecting...");
            classicSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(
                    SexyTopo.DISTO_X_UUID);
            classicSocket.connect(); // blocks until connection is complete or fails with an exception

        } catch(Exception exception) {
            String message = exception.getMessage();
            if (message != null && message.contains("socket might closed or timeout")) {
                Log.device("Unable to create classic socket, trying fallback...");
                classicSocket = createFallbackSocket();
                classicSocket.connect();
            } else {
                throw(exception);
            }

        }
    }

    protected BluetoothSocket createFallbackSocket() throws Exception {
        BluetoothSocket socket = (BluetoothSocket)
                bluetoothDevice.getClass()
                        .getMethod("createRfcommSocket", new Class[]{int.class})
                        .invoke(bluetoothDevice, 1);
        return socket;
    }


    protected void BLEConnect() throws Exception {
        Log.d("BLEConnect1");
        bleListener = new BLEListener();
        Log.d("BLEConnect2");
        bleSocket = new BLESocket();
        Log.d("BLEConnect3");
        bleSocket.connect(SexyTopo.context, bleListener, bluetoothDevice);
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() < startTime+8000) {
            if (bleSocket.isConnected()) return;
        }
        throw new Exception("BLE Connection timed out");
    }

    public boolean isConnected() {
        if (isBLE()) {
            return ((bleSocket != null) && bleSocket.isConnected());
        } else {
            return ((classicSocket != null) && classicSocket.isConnected());
        }
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void close() throws IOException{
        if (!isBLE()) {
            classicSocket.close();
        }
    }
}

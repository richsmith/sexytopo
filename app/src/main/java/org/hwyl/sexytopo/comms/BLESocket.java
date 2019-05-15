package org.hwyl.sexytopo.comms;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;


import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.Log;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.UUID;

public class BLESocket extends BluetoothGattCallback {
    private static final UUID RN4870_SERVICE = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455");
    private static final UUID RN4870_RX = UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3");
    private static final UUID RN4870_TX = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");

    class BLESerialPipedOutputClass extends PipedOutputStream {
        private BLESocket socket;

        public BLESerialPipedOutputClass(PipedInputStream snk, BLESocket socket) throws IOException {
            super(snk);
            this.socket = socket;
        }

        @Override
        public void write(@NonNull byte[] b) throws IOException {
            super.write(b);
            if (b.length > 0) {
                socket.startWrite();
            }
        }
    }

    private PipedInputStream inputStream, outStreamPipe;
    private PipedOutputStream inStreamPipe;
    private BLESerialPipedOutputClass outputStream;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic rxCharacter, txCharacter;
    private boolean connected = false;
    private boolean write_pending = false;

    public BLESocket(BluetoothDevice device) throws IOException {
        // setup our pipes
        inputStream = new PipedInputStream();
        inStreamPipe = new PipedOutputStream(inputStream);
        outStreamPipe = new PipedInputStream();
        gatt = device.connectGatt(SexyTopo.context,true, this);
        outputStream = new BLESerialPipedOutputClass(outStreamPipe, this);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        connected = (newState == BluetoothGatt.STATE_CONNECTED);
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        write_pending = false;
        BluetoothGattService service = gatt.getService(RN4870_SERVICE);
        if (service != null) {
            rxCharacter = service.getCharacteristic(RN4870_RX);
            txCharacter = service.getCharacteristic(RN4870_TX);
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (characteristic==txCharacter) {
            doWrite();
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (characteristic == rxCharacter) {
            byte [] data = characteristic.getValue();
            try {
                inStreamPipe.write(data);
            } catch (IOException e) {
                Log.e(e);
            }
        }
    }

    public void startWrite() {
        byte [] data;
        if (!write_pending) {
            doWrite();
        }
    }

    public void doWrite() {
        try {
            if (outStreamPipe.available() > 0) {
                byte[] data = new byte [] {};
                outStreamPipe.read(data, 0, 20);
                txCharacter.setValue(data);
                write_pending = true;
                gatt.writeCharacteristic(txCharacter);
            } else {
                write_pending = false;
            }
        } catch (IOException e) {
            Log.e(e);
        }
    }

    public PipedInputStream getInputStream() {
        return inputStream;
    }

    public PipedOutputStream getOutputStream() {
        return outputStream;
    }

    public boolean isConnected() {
        return connected;
    }
}

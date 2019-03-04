
package org.hwyl.sexytopo.comms;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.activity.SexyTopoActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.hwyl.sexytopo.comms.DistoXProtocol.WAIT_BETWEEN_CONNECTION_ATTEMPTS_MS;
import static org.hwyl.sexytopo.control.activity.DeviceActivity.DISTO_X_PREFIX;


public class DistoXCommunicator extends Thread {

    private CalibrationProtocol calibrationProtocol;

    public enum Protocol {
        NULL,
        MEASUREMENT,
        CALIBRATION
    }

    private enum ProtocolState {
        NOTHING,
        RUNNING_STANDARD,
        RUNNING_ONE_OFF
    }

    private DistoXProtocol currentProtocol = NullProtocol.INSTANCE;
    private DistoXProtocol requestedProtocol = null;
    private DistoXProtocol oneOffProtocol = null;

    private static final BluetoothAdapter BLUETOOTH_ADAPTER = BluetoothAdapter.getDefaultAdapter();
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket socket;

    DataInputStream inStream = null;
    DataOutputStream outStream = null;


    private SurveyManager dataManager;
    private SexyTopoActivity activity;


    private static DistoXCommunicator instance = null;


    private boolean keepAlive;

    public DistoXCommunicator(SexyTopoActivity activity, SurveyManager dataManager) {
        this.activity = activity;
        this.dataManager = dataManager;
    }


    protected void pauseForDistoXToCatchUp() throws InterruptedException {
        sleep(DistoXProtocol.INTER_PACKET_DELAY);
    }


    public void setProtocol(Protocol protocol) {

        switch(protocol) {
            case NULL:
                requestedProtocol = NullProtocol.INSTANCE;
                break;
            case MEASUREMENT:
                requestedProtocol =
                        new MeasurementProtocol(activity, bluetoothDevice, dataManager);
                break;
            case CALIBRATION:
                requestedProtocol =
                        new CalibrationProtocol(activity, bluetoothDevice, dataManager);
                break;
            default:
                Log.e("Unexpected protocol requested");
        }
    }


    public void startCalibration() {
        setProtocol(Protocol.NULL);
        disconnect(); // need to interrupt any reads in progress or we'll be waiting forever
        DistoXProtocol startCalibration =
                new StartCalibrationProtocol(activity, bluetoothDevice, dataManager);
        oneOff(startCalibration);
        setProtocol(Protocol.CALIBRATION);
    }


    public void stopCalibration() {
        setProtocol(Protocol.NULL);
        disconnect(); // need to interrupt any reads in progress or we'll be waiting forever
        DistoXProtocol stopCalibration =
                new StopCalibrationProtocol(activity, bluetoothDevice, dataManager);
        oneOff(stopCalibration);
        setProtocol(Protocol.MEASUREMENT);
    }


    public WriteCalibrationProtocol writeCalibration(byte[] coeff) {
        setProtocol(Protocol.NULL);
        disconnect(); // need to interrupt any reads in progress or we'll be waiting forever
        WriteCalibrationProtocol writeCalibration =
                new WriteCalibrationProtocol(activity, bluetoothDevice, dataManager);
        writeCalibration.setCoeffToWrite(coeff);
        oneOff(writeCalibration);
        setProtocol(Protocol.MEASUREMENT);
        return writeCalibration;
    }


    @Override
    public void start() {
        super.start();
    }


    public void requestStart(Protocol protocol) {

        setProtocol(protocol);

        if (isAlive()) {
            return;
        }

        keepAlive = true;
        bluetoothDevice = getDistoX();
        // fixme throw error if no bluetooth device
        start();
    }

    public void requestStop() {
        keepAlive = false;
    }


    public void run() {

        Log.device("Started communication thread");


        while(keepAlive) {

            if (requestedProtocol != null) {
                currentProtocol = requestedProtocol;
                requestedProtocol = null;
            }

            tryToConnectUntilConnected();

            if (oneOffProtocol == null) {
                communicate(currentProtocol);

            } else {
                communicate(oneOffProtocol);
                oneOffProtocol = null;
            }
        }

        try {
            inStream.close();
        } catch (Exception exception) {
            // ignore any errors; they are expected if the socket has been closed
        }
        try {
            outStream.close();
        } catch (Exception exception) {
            // ignore any errors; they are expected if the socket has been closed
        }

        disconnect();
    }


    private synchronized void communicate(DistoXProtocol protocol) {

        try {
            // ********* pass control to current strategy **********
            // (this is where all the work is done)
            protocol.go(inStream, outStream);

        } catch(IOException e){
            if (e.getMessage().toLowerCase().contains("bt socket closed")) {
                // this is common; probably don't need to bother the user with this..
                disconnect();
            } else {
                Log.device("Communication error: " + e.getMessage());
                disconnect();
            }

        } catch(Exception exception){
            Log.e(exception);
            Log.device("General device error: " + exception);
            disconnect();
        }
    }


    public void oneOff(DistoXProtocol protocol) {
        oneOffProtocol = protocol;
    }


    public void tryToConnectUntilConnected() {

        while(keepAlive && !isConnected()) {
            tryToConnectIfNotConnected();

            if (!isConnected()) {
                try {
                    sleep(WAIT_BETWEEN_CONNECTION_ATTEMPTS_MS);
                } catch (InterruptedException exception) {
                    break;
                }
                continue;
            }
        }

    }

    public void tryToConnectIfNotConnected() {

        if (isConnected()) {
            return;
        }

        try {
            Log.device(activity.getString(R.string.device_log_connecting));
            socket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(
                    SexyTopo.DISTO_X_UUID);
            socket.connect(); // blocks until connection is complete or fails with an exception

        } catch(Exception exception) {
            if (exception.getMessage().contains("socket might closed or timeout")) {
                try {
                    Log.device(activity.getString(R.string.device_trying_fallback));
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
                Log.device(activity.getString(R.string.device_log_connected));
            } else {
                Log.device(activity.getString(R.string.device_log_not_connected));
            }
        }

        try {
            inStream = new DataInputStream(socket.getInputStream());
            outStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException exception) {
            Log.device("Failed to create data streams :(");
            disconnect();
            return;
        }
    }

    private BluetoothSocket createFallbackSocket() throws Exception {
        BluetoothSocket socket = (BluetoothSocket)
                bluetoothDevice.getClass()
                        .getMethod("createRfcommSocket", new Class[]{int.class})
                        .invoke(bluetoothDevice, 1);
        return socket;
    }


    public void disconnect() {
        try {
            if (socket != null && socket.isConnected()) {
                socket.close();
                Log.device(activity.getString(R.string.device_log_stopped));
            }
        } catch (Exception e) {
            Log.device("Error disconnecting: " + e.getMessage());
        }
    }


    private static BluetoothDevice getDistoX() {
        Set<BluetoothDevice> distoXes = getPairedDistos();

        if (distoXes.size() != 1) {
            throw new IllegalStateException(distoXes.size() + " DistoXes paired");
        }

        return distoXes.toArray(new BluetoothDevice[]{})[0];
    }


    private static Set<BluetoothDevice> getPairedDistos() {

        if (BLUETOOTH_ADAPTER == null) {
            return new HashSet<>(0);
        }

        Set<BluetoothDevice> pairedDistoXes = new HashSet<>();
        Set<BluetoothDevice> pairedDevices = BLUETOOTH_ADAPTER.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (isDistoX(device)) {
                pairedDistoXes.add(device);
            }
        }

        return pairedDistoXes;
    }

    private static boolean isDistoX(BluetoothDevice device) {
        String name = device.getName();
        return name.toLowerCase().contains(DISTO_X_PREFIX.toLowerCase());
    }


    public boolean isConnected() {
        return (socket != null) && socket.isConnected();
    }

}
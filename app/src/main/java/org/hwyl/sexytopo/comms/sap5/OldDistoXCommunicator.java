
package org.hwyl.sexytopo.comms.sap5;

import static org.hwyl.sexytopo.comms.distox.DistoXProtocol.WAIT_BETWEEN_CONNECTION_ATTEMPTS_MS;
import static org.hwyl.sexytopo.control.activity.DeviceActivity.DISTO_X_PREFIX;
import static org.hwyl.sexytopo.control.activity.DeviceActivity.SHETLAND_PREFIX;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.comms.InstrumentType;
import org.hwyl.sexytopo.comms.distox.CalibrationProtocol;
import org.hwyl.sexytopo.comms.distox.DistoXProtocol;
import org.hwyl.sexytopo.comms.distox.MeasurementProtocol;
import org.hwyl.sexytopo.comms.distox.NullProtocol;
import org.hwyl.sexytopo.comms.distox.StartCalibrationProtocol;
import org.hwyl.sexytopo.comms.distox.StopCalibrationProtocol;
import org.hwyl.sexytopo.comms.distox.WriteCalibrationProtocol;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.activity.SexyTopoActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


/**
 * Most of this class is now outdated - replaced by DistoXCommunicator and DistoXThread in the
 * comms.distox package. A copy of the previous version is provided here to work with the existing
 * SAP5 code.
 */
public class OldDistoXCommunicator extends Thread {

    public enum Protocol {
        NULL,
        MEASUREMENT,
        CALIBRATION
    }

    public enum DistoXType {
        A3(false),
        X310(true);

        public final boolean preferNonLinearCalibration;

        DistoXType(boolean preferNonLinearCalibration) {
            this.preferNonLinearCalibration = preferNonLinearCalibration;
        }
    }

    private DistoXProtocol currentProtocol = NullProtocol.INSTANCE;
    private DistoXProtocol requestedProtocol = null;
    private DistoXProtocol oneOffProtocol = null;

    private static final BluetoothAdapter BLUETOOTH_ADAPTER = BluetoothAdapter.getDefaultAdapter();
    private BluetoothDevice bluetoothDevice;
    private SerialSocket socket;

    private DataInputStream inStream = null;
    private DataOutputStream outStream = null;

    private final SurveyManager dataManager;
    private final SexyTopoActivity activity;

    private boolean keepAlive;


    public OldDistoXCommunicator(SexyTopoActivity activity, SurveyManager dataManager) {
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


    public WriteCalibrationProtocol writeCalibration(Byte[] coeff) {
        setProtocol(Protocol.NULL);
        disconnect(); // need to interrupt any reads in progress or we'll be waiting forever
        WriteCalibrationProtocol writeCalibration =
                new WriteCalibrationProtocol(activity, bluetoothDevice, dataManager);
        writeCalibration.setCoeffToWrite(coeff);
        oneOff(writeCalibration);
        setProtocol(Protocol.MEASUREMENT);
        return writeCalibration;
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
            }
        }

    }

    public void tryToConnectIfNotConnected() {

        if (isConnected()) {
            return;
        }

        try {
            socket = new SerialSocket(bluetoothDevice);
            inStream = new DataInputStream(socket.getInputStream());
            outStream = new DataOutputStream(socket.getOutputStream());
        } catch(Exception e) {
            Log.device("Failed to create socket: " + e.getMessage());
        } finally {
            if (isConnected()) {
                Log.device(activity.getString(R.string.device_connection_connected));
            } else {
                Log.device(activity.getString(R.string.device_connection_not_connected));
            }
        }

    }


    public void disconnect() {
        try {
            if (socket != null && socket.isConnected()) {
                socket.close();
                Log.device(activity.getString(R.string.device_connection_closed));
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


    private static Set<BluetoothDevice> getPairedDistos() throws SecurityException {

        if (BLUETOOTH_ADAPTER == null) {
            return new HashSet<>(0);
        }

        Set<BluetoothDevice> pairedDistoXes = new HashSet<>();
        Set<BluetoothDevice> pairedDevices = BLUETOOTH_ADAPTER.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (isDistoX(device) || isShetland(device)) {
                pairedDistoXes.add(device);
            }
        }

        return pairedDistoXes;
    }

    private static boolean isDistoX(BluetoothDevice device) {
        String name = InstrumentType.describe(device);
        return name.toLowerCase().contains(DISTO_X_PREFIX.toLowerCase());
    }

    private static boolean isShetland(BluetoothDevice device) {
        String name = InstrumentType.describe(device);
        return name.toLowerCase().contains(SHETLAND_PREFIX.toLowerCase());
    }


    public boolean doesCurrentDistoPreferNonLinearCalibration() {
        String name = InstrumentType.describe(bluetoothDevice);
        if (name.startsWith("DistoX-")) {
            return DistoXType.X310.preferNonLinearCalibration;
        } else if (name.startsWith("DistoX")) {
            return DistoXType.A3.preferNonLinearCalibration;
        } else {
            return false; // shouldn't get here but linear is safer as default?
        }
    }


    public boolean isConnected() {
        return (socket != null) && socket.isConnected();
    }

}
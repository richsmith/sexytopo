
package org.hwyl.sexytopo.comms;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;

import java.util.HashSet;
import java.util.Set;

import static org.hwyl.sexytopo.control.activity.DeviceActivity.DISTO_X_PREFIX;


public class DistoXCommunicator {


    private static final BluetoothAdapter BLUETOOTH_ADAPTER = BluetoothAdapter.getDefaultAdapter();

    private SurveyManager dataManager;
    private Context context;


    private static DistoXCommunicator instance = null;


    private DistoXProtocol currentStrategy = new NullProtocol();


    private DistoXCommunicator(Context context, SurveyManager dataManager) {
        this.context = context;
        this.dataManager = dataManager;
    }

    public static synchronized DistoXCommunicator getInstance(
            Context context, SurveyManager dataManager) {
        if (instance == null) {
            instance = new DistoXCommunicator(context, dataManager);
        }
        return instance;
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



    public void startCalibration() throws Exception {


        if (currentStrategy instanceof CalibrationProtocol) {

            if (currentStrategy.keepAlive()) {
                Log.device("Calibration already running");
                return;
            } else {
                Log.device("Please wait while existing calibration thread stops");
                currentStrategy.stopDoingStuff();
            }
        } else if (currentStrategy instanceof MeasurementProtocol) {
            Log.device("Please wait while standard mode stops");
            currentStrategy.stopDoingStuff();
            return;
        }


        Log.device("Entering calibration mode");

        BluetoothDevice bluetoothDevice = getDistoX();
        CalibrationProtocol calibrationProtocol =
                new CalibrationProtocol(context, bluetoothDevice, dataManager);
        currentStrategy = calibrationProtocol;

        calibrationProtocol.startNewCalibration();

        currentStrategy.start();
    }

    public void stopDoingStuff() {
        currentStrategy.stopDoingStuff();
    }



    public void stopCalibration() throws Exception {
        if (currentStrategy instanceof CalibrationProtocol) {
            ((CalibrationProtocol)currentStrategy).cancelCalibration();
            currentStrategy.stopDoingStuff();
        }
    }

    public boolean doingSomething() {
        return currentStrategy.isAlive();
    }


    public void startMeasuring() {

        if (currentStrategy.isAlive()) {
            if (currentStrategy instanceof MeasurementProtocol) {
                return;
            } else if (currentStrategy instanceof CalibrationProtocol) {
                currentStrategy.stopDoingStuff();
                Log.device("Please wait while calibration mode stops");
                return;
            }

        }

        BluetoothDevice bluetoothDevice = getDistoX();
        MeasurementProtocol measurementProtocol =
                new MeasurementProtocol(context, bluetoothDevice, dataManager);
        Log.device("Entering measurement mode");

        currentStrategy = measurementProtocol;
        currentStrategy.start();
    }

    public void stopMeasuring() {
        if (currentStrategy instanceof MeasurementProtocol) {
            currentStrategy.stopDoingStuff();
        }
    }

    public void kill() {
        if (currentStrategy.isAlive()) {
            Log.device("Killing process " + currentStrategy.getClass().getName());
            currentStrategy.stop();
        }
    }

}
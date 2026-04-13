package org.hwyl.sexytopo.comms;

import android.bluetooth.BluetoothDevice;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.SexyTopo;

public class Instrument {

    private final InstrumentType instrumentType;
    private final BluetoothDevice bluetoothDevice;

    private final boolean isTest;

    public Instrument(BluetoothDevice bluetoothDevice) throws SecurityException {
        this.isTest = false;
        this.bluetoothDevice = bluetoothDevice;
        String reportedName = bluetoothDevice == null ? null : bluetoothDevice.getName();
        instrumentType = InstrumentType.byName(reportedName);
    }

    private Instrument() {
        isTest = true;
        bluetoothDevice = null;
        instrumentType = InstrumentType.TEST;
    }

    public static Instrument getTestInstrument() {
        return new Instrument();
    }

    public InstrumentType getInstrumentType() {
        return instrumentType;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public String getName() {
        if (isTest) {
            return instrumentType.describe();
        }

        try {
            return bluetoothDevice == null ? null : bluetoothDevice.getName();
        } catch (SecurityException e) {
            return null;
        }
    }

    public String describe() {
        if (isTest) {
            return this.getName();
        }
        return this.describe(bluetoothDevice);
    }

    public static String describe(BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            return SexyTopo.staticGetString(R.string.device_no_device);
        }
        try {
            return bluetoothDevice.getName();
        } catch (SecurityException e) {
            return SexyTopo.staticGetString(R.string.device_no_permitted_access);
        }
    }

    public String toString() {
        return describe();
    }
}

package org.hwyl.sexytopo.comms;

import android.bluetooth.BluetoothDevice;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.SexyTopo;

public class Instrument {

    private final InstrumentType instrumentType;
    private final BluetoothDevice bluetoothDevice;
    private final String name;

    private final boolean isTest;

    /** The name is passed in because Android may not know it for an unbonded device. */
    public Instrument(BluetoothDevice bluetoothDevice, String name) {
        this.isTest = false;
        this.bluetoothDevice = bluetoothDevice;
        this.name = name;
        instrumentType = InstrumentType.byName(name);
    }

    private Instrument() {
        isTest = true;
        bluetoothDevice = null;
        name = null;
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
        return name;
    }

    public String describe() {
        if (isTest || name != null) {
            return getName();
        }
        return describe(bluetoothDevice);
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

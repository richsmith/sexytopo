package org.hwyl.sexytopo.comms;

import android.bluetooth.BluetoothDevice;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.SexyTopo;

public class Instrument {

    private final InstrumentType instrumentType;
    private final BluetoothDevice bluetoothDevice;

    public Instrument(BluetoothDevice bluetoothDevice) throws SecurityException {
        this.bluetoothDevice = bluetoothDevice;
        String reportedName = bluetoothDevice == null ? null : bluetoothDevice.getName();
        instrumentType = InstrumentType.byName(reportedName);

    }

    public InstrumentType getInstrumentType() {
        return instrumentType;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public String describe() {
        if (bluetoothDevice == null) {
            return SexyTopo.staticGetString(R.string.device_no_device);
        }
        try {
            return bluetoothDevice.getName();
        } catch (SecurityException e) {
            return SexyTopo.staticGetString(R.string.device_no_permitted_access);
        }
    }



}

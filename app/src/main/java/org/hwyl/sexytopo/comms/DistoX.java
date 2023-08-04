package org.hwyl.sexytopo.comms;

import android.bluetooth.BluetoothDevice;

/**
 * DistoX subtype.
 */
public enum DistoX {
    A3("A3", false),
    X310("X310", true),

    BLE("BLE", true),

    UNKNOWN("???", false);

    private String name;
    private boolean preferNonLinearCalibration;

    DistoX(String name, boolean preferNonLinearCalibration) {
        this.name = name;
        this.preferNonLinearCalibration = preferNonLinearCalibration;
    }

    public static DistoX fromDevice(BluetoothDevice bluetoothDevice) {
        return fromName(InstrumentType.describe(bluetoothDevice));
    }

    public static DistoX fromName(String name) {
        // NB: order is important here
        if (name.startsWith("DistoXBLE")) {
            return BLE;
        } else if (name.startsWith("DistoX-")) {
            return X310;
        } else if (name.startsWith("DistoX")) {
            return A3;
        } else {
            return UNKNOWN;
        }
    }

    public boolean prefersNonLinearCalibration() {
        return preferNonLinearCalibration;
    }
}

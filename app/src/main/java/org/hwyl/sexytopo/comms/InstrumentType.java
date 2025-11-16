package org.hwyl.sexytopo.comms;

import android.bluetooth.BluetoothDevice;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.comms.bric4.Bric4Communicator;
import org.hwyl.sexytopo.comms.cavwayx1.CavwayX1Communicator;
import org.hwyl.sexytopo.comms.distox.DistoXCommunicator;
import org.hwyl.sexytopo.comms.distoxble.DistoXBleCommunicator;
import org.hwyl.sexytopo.comms.fcl.FCLCommunicator;
import org.hwyl.sexytopo.comms.missing.NullCommunicator;
import org.hwyl.sexytopo.comms.sap5.Sap5Communicator;
import org.hwyl.sexytopo.comms.sap6.SAP6Communicator;
import org.hwyl.sexytopo.control.SexyTopo;
import org.hwyl.sexytopo.control.activity.DeviceActivity;

import java.lang.reflect.InvocationTargetException;


public enum InstrumentType {

    // DISTOX BLE gets listed before DistoX due to the BT prefix overlap
    DISTOX_BLE(R.string.device_distox_ble_name, R.string.device_distox_ble_short_name, "DistoXBLE-", DistoXBleCommunicator.class),
    DISTOX(R.string.device_distox_name, R.string.device_distox_short_name, "DistoX", DistoXCommunicator.class),
    CAVWAY_X1_BLE(R.string.device_cavway_x1_name, R.string.device_cavway_x1_short_name, "CavwayX1-", CavwayX1Communicator.class),
    BRIC4(R.string.device_bric4_name, R.string.device_bric4_short_name, "BRIC4_", Bric4Communicator.class),
    BRIC5(R.string.device_bric5_name, R.string.device_bric5_short_name, "BRIC5_", Bric4Communicator.class),

    SAP5(R.string.device_sap5_name, R.string.device_sap5_short_name, "Shetland", Sap5Communicator.class),
    SAP6(R.string.device_sap6_name, R.string.device_sap6_short_name, "SAP6", SAP6Communicator.class),
    FCL(R.string.device_fcl_name, R.string.device_fcl_short_name, "FCL", FCLCommunicator.class),
    DISCOX(R.string.device_discox_name, R.string.device_discox_short_name, "DiscoX", SAP6Communicator.class),
    OTHER(R.string.device_unknown_name, R.string.device_unknown_short_name, "", NullCommunicator.class),
    NONE(R.string.device_missing_name, R.string.device_missing_short_name, "", NullCommunicator.class);

    private final int name;
    private final int shortName;
    private final String prefix;
    private final Class<? extends Communicator> communicator;

    InstrumentType(int name,
                   int shortName,
                   String bluetoothPrefix,
                   Class<? extends Communicator> communicator) {
        this.name = name;
        this.shortName = shortName;
        this.prefix = bluetoothPrefix;
        this.communicator = communicator;
    }

    public String describe() {
        return SexyTopo.staticGetString(name);
    }

    public String getShortName() {
        return SexyTopo.staticGetString(name);

    }

    public Communicator getNewCommunicator(
            DeviceActivity activity, BluetoothDevice bluetoothDevice)
            throws
                InstantiationException, IllegalAccessException,
                NoSuchMethodException, InvocationTargetException {

        return communicator
                .getDeclaredConstructor(DeviceActivity.class, BluetoothDevice.class)
                .newInstance(activity, bluetoothDevice);
    }

    public static InstrumentType byName(String name) {

        if (name == null) {
            return NONE;
        }

        for (InstrumentType instrumentType : values()) {
            if (instrumentType != OTHER && instrumentType != NONE &&
                    name.toLowerCase().startsWith(instrumentType.prefix.toLowerCase())) {
                return instrumentType;
            }
        }

        return OTHER;
    }

    public static InstrumentType byDevice(BluetoothDevice device) {
        if (device == null) {
            return NONE;
        }

        try {
            String name = device.getName();
            return byName(name);
        } catch (SecurityException e) {
            return NONE;
        }
    }

    public static String describe(BluetoothDevice device) {
        if (device == null) {
            return SexyTopo.staticGetString(R.string.device_no_device);
        }
        try {
            return device.getName();
        } catch (SecurityException e) {
            return SexyTopo.staticGetString(R.string.device_no_permitted_access);
        }
    }

    public boolean isUsable() {
        return this != NONE && this != OTHER;
    }
}

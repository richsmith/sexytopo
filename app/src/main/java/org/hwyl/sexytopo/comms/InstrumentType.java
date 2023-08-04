package org.hwyl.sexytopo.comms;

import android.bluetooth.BluetoothDevice;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.comms.bric4.Bric4Communicator;
import org.hwyl.sexytopo.comms.distox.DistoXCommunicator;
import org.hwyl.sexytopo.comms.distoxble.DistoXBleCommunicator;
import org.hwyl.sexytopo.comms.missing.NullCommunicator;
import org.hwyl.sexytopo.comms.sap5.Sap5Communicator;
import org.hwyl.sexytopo.comms.sap6.SAP6Communicator;
import org.hwyl.sexytopo.control.SexyTopo;
import org.hwyl.sexytopo.control.activity.DeviceActivity;

import java.lang.reflect.InvocationTargetException;


public enum InstrumentType {

    // DISTOX BLE gets listed before DistoX due to the BT prefix overlap
    DISTOX_BLE("DistoX BLE", "DistoX BLE", "DistoXBLE-", DistoXBleCommunicator.class),
    DISTOX("DistoX", "DistoX", "DistoX", DistoXCommunicator.class),
    BRIC4("BRIC4", "BRIC4", "BRIC4_", Bric4Communicator.class),
    SAP5("Shetland Attack Pony 5", "SAP5", "Shetland", Sap5Communicator.class),
    SAP6("Shetland Attack Pony 6", "SAP6", "SAP6", SAP6Communicator.class),
    OTHER("[Unknown device]", "Unknown", "", NullCommunicator.class),
    NONE("[Missing device]", "missing", "", NullCommunicator.class);

    private final String name;
    private final String shortName;
    private final String prefix;
    private final Class<? extends Communicator> communicator;

    InstrumentType(String name,
                   String shortName,
                   String bluetoothPrefix,
                   Class<? extends Communicator> communicator) {
        this.name = name;
        this.shortName = shortName;
        this.prefix = bluetoothPrefix;
        this.communicator = communicator;
    }

    public String describe() {
        return name;
    }

    public String getShortName() {
        return shortName;
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
}
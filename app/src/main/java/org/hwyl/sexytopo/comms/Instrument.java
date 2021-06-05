package org.hwyl.sexytopo.comms;

import android.bluetooth.BluetoothDevice;

import org.hwyl.sexytopo.comms.bric4.Bric4Communicator;
import org.hwyl.sexytopo.comms.distox.DistoXCommunicator;
import org.hwyl.sexytopo.comms.missing.NullCommunicator;
import org.hwyl.sexytopo.control.activity.DeviceActivity;

import java.lang.reflect.InvocationTargetException;


public enum Instrument {

    DISTOX("DistoX", "DistoX", "DistoX", DistoXCommunicator.class),
    BRIC4("BRIC4", "BRIC4", "BRIC4_", Bric4Communicator.class),
    SAP5("Shetland Attack Pony 5", "SAP5", "Shetland", DistoXCommunicator.class),
    OTHER("unknown device", "Unknown", "", NullCommunicator.class),
    NONE("missing device", "missing", "", NullCommunicator.class);

    private final String name;
    private final String shortName;
    protected final String prefix;
    protected final Class<? extends Communicator> communicator;

    Instrument(String name,
               String shortName,
               String bluetoothPrefix,
               Class<? extends Communicator> communicator) {
        this.name = name;
        this.shortName = shortName;
        this.prefix = bluetoothPrefix;
        this.communicator = communicator;
    }

    public String getName() {
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

    public static Instrument byName(String name) {

        for (Instrument instrument: values()) {
            if (instrument != OTHER && instrument != NONE &&
                    name.toLowerCase().startsWith(instrument.prefix.toLowerCase())) {
                return instrument;
            }
        }

        return OTHER;
    }

    public static Instrument byDevice(BluetoothDevice device) {
        if (device == null) {
            return NONE;
        }
        String name = device.getName();
        return byName(name);
    }

}
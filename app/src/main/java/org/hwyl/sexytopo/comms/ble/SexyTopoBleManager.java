package org.hwyl.sexytopo.comms.ble;

import android.content.Context;

import java.util.Map;

import no.nordicsemi.android.ble.BleManager;

/**
 * See https://github.com/NordicSemiconductor/Android-BLE-Library
 * for documentation for the library we're using
 */
public abstract class SexyTopoBleManager extends BleManager {


    public SexyTopoBleManager(Context context) {
        super(context);
    }

    public abstract Map<Integer, Integer> getCustomCommands();

    public abstract boolean handleCustomCommand(Integer id);


}

package org.hwyl.sexytopo.comms.ble;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.Log;

import no.nordicsemi.android.ble.callback.profile.ProfileDataCallback;
import no.nordicsemi.android.ble.data.Data;

public abstract class SexyTopoDataHandler implements ProfileDataCallback {

    @Override
    public void onInvalidDataReceived(@NonNull final BluetoothDevice device,
                                      @NonNull final Data data) {

        Log.device(R.string.device_ble_invalid_data_received);
    }
}

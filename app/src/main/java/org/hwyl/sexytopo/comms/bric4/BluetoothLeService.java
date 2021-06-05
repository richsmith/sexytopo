package org.hwyl.sexytopo.comms.bric4;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;


class BluetoothLeService extends Service {

    private Binder binder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }
}

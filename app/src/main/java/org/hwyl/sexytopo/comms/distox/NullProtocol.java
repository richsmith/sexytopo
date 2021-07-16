package org.hwyl.sexytopo.comms.distox;

import android.annotation.SuppressLint;

import java.io.DataInputStream;
import java.io.DataOutputStream;


public class NullProtocol extends DistoXProtocol {

    @SuppressLint("StaticFieldLeak")
    public static final NullProtocol INSTANCE = new NullProtocol();

    private NullProtocol() {
        super(null, null, null);
    }

    @Override
    public void go(DataInputStream inStream, DataOutputStream outStream) {
        // do nothing
    }
}

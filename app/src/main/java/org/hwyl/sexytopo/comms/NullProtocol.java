package org.hwyl.sexytopo.comms;

import java.io.DataInputStream;
import java.io.DataOutputStream;


public class NullProtocol extends DistoXProtocol {

    public static NullProtocol INSTANCE = new NullProtocol();

    private NullProtocol() {
        super(null, null, null);
    }

    @Override
    public void go(DataInputStream inStream, DataOutputStream outStream) {
        // do nothing
    }
}

package org.hwyl.sexytopo.comms;

import java.util.HashMap;
import java.util.Map;


public abstract class Communicator {

    protected static final Map<Integer, String> NO_COMMANDS = new HashMap<>();

    public abstract boolean isConnected();

    public abstract void requestConnect();

    public abstract void requestDisconnect();

    public Map<Integer, String> getCustomCommands() {
        return NO_COMMANDS;
    }

    public boolean handleCustomCommand(int viewId) {
        return false;
    }

}



package org.hwyl.sexytopo.comms.test;

import android.bluetooth.BluetoothDevice;
import android.view.View;
import java.util.HashMap;
import java.util.Map;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.comms.Communicator;
import org.hwyl.sexytopo.control.SexyTopo;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.activity.DeviceActivity;
import org.hwyl.sexytopo.model.survey.Leg;

public class TestCommunicator implements Communicator {

    private static final int SEND_MEASUREMENT_ID = View.generateViewId();

    private static final Map<Integer, Integer> CUSTOM_COMMANDS = new HashMap<>();

    static {
        CUSTOM_COMMANDS.put(SEND_MEASUREMENT_ID, R.string.device_command_take_shot);
    }

    @SuppressWarnings("unused")
    public TestCommunicator(DeviceActivity activity, BluetoothDevice bluetoothDevice) {
        // no-op; uses application context via SexyTopo
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void requestConnect() {
        // do nothing
    }

    @Override
    public void requestDisconnect() {
        // do nothing
    }

    @Override
    public Map<Integer, Integer> getCustomCommands() {
        return CUSTOM_COMMANDS;
    }

    @Override
    public boolean handleCustomCommand(int eventId) {
        if (eventId == SEND_MEASUREMENT_ID) {
            Leg leg = new Leg(1.0f, 0.0f, 0.0f);
            SurveyManager.getInstance(SexyTopo.context).updateSurvey(leg);
            return true;
        }
        return false;
    }
}

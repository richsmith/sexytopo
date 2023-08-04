package org.hwyl.sexytopo.comms.distox;

import org.hwyl.sexytopo.comms.Communicator;

public interface DistoXStyleCommunicator extends Communicator {

    void startCalibration();

    void stopCalibration();

    WriteCalibrationProtocol writeCalibration(Byte[] coefficients);
}

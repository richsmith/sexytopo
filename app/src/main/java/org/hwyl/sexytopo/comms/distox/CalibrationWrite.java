package org.hwyl.sexytopo.comms.distox;

/** An in-progress write of calibration coefficients to a DistoX, which may complete later. */
public interface CalibrationWrite {

    boolean isFinished();

    boolean wasSuccessful();
}

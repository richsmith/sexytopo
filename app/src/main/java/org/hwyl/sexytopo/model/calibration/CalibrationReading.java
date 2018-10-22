package org.hwyl.sexytopo.model.calibration;

public class CalibrationReading {

    public enum State {
        UPDATE_ACCELERATION,
        UPDATE_MAGNETIC,
        COMPLETE
    }

    private State state;

    private int gx, gy, gz;
    private int mx, my, mz;


    public CalibrationReading() {
        state = State.UPDATE_ACCELERATION;
    }


    public void updateAccelerationValues(int gx, int gy, int gz) {
        checkStateIs(State.UPDATE_ACCELERATION);
        this.gx = gx;
        this.gy = gy;
        this.gz = gz;
        state = State.UPDATE_MAGNETIC;
    }


    public void updateMagneticValues(int mx, int my, int mz) {
        checkStateIs(State.UPDATE_MAGNETIC);
        this.mx = mx;
        this.my = my;
        this.mz = mz;
        state = State.COMPLETE;
    }


    private void checkStateIs(State expected) {
        if (state != expected) {
            throw new IllegalStateException(
                    "Calibration state error: state is " + state + " but should be " + expected);
        }
    }


    public State getState() {
        return state;
    }

    public int getGx() {
        return gx;
    }


    public int getGy() {
        return gy;
    }


    public int getGz() {
        return gz;
    }

    public int getMx() {
        return mx;
    }


    public int getMy() {
        return my;
    }


    public int getMz() {
        return mz;
    }
}

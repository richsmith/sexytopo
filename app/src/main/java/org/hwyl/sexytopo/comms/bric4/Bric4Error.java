package org.hwyl.sexytopo.comms.bric4;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;


public enum Bric4Error {

    NO_ERROR(0, "no error"),
    ACCELEROMETER_1_HIGH_MAGNITUDE(1, "accelerometer 1 high magnitude"),
    ACCELEROMETER_2_HIGH_MAGNITUDE(2, "accelerometer 2 high magnitude"),
    MAGNETOMETER_1_HIGH_MAGNITUDE(3, "magnetometer 1 high magnitude"),
    MAGNETOMETER_2_HIGH_MAGNITUDE(4, "magnetometer 2 high magnitude"),
    ACCELEROMETER_DISPARITY(5, "accelerometer disparity"),
    MAGNETOMETER_DISPARITY(6, "magnetometer disparity"),
    TOO_FAST(7, "target moved too fast"),
    TOO_WEAK(8, "target didn't reflect"),
    TOO_REFLECTIVE(9, "target too reflective"),
    COMMUNICATION_ERROR(10, "communication error"),
    TIMEOUT(10, "message timeout"),
    UNRECOGNISED_ERROR(12, "unrecognised error"),
    WRONG_MESSAGE(13, "wrong message received"),
    INCLINATION_ERROR(14, "inclination calculation problem"),
    AZIMUTH_ERROR(15, "azimuth calculation problem");

    private final int code;
    private final String description;

    private static final Map<Integer, Bric4Error> codesToErrors = new HashMap<>();

    static {
        for (Bric4Error error : values()) {
            codesToErrors.put(error.code, error);
        }
    }

    Bric4Error(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    @NonNull
    public String toString() {
        return getDescription();
    }

    @NonNull
    public static Bric4Error fromCode(int code) {
        Bric4Error error = codesToErrors.get(code);
        if (error == null) {
            error = UNRECOGNISED_ERROR;
        }
        return error;
    }
}

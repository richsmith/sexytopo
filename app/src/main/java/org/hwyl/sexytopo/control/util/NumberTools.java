package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.SexyTopo;


public class NumberTools {

    public static boolean isWithinDelta(double first, double second) {
        double diff = Math.abs(first - second);
        return diff < SexyTopo.ALLOWED_DOUBLE_DELTA;
    }

}

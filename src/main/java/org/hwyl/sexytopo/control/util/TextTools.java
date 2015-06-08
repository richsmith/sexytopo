package org.hwyl.sexytopo.control.util;

/**
 * Created by rls on 08/06/15.
 */
public class TextTools {

    public static String pluralise(int n, String noun) {
        return n + " " + ((n == 1)? noun : noun + "s");
    }
}

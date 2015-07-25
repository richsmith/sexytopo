package org.hwyl.sexytopo.control.util;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Created by rls on 08/06/15.
 */
public class TextTools {

    public static String pluralise(int n, String noun) {
        return n + " " + ((n == 1)? noun : noun + "s");
    }

    public static String join(String joiner, Object ... list) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object item : list) {
            if (first) {
                first = false;
            } else {
                sb.append(joiner);
            }
            sb.append(item.toString());
        }
        return sb.toString();
    }

    public static String join(List<String> list, String joiner) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String item : list) {
            if (first) {
                first = false;
            } else {
                sb.append(joiner);
            }
            sb.append(item);
        }
        return sb.toString();
    }

    public static String format(Integer number) {
        DecimalFormat formatter = new DecimalFormat("#,##0");
        return formatter.format(number);
    }

    public static String formatTo2dp(Number number) {
        DecimalFormat formatter = new DecimalFormat("#,##0.00");
        return formatter.format(number);
    }
}

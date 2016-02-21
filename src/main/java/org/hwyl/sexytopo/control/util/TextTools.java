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
    

    public static String advanceLastNumber(String originatingName) {


        if (originatingName.length() == 0) {
            return "1";
        }

        int lastDigitChar = -1;
        int firstDigitChar = -1;

        for (int i = originatingName.length() - 1; i >= 0; i--) {
            char c = originatingName.charAt(i);


            if (lastDigitChar == -1 && Character.isDigit(c)) {
                lastDigitChar = i;
            }

            if (Character.isDigit(c)) {
                firstDigitChar = i;
            }

            if (! Character.isDigit(c) && firstDigitChar > -1) {
                break;
            }
        }

        if (lastDigitChar == -1) {
            return originatingName + "1";
        } else {
            int value = Integer.parseInt(originatingName.substring(firstDigitChar, lastDigitChar + 1));
            value++;
            String firstPart = originatingName.substring(0, firstDigitChar);
            String lastPart = originatingName.substring(lastDigitChar + 1);
            return firstPart + value + lastPart;
        }
    }

    public static String[] toArrayOfLines(String text) {
        return text.split("\\r?\\n");
    }
}

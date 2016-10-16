package org.hwyl.sexytopo.control.util;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;

/**
 * Created by rls on 08/06/15.
 */
public class TextTools {

    final static DecimalFormat dp0WithCommaFormatter = new DecimalFormat("#,##0");
    final static DecimalFormat dp2WithCommaFormatter = new DecimalFormat("#,##0.00");
    final static DecimalFormat dp2WithoutCommaFormatter = new DecimalFormat("##0.00");


    public static String pluralise(int n, String noun) {
        return n + " " + ((n == 1)? noun : noun + "s");
    }

    public static String joinAll(String joiner, Object ... list) {

        if (list.length == 1 && list[0] instanceof Collection) {
            throw new IllegalArgumentException("Wrong method called; should be join()");
        }

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

    public static String join(String joiner, List<?> list) {
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

    public static String formatWithComma(Integer number) {
        return dp0WithCommaFormatter.format(number);
    }

    public static String formatTo2dpWithComma(Number number) {
        return dp2WithCommaFormatter.format(number);
    }

    public static String formatTo2dp(Number number) {
        return dp2WithoutCommaFormatter.format(number);
    }

    public static String formatTo0dpWithComma(Number number) {
        return dp0WithCommaFormatter.format(number);
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

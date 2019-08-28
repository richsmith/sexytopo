package org.hwyl.sexytopo.control.util;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class TextTools {

    final static DecimalFormat dp0WithCommaFormatter = new DecimalFormat("#,##0");
    final static DecimalFormat dp2WithCommaFormatter = new DecimalFormat("#,##0.00");
    final static DecimalFormat dp2WithoutCommaFormatter = new DecimalFormat("##0.00");
    final static DecimalFormat dp2WithoutCommaFormatterUk =
        new DecimalFormat("##0.00", new DecimalFormatSymbols(Locale.UK));

    public final static DateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");


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


    public static String join(String joiner, Object ... items) {
        List<Object> list = Arrays.asList(items);
        return join(joiner, list);
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

    public static String formatTo2dpWithDot(Number number) {
        return dp2WithoutCommaFormatterUk.format(number);
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
            String oldDigitString = originatingName.substring(firstDigitChar, lastDigitChar + 1);
            Integer oldValue = Integer.parseInt(oldDigitString);
            Integer newValue = oldValue + 1;
            String newDigitString = newValue.toString();
            int lengthDifference = oldDigitString.length() - newDigitString.length();
            if (lengthDifference > 0) {
                String zeroPadding = new String(new char[lengthDifference]).replace("\0", "0");
                newDigitString = zeroPadding + newDigitString;
            }
            String firstPart = originatingName.substring(0, firstDigitChar);
            String lastPart = originatingName.substring(lastDigitChar + 1);
            return firstPart + newDigitString + lastPart;
        }
    }

    public static String[] toArrayOfLines(String text) {
        return text.split("\\r?\\n");
    }


    public static String toIsoDate(Date date) {
        return ISO_DATE_FORMAT.format(date);
    }
}

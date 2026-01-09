package org.hwyl.sexytopo.control.util;

import android.annotation.SuppressLint;
import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.activity.SexyTopoActivity;

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

    final static Character[] PROBLEMATIC = {' ', '\t', '\n', '\r', ':'};
    final static Character DEFAULT_JOINER = '-';

    @SuppressLint("SimpleDateFormat")
    public final static DateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");


    public static String pluralise(int n, String noun) {
        return n + " " + ((n == 1)? noun : noun + "s");
    }

    public static Character getJoiner(String text) {
        return text.contains("_")? '_' : DEFAULT_JOINER;
    }
    public static String intelligentlySanitise(String text) {
        Character joiner = getJoiner(text);
        for (Character c : PROBLEMATIC) {
            text = text.replace(c, joiner);
        }
        return text;
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

        if (originatingName.isEmpty()) {
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
            int oldValue = Integer.parseInt(oldDigitString);
            int newValue = oldValue + 1;
            String newDigitString = Integer.toString(newValue);
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

    public static String getFileAttribution(Context context) {
        String date = TextTools.toIsoDate(new Date());
        String app = context.getString(R.string.app_name) +
                " " + SexyTopoActivity.getVersionName(context);
        return context.getString(R.string.created_with, app, date);
    }
}

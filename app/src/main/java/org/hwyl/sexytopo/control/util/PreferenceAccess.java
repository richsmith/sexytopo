package org.hwyl.sexytopo.control.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


public class PreferenceAccess {

    public static SharedPreferences getPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static int getInt(Context context, String id, int defaultInt) {
        return Integer.parseInt(getPreferences(context)
                .getString(id, Integer.toString(defaultInt)));
    }

    public static float getFloat(Context context, String id, float defaultFloat) {
        return Float.parseFloat(getPreferences(context)
                .getString(id, Float.toString(defaultFloat)));
    }

    public static boolean getBoolean(Context context, String id, boolean defaultBoolean) {

        return getPreferences(context).getBoolean(id, defaultBoolean);
    }

    public static String getString(Context context, String id, String defaultString) {
        return getPreferences(context).getString(id, defaultString);
    }

    public static void setString(Context context, String id, String value) {
        SharedPreferences preferences = getPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(id, value);
        editor.apply();
    }

    public static void removeKey(Context context, String id) {
        SharedPreferences preferences = getPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(id);
        editor.apply();
    }
}

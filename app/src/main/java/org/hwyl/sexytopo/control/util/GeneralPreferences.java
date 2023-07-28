package org.hwyl.sexytopo.control.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;


public class GeneralPreferences {

    private static SharedPreferences prefs;

    public static void initialise(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private static String getString(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }

    private static boolean getBoolean(String key, Boolean defaultValue) {
        boolean value;

        try {
            value = prefs.getBoolean(key, defaultValue);
        } catch (ClassCastException cce) {
            String string = prefs.getString(key, defaultValue.toString());
            value = Boolean.parseBoolean(string);
        }
        return value;
    }

    private static int getInt(String key, Integer defaultValue) {
        int value = defaultValue;

        try {
            value = prefs.getInt(key, defaultValue);
        } catch (ClassCastException cce) {
            String string = prefs.getString(key, defaultValue.toString());
            try {
                value = Integer.parseInt(string);
            } catch (NumberFormatException nfe) {
                // Keep default
            }
        }

        return value;
    }

    private static float getFloat(String key, float defaultValue) {
        float value = defaultValue;

        try {
            value = prefs.getFloat(key, defaultValue);
        } catch (ClassCastException cce) {
            String string = prefs.getString(key, null);
            if (string != null) {
                try {
                    value = Float.parseFloat(string);
                } catch (NumberFormatException nfe) {
                    // Keep default
                }
            }
        }

        return value;
    }

    // ********** General ***********

    public static Uri getActiveSurveyUri() {
        String uriString = prefs.getString("pref_active_survey_uri", null);
        if (uriString == null) {
            return null;
        }

        try {
            return Uri.parse(uriString);
        } catch (Exception exception) {
            return null;
        }
    }


    public static void setActiveSurveyUri(Uri uri) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("pref_active_survey_uri", uri.toString());
        editor.apply();
    }

    public static String getTheme() {
        return getString("pref_theme", "auto");
    }
    public static boolean isDevModeOn() {
        return getBoolean("pref_developer_mode", false);
    }

    public static String getOrientationMode() {
        return getString("pref_orientation", "auto");
    }

    public static boolean isDeletePathFragmentsModeOn() {
        return getBoolean("pref_delete_path_fragments", true);
    }

    public static boolean isHighlightLatestLegModeOn() {
        return getBoolean("pref_highlight_latest_leg", true);
    }


    // ********** Table ***********

    public static boolean isManualLrudModeOn() {
        return getBoolean("pref_lrud_fields", false);
    }

    public static boolean isDegMinsSecsModeOn() {
        return getBoolean("pref_deg_mins_secs", false);
    }

    // ********** Surveying ***********

    public static float getMaxDistanceDelta() {
        return getFloat("pref_max_distance_delta", 0.05f);
    }

    public static float getMaxAngleDelta() {
        return getFloat("pref_max_angle_delta", 1.7f);
    }

    public static boolean isHotCornersModeActive() {
        return getBoolean("pref_hot_corners", true);
    }

    // ********** Sketching ***********
    public static boolean isAntialiasingModeOn() {
        return getBoolean("pref_anti_alias", false);
    }

    public static int getStationCrossDiameterPixels() {
        return getInt("pref_station_diameter", 16);
    }

    public static int getLegStrokeWidth() {
        return getInt("pref_leg_width", 3);
    }

    public static int getSplayStrokeWidth() {
        return getInt("pref_splay_width", 1);
    }

    public static int getLegendFontSizeSp() {
        return getInt("pref_legend_font_size_sp", 10);
    }

    public static int getLabelFontSizeSp() {
        return getInt("pref_label_font_size_sp", 12);
    }

    public static int getStationLabelFontSizeSp() {
        return getInt("pref_station_label_font_size_sp", 10);
    }

    public static int getTextStartingSizePixels() {
        return getInt("pref_survey_text_tool_font_size", 50);
    }

    public static int getSymbolStartingSizePixels() {
        return getInt("pref_survey_symbol_size", 35);
    }

    // ********** Calibration ***********

    public static String getCalibrationAlgorithm() {
        return getString("pref_calibration_algorithm", "linear");
    }


}

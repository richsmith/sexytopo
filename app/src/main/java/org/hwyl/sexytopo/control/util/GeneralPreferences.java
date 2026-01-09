package org.hwyl.sexytopo.control.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.preference.PreferenceManager;

import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.model.sketch.Colour;


public class GeneralPreferences {

    private static SharedPreferences prefs;

    public static void initialise(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private static String getString(String key, String defaultValue) {

        if (prefs == null) {  // e.g. during tests
            return defaultValue;
        }

        return prefs.getString(key, defaultValue);
    }

    private static boolean getBoolean(String key, Boolean defaultValue) {

        if (prefs == null) {  // e.g. during tests
            return defaultValue;
        }

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

        if (prefs == null) {  // e.g. during tests
            return defaultValue;
        }

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

        if (prefs == null) {  // e.g. during tests
            return defaultValue;
        }

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

    public static boolean isImmersiveModeOn() {
        return getBoolean("pref_immersive_mode", false);
    }

    public static void setImmersiveMode(boolean enabled) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("pref_immersive_mode", enabled);
        editor.apply();
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

    public static boolean isTwoFingerModeActive() {
        return getBoolean("pref_two_finger_movement", false);
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

    public static int getTextStartingSizeSp() {
        return getInt("pref_survey_text_tool_font_size_sp", 16);
    }

    public static int getSymbolStartingSizeDp() {
        return getInt("pref_survey_symbol_size", 25);
    }

    // ********** Calibration ***********

    public static String getCalibrationAlgorithm() {
        return getString("pref_calibration_algorithm", "linear");
    }

    // ********** Export ***********
    public static Colour getExportSvgBackgroundColour() {
        String colour = getString("pref_export_svg_background", "transparent");
        Log.i("Colour is " + colour);
        if (colour.equalsIgnoreCase("transparent")) {
            return Colour.TRANSPARENT;
        } else {
            return Colour.WHITE;
        }
    }

    public static int getExportSvgStrokeWidth() {
        return getInt("pref_export_svg_stroke_width", 1);
    }

    public static int getExportSvgLegStrokeWidth() {
        return getInt("pref_export_svg_leg_width", 2);
    }

    public static int getExportSvgSplayStrokeWidth() {
        return getInt("pref_export_svg_splay_width", 1);
    }

    public static boolean isXviExportSymbolsEnabled() {
        return getBoolean("pref_therion_export_symbols", true);
    }

    public static boolean isXviExportTextEnabled() {
        return getBoolean("pref_therion_export_text", true);
    }


}

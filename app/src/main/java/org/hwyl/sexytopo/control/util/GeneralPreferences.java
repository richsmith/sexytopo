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
        return prefs.getString("pref_theme", "auto");
    }
    public static boolean isDevModeOn() {
        return prefs.getBoolean("pref_developer_mode", false);
    }

    public static String getOrientationMode() {
        return prefs.getString("pref_orientation", "auto");
    }

    public static boolean isDeletePathFragmentsModeOn() {
        return prefs.getBoolean("pref_delete_path_fragments", true);
    }

    public static boolean isHighlightLatestLegModeOn() {
        return prefs.getBoolean("pref_highlight_latest_leg", true);
    }


    // ********** Table ***********

    public static boolean isManualLrudModeOn() {
        return prefs.getBoolean("pref_lrud_fields", false);
    }

    public static boolean isDegMinsSecsModeOn() {
        return prefs.getBoolean("pref_deg_mins_secs", false);
    }

    // ********** Surveying ***********

    public static float getMaxDistanceDelta() {
        return prefs.getFloat("pref_max_distance_delta", 0.05f);
    }

    public static float getMaxAngleDelta() {
        return prefs.getFloat("pref_max_angle_delta", 1.7f);
    }

    public static boolean isHotCornersModeActive() {
        return prefs.getBoolean("pref_hot_corners", true);
    }

    // ********** Sketching ***********
    public static boolean isAntialiasingModeOn() {
        return prefs.getBoolean("pref_anti_alias", false);
    }

    public static int getStationCrossDiameterPixels() {
        return prefs.getInt("pref_station_diameter", 16);
    }

    public static int getLegStrokeWidth() {
        return prefs.getInt("pref_leg_width", 3);
    }

    public static int getSplayStrokeWidth() {
        return prefs.getInt("pref_splay_width", 1);
    }

    public static int getLegendFontSizeSp() {
        return prefs.getInt("pref_legend_font_size_sp", 10);
    }

    public static int getLabelFontSizeSp() {
        return prefs.getInt("pref_label_font_size_sp", 12);
    }

    public static int getStationLabelFontSizeSp() {
        return prefs.getInt("pref_station_label_font_size_sp", 10);
    }

    public static int getTextStartingSizePixels() {
        return prefs.getInt("pref_survey_text_tool_font_size", 50);
    }

    public static int getSymbolStartingSizePixels() {
        return prefs.getInt("pref_survey_symbol_size", 35);
    }

    // ********** Calibration ***********

    public static String getCalibrationAlgorithm() {
        return prefs.getString("pref_calibration_algorithm", "linear");
    }


}

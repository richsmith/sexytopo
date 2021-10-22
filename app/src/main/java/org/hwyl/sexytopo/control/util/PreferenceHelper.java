package org.hwyl.sexytopo.control.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.hwyl.sexytopo.SexyTopo;

public class PreferenceHelper {

    private static SharedPreferences sharedPrefs;

    // Initializes a sharedPreferences instance for key-getting around the app
    public static void initPreferences(final Context context) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private static boolean getHelperBoolean(final String key, final boolean defValue) {
        return sharedPrefs.getBoolean(key, defValue);
    }

    private static int getHelperInt(final String key, final int defValue) {
        return sharedPrefs.getInt(key, defValue);
    }

    private static String getHelperString(final String key, final String defValue) {
        return sharedPrefs.getString(key, defValue);
    }

    private static float getHelperFloat(final String key, final float defValue) {
        return sharedPrefs.getFloat(key, defValue);
    }

    /**
     * Preference keys
     */

    ////////////////////////////////////////////////////////////////////
    // General
    ////////////////////////////////////////////////////////////////////
    public static String orientation() {
        return getHelperString("pref_orientation", "orientation_auto");
    }

    public static boolean vibrateOnNewStation() {
        return getHelperBoolean("pref_vibrate_on_new_station", false);
    }

    public static boolean hotCorners() {
        return getHelperBoolean("pref_hot_corners", false);
    }


    ////////////////////////////////////////////////////////////////////
    // Disto-X Communication
    ////////////////////////////////////////////////////////////////////
    public static float maxDistanceDelta() {
        return getHelperFloat("pref_max_distance_delta", 0.05f);
    }

    public static float maxAngleDelta() {
        return getHelperFloat("pref_max_angle_delta", 1.7f);
    }


    ////////////////////////////////////////////////////////////////////
    // Calibration
    ////////////////////////////////////////////////////////////////////
    public static String calibrationAlgorithm() {
        return getHelperString("pref_calibration_algorithm", "algo_auto");
    }


    ////////////////////////////////////////////////////////////////////
    // Sketching
    ////////////////////////////////////////////////////////////////////
    public static boolean deleteLineFragments() {
        return getHelperBoolean("pref_delete_path_fragments", true);
    }

    public static boolean highlightLatestLeg() {
        return getHelperBoolean("pref_key_highlight_latest_leg", true);
    }

    public static int textToolFontSize() {
        return getHelperInt("pref_survey_text_tool_font_size", 50);
    }

    public static int surveySymbolSize() {
        return getHelperInt("pref_survey_symbol_size", 35);
    }

    public static int legStrokeWidth() {
        return getHelperInt("pref_leg_width", 3);
    }

    public static int splayStrokeWidth() {
        return getHelperInt("pref_splay_width", 1);
    }

    public static int stationDiameter() {
        return getHelperInt("pref_station_diameter", 16);
    }

    public static int labelFontSize() {
        return getHelperInt("pref_station_label_font_size", 22);
    }

    public static boolean applyAntiAlias() {
        return getHelperBoolean("pref_key_anti_alias", false);
    }


    ////////////////////////////////////////////////////////////////////
    // Manual Data Entry
    ////////////////////////////////////////////////////////////////////
    public static boolean lrudFields() {
        return getHelperBoolean("pref_key_lrud_fields", false);
    }

    public static boolean degreesMinutesSeconds() {
        return getHelperBoolean("pref_key_deg_mins_secs", false);
    }


    ////////////////////////////////////////////////////////////////////
    // Developer
    ////////////////////////////////////////////////////////////////////
    public static boolean developerMode() {
        return getHelperBoolean("pref_key_developer_mode", false);
    }


    /**
     * Other keys
     */

    public static int textFontSize() {
        return getHelperInt("pref_survey_text_font_size", 32);
    }

    public static String activeSurveyName() {
        return getHelperString(SexyTopo.ACTIVE_SURVEY_NAME, "Error");
    }

    public static boolean isThereAnActiveSurvey() {
        return sharedPrefs.contains(SexyTopo.ACTIVE_SURVEY_NAME);
    }

    /**
     * This is used to set whether a survey will be reopened when opening SexyTopo
     */
    public static void updateRememberedSurvey(final String surveyName) {
        sharedPrefs.edit()
                .putString(SexyTopo.ACTIVE_SURVEY_NAME, surveyName)
                .apply();
    }
}

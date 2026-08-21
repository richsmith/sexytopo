package org.hwyl.sexytopo.control.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.survey.LicenceOption;
import org.hwyl.sexytopo.model.table.LRUD;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GeneralPreferences {

    private static SharedPreferences prefs;

    public static void initialise(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private static String getString(String key, String defaultValue) {

        if (prefs == null) { // e.g. during tests
            return defaultValue;
        }

        return prefs.getString(key, defaultValue);
    }

    private static boolean getBoolean(String key, Boolean defaultValue) {

        if (prefs == null) { // e.g. during tests
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

        if (prefs == null) { // e.g. during tests
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

        if (prefs == null) { // e.g. during tests
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

    public static boolean isManualControlsEnabled() {
        return getBoolean("pref_manual_controls", true);
    }

    public static boolean isManualLrudModeOn() {
        return getBoolean("pref_lrud_fields", false);
    }

    public static LRUD.Mode getLrudMode() {
        return LRUD.Mode.fromPreferenceValue(getString("pref_lrud_direction", "survey"));
    }

    public static boolean isDegMinsSecsModeOn() {
        return getBoolean("pref_deg_mins_secs", false);
    }

    public static boolean isIncDegMinsSecsModeOn() {
        return getBoolean("pref_inc_deg_mins_secs", false);
    }

    // ********** Surveying ***********

    public static String getLegAmalgamationAlgorithm() {
        return getString("pref_leg_amalgamation_algorithm", "angular");
    }

    public static float getMaxDistanceDelta() {
        return getFloat("pref_max_distance_delta", 0.05f);
    }

    public static float getMaxAngleDelta() {
        return getFloat("pref_max_angle_delta", 1.7f);
    }

    public static float getMaxEndpointDelta() {
        // Default to the BCRA Grade 5 cell size: all readings should fall within 0.1m of each other
        return getFloat("pref_max_endpoint_delta", 0.1f);
    }

    public static float getMaxPairwiseError() {
        return getFloat("pref_max_pairwise_error", 0.05f);
    }

    public static boolean isHotCornersModeActive() {
        return getBoolean("pref_hot_corners", true);
    }

    public static boolean isTwoFingerModeActive() {
        return getBoolean("pref_two_finger_movement", false);
    }

    public static boolean isLegacyCrossSectionsOn() {
        return getBoolean("pref_legacy_cross_sections", false);
    }

    // ********** Sketching ***********

    public static boolean isAntialiasingModeOn() {
        return getBoolean("pref_anti_alias", true);
    }

    public static float getSketchLineWidthDp() {
        return getFloat("pref_sketch_line_width", 1.5f);
    }

    public static float getStationCrossDiameterDp() {
        return getFloat("pref_station_diameter", 10f);
    }

    public static float getLegStrokeWidthDp() {
        return getFloat("pref_leg_width", 2.0f);
    }

    public static float getSplayStrokeWidthDp() {
        return getFloat("pref_splay_width", 1f);
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
        String colour = getString("pref_export_svg_background", "white");
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

    public static boolean isExportSvgLegendEnabled() {
        return getBoolean("pref_export_svg_legend", true);
    }

    public static boolean isExportSvgNorthArrowEnabled() {
        return getBoolean("pref_export_svg_north_arrow", true);
    }

    public static boolean isExportSvgScaleBarEnabled() {
        return getBoolean("pref_export_svg_scale_bar", true);
    }

    public static boolean isExportSvgTeamEnabled() {
        return getBoolean("pref_export_svg_team", true);
    }

    public static boolean isExportSvgCrossSectionsEnabled() {
        return getBoolean("pref_export_svg_cross_sections", true);
    }

    public static boolean isExportSvgSymbolsEnabled() {
        return getBoolean("pref_export_svg_symbols", true);
    }

    public static boolean isExportSvgCentrelineEnabled() {
        return getBoolean("pref_export_svg_centreline", true);
    }

    public static boolean isExportSvgStationsEnabled() {
        return getBoolean("pref_export_svg_stations", true);
    }

    public static boolean isExportSvgSplaysEnabled() {
        return getBoolean("pref_export_svg_splays", true);
    }

    public static boolean isExportSvgGridEnabled() {
        return getBoolean("pref_export_svg_grid", true);
    }

    public static boolean isExportSvgTaglineEnabled() {
        return getBoolean("pref_export_svg_tagline", true);
    }

    public static boolean isExportSvgCopyrightEnabled() {
        return getBoolean("pref_export_svg_copyright", true);
    }

    public static SharedPreferences getRawPreferences() {
        return prefs;
    }

    public static boolean isXviExportSymbolsEnabled() {
        return getBoolean("pref_therion_export_symbols", true);
    }

    public static boolean isXviExportTextEnabled() {
        return getBoolean("pref_therion_export_text", true);
    }

    public static String getTherionPlanSuffix() {
        return getString("pref_therion_plan_suffix", ".plan");
    }

    public static String getTherionEeSuffix() {
        return getString("pref_therion_ee_suffix", ".ee");
    }

    public static String getTherionXviFolder() {
        return getString("pref_therion_xvi_folder", "");
    }

    public static String getTherionPlanScrapSuffix() {
        return getString("pref_therion_plan_scrap_suffix", "-plan");
    }

    public static String getTherionEeScrapSuffix() {
        return getString("pref_therion_ee_scrap_suffix", "-ee");
    }

    public static boolean isTherionCrossSectionsEnabled() {
        return getBoolean("pref_therion_cross_sections", true);
    }

    public static String getTherionPlanXsSuffix() {
        return getString("pref_therion_plan_xs_suffix", "PX#");
    }

    public static String getTherionEeXsSuffix() {
        return getString("pref_therion_ee_xs_suffix", "EEX#");
    }

    public static String getExportFolderName() {
        return getString("pref_export_folder_name", "Exported");
    }

    public static boolean isExportTypeSubfoldersEnabled() {
        return getBoolean("pref_export_type_subfolders", true);
    }

    // ********** Known Cavers ***********

    private static final String PREF_KNOWN_CAVERS = "pref_known_cavers";

    public static List<String> getKnownCavers() {
        if (prefs == null) return new ArrayList<>();
        Set<String> set = prefs.getStringSet(PREF_KNOWN_CAVERS, new HashSet<>());
        List<String> list = new ArrayList<>(set);
        Collections.sort(list);
        return list;
    }

    public static void addKnownCaver(String name) {
        if (prefs == null || name == null || name.trim().isEmpty()) return;
        Set<String> set = new HashSet<>(prefs.getStringSet(PREF_KNOWN_CAVERS, new HashSet<>()));
        set.add(name.trim());
        prefs.edit().putStringSet(PREF_KNOWN_CAVERS, set).apply();
    }

    public static void removeKnownCaver(String name) {
        if (prefs == null) return;
        Set<String> set = new HashSet<>(prefs.getStringSet(PREF_KNOWN_CAVERS, new HashSet<>()));
        set.remove(name);
        prefs.edit().putStringSet(PREF_KNOWN_CAVERS, set).apply();
    }

    // ********** Licence Options ***********

    private static final String PREF_LICENCE_OPTIONS = "pref_licence_options";
    private static final String LICENCE_NAME_TAG = "name";
    private static final String LICENCE_DEFAULT_TAG = "default";

    /**
     * The name of the "no licence" option: a deliberate choice to leave the survey unlicensed, as
     * opposed to simply not having decided yet. It is stored as the empty string, so it needs no
     * special-casing on export - a trip with this licence behaves exactly like one with none.
     * TripActivity displays it under a friendlier label.
     */
    public static final String NO_LICENCE_NAME = "";

    private static List<LicenceOption> defaultLicenceOptions() {
        return new ArrayList<>(
                Arrays.asList(
                        new LicenceOption("GPLv3.0+", false),
                        new LicenceOption("CC0", false),
                        new LicenceOption("CC BY 4.0", false),
                        new LicenceOption("CC BY SA 4.0", false),
                        new LicenceOption("CC BY SA NC 4.0", false),
                        new LicenceOption("All rights reserved", false),
                        new LicenceOption(NO_LICENCE_NAME, false)));
    }

    public static List<LicenceOption> getLicenceOptions() {
        if (prefs == null) return defaultLicenceOptions();

        String json = prefs.getString(PREF_LICENCE_OPTIONS, null);
        if (json == null) return defaultLicenceOptions();

        try {
            return licenceOptionsFromJson(json);
        } catch (JSONException exception) {
            Log.e("Could not load licence options: " + exception);
            return defaultLicenceOptions();
        }
    }

    public static void setLicenceOptions(List<LicenceOption> options) {
        if (prefs == null) return;
        try {
            String json = licenceOptionsToJson(options).toString();
            prefs.edit().putString(PREF_LICENCE_OPTIONS, json).apply();
        } catch (JSONException exception) {
            Log.e("Could not save licence options: " + exception);
        }
    }

    /**
     * Returns the name of the licence option currently flagged as the default, or "" if none is -
     * which is the case until the user picks one, so that no licence is ever applied to a survey
     * without them having chosen it.
     */
    public static String getDefaultLicenceName() {
        for (LicenceOption option : getLicenceOptions()) {
            if (option.isDefault()) {
                return option.getName();
            }
        }
        return NO_LICENCE_NAME;
    }

    /**
     * Adds a new licence option. It is always added as a non-default option: a default is only ever
     * set by the user explicitly choosing one, never as a side effect of editing the list.
     */
    public static void addLicenceOption(String name) {
        if (name == null || name.trim().isEmpty()) return;
        setLicenceOptions(withAdded(getLicenceOptions(), name.trim()));
    }

    /**
     * Removes the licence option with the given name. If it was the default, the list is simply
     * left with no default rather than promoting a replacement - which would amount to picking a
     * licence on the user's behalf.
     */
    public static void removeLicenceOption(String name) {
        if (name == null) return;
        setLicenceOptions(withRemoved(getLicenceOptions(), name));
    }

    /** Renames a licence option, preserving its position in the list and its default flag. */
    public static void renameLicenceOption(String oldName, String newName) {
        if (oldName == null || newName == null || newName.trim().isEmpty()) return;
        setLicenceOptions(withRenamed(getLicenceOptions(), oldName, newName.trim()));
    }

    /**
     * Flags the licence option with the given name as the default, and un-flags every other option.
     * Does nothing if no option with that name exists.
     */
    public static void setDefaultLicenceOption(String name) {
        if (name == null) return;
        setLicenceOptions(withDefaultSet(getLicenceOptions(), name));
    }

    /**
     * Un-flags whatever option is currently the default, leaving the list with none - so new trips
     * start with a blank licence and the user is asked to pick one.
     */
    public static void clearDefaultLicenceOption() {
        setLicenceOptions(withDefaultCleared(getLicenceOptions()));
    }

    /**
     * Pure list transformation backing {@link #clearDefaultLicenceOption()}, kept separate so it
     * can be unit tested without a SharedPreferences-backed Context.
     */
    public static List<LicenceOption> withDefaultCleared(List<LicenceOption> options) {
        List<LicenceOption> result = new ArrayList<>();
        for (LicenceOption option : options) {
            result.add(option.withDefault(false));
        }
        return result;
    }

    /**
     * Pure list transformation backing {@link #addLicenceOption(String)}, kept separate so it can
     * be unit tested without a SharedPreferences-backed Context.
     */
    public static List<LicenceOption> withAdded(List<LicenceOption> options, String trimmedName) {
        List<LicenceOption> result = new ArrayList<>(options);
        result.add(new LicenceOption(trimmedName, false));
        return result;
    }

    /**
     * Pure list transformation backing {@link #removeLicenceOption(String)}, kept separate so it
     * can be unit tested without a SharedPreferences-backed Context.
     */
    public static List<LicenceOption> withRemoved(List<LicenceOption> options, String name) {
        List<LicenceOption> remaining = new ArrayList<>();
        for (LicenceOption option : options) {
            if (!option.getName().equals(name)) {
                remaining.add(option);
            }
        }
        return remaining;
    }

    /**
     * Pure list transformation backing {@link #renameLicenceOption(String, String)}, kept separate
     * so it can be unit tested without a SharedPreferences-backed Context.
     */
    public static List<LicenceOption> withRenamed(
            List<LicenceOption> options, String oldName, String trimmedNewName) {
        List<LicenceOption> result = new ArrayList<>(options);
        for (int i = 0; i < result.size(); i++) {
            if (result.get(i).getName().equals(oldName)) {
                result.set(i, result.get(i).withName(trimmedNewName));
                break;
            }
        }
        return result;
    }

    /**
     * Pure list transformation backing {@link #setDefaultLicenceOption(String)}, kept separate so
     * it can be unit tested without a SharedPreferences-backed Context. Does nothing (returns an
     * unchanged copy) if no option with the given name exists.
     */
    public static List<LicenceOption> withDefaultSet(List<LicenceOption> options, String name) {
        if (!hasOption(options, name)) {
            return new ArrayList<>(options);
        }

        List<LicenceOption> result = new ArrayList<>();
        for (LicenceOption option : options) {
            result.add(option.withDefault(option.getName().equals(name)));
        }
        return result;
    }

    private static boolean hasOption(List<LicenceOption> options, String name) {
        for (LicenceOption option : options) {
            if (option.getName().equals(name)) return true;
        }
        return false;
    }

    public static JSONArray licenceOptionsToJson(List<LicenceOption> options) throws JSONException {
        JSONArray array = new JSONArray();
        for (LicenceOption option : options) {
            JSONObject json = new JSONObject();
            json.put(LICENCE_NAME_TAG, option.getName());
            json.put(LICENCE_DEFAULT_TAG, option.isDefault());
            array.put(json);
        }
        return array;
    }

    public static List<LicenceOption> licenceOptionsFromJson(String jsonString)
            throws JSONException {
        JSONArray array = new JSONArray(jsonString);
        List<LicenceOption> options = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject json = array.getJSONObject(i);
            String name = json.getString(LICENCE_NAME_TAG);
            boolean isDefault = json.optBoolean(LICENCE_DEFAULT_TAG, false);
            options.add(new LicenceOption(name, isDefault));
        }
        return options;
    }
}

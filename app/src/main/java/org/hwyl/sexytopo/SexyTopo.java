package org.hwyl.sexytopo;

import android.content.Context;

import java.util.UUID;


public class SexyTopo {

    public static final String APP_NAME = "SexyTopo";

    public static final String TAG = APP_NAME; // for writing debug log

    public static final UUID DISTO_X_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final UUID SHETLAND_UUID = UUID.fromString("49535343-8841-43f4-a8d4-ecbe34729bb3");
    public static final int NUM_OF_REPEATS_FOR_NEW_STATION = 3;

    public static final String SURVEY_UPDATED_EVENT = "surveyUpdatedEvent";
    public static final String NEW_STATION_CREATED_EVENT = "newStationCreatedEvent";
    public static final String SYSTEM_LOG_UPDATED_EVENT = "systemLogUpdatedEvent";
    public static final String DEVICE_LOG_UPDATED_EVENT = "deviceLogUpdatedEvent";
    public static final String CALIBRATION_UPDATED_EVENT = "calibrationUpdatedEvent";

    public static final String SURVEY_DIR = "Surveys";
    public static final String IMPORT_DIR = "To Import";
    public static final String EXPORT_DIR = "Exported";
    public static final String LOG_DIR = "Logs";
    public static final String CALIBRATION_DIR = "Calibrations";

    public static final String IMPORT_SOURCE_DIR = "Import Source";

    public static final String ACTIVE_SURVEY_NAME = "activeSurveyName";

    public static final String BLANK_STATION_NAME = "-";

    public static final String GENERAL_PREFS = "generalPrefs";
    public static final String INPUT_MODE_PREFERENCE = "inputMode";

    public static final String DATA_EXTENSION = "data.json";
    public static final String PLAN_SKETCH_EXTENSION = "plan.json";
    public static final String EXT_ELEVATION_SKETCH_EXTENSION = "ext-elevation.json";
    public static final String METADATA_EXTENSION = "metadata.json";
    public static final String AUTOSAVE_EXTENSION = "autosave";

    public static final String PLAN_SUFFIX = "plan";
    public static final String EE_SUFFIX = "ee";

    public static final double ALLOWED_DOUBLE_DELTA = 0.0001;

    // This is extremely hacky but quite useful for getting a context when it's not sensible
    // to pass one around
    public static Context context;



}

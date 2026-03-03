package org.hwyl.sexytopo;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.util.UUID;


public class SexyTopoConstants {

    // ********** System **********

    public static final String APP_NAME = "SexyTopo";
    public static final String TAG = APP_NAME; // for writing debug log

    @SuppressLint("SimpleDateFormat")
    public static final SimpleDateFormat ISO_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    // ********** Event codes **********

    public static final String SURVEY_UPDATED_EVENT = "surveyUpdatedEvent";
    public static final String NEW_STATION_CREATED_EVENT = "newStationCreatedEvent";
    public static final String SYSTEM_LOG_UPDATED_EVENT = "systemLogUpdatedEvent";
    public static final String DEVICE_LOG_UPDATED_EVENT = "deviceLogUpdatedEvent";
    public static final String CALIBRATION_UPDATED_EVENT = "calibrationUpdatedEvent";

    // ********** Request codes **********
    public static final int REQUEST_CODE_SAVE_AS_SURVEY = 1000001;
    public static final int REQUEST_CODE_OPEN_SURVEY = 1000003;
    public static final int REQUEST_CODE_IMPORT_SURVEY_FILE = 1000004;
    public static final int REQUEST_CODE_IMPORT_SURVEY_DIRECTORY = 1000005;
    public static final int REQUEST_CODE_SELECT_SURVEY_TO_LINK = 1000006;
    public static final int REQUEST_CODE_DELETE_SURVEY_DIRECTORY = 1000007;

    public static final int REQUEST_CODE_OPEN_CALIBRATION = 1000010;
    public static final int REQUEST_CODE_SAVE_CALIBRATION = 1000011;


    // ********** Preference refs **********

    public static final String PREFERENCE_ACTIVE_SURVEY_URI = "activeSurveyUri";

    public static final String GENERAL_PREFS = "generalPrefs";
    public static final String INPUT_MODE_PREFERENCE = "inputMode";


    // ********** File handling **********

    public static final String DEFAULT_ROOT_DIR = "Documents";

    public static final String DATA_EXTENSION = "data.json";
    public static final String PLAN_SKETCH_EXTENSION = "plan.json";
    public static final String EXT_ELEVATION_SKETCH_EXTENSION = "ext-elevation.json";
    public static final String METADATA_EXTENSION = "metadata.json";
    public static final String AUTOSAVE_EXTENSION = "autosave";
    public static final String LOG_EXTENSION = "log";

    public static final String PLAN_SUFFIX = "plan";
    public static final String EE_SUFFIX = "ee";

    public static final String MIME_TYPE_DEFAULT = "text/plain";
    public static final String MIME_TYPE_JSON = "application/json";

    public static final int JSON_INDENT = 2;


    // ********** Testing **********

    public static final double ALLOWED_DOUBLE_DELTA = 0.0001;


    // ********** Hardware **********

    public static final UUID DISTO_X_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final UUID SHETLAND_UUID = UUID.fromString("49535343-8841-43f4-a8d4-ecbe34729bb3");


    // ********** Misc **********

    public static final int NUM_OF_REPEATS_FOR_NEW_STATION = 3;
    public static final String BLANK_STATION_NAME = "-";
    public static final String JUMP_TO_STATION = "jumpToStation";
    public static final String LINK_STATION = "linkStation";



}

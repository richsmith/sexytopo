package org.hwyl.sexytopo;

import android.os.Environment;

import java.util.UUID;

/**
 * Created by rls on 22/07/14.
 */
public class SexyTopo {

    public static final String APP_NAME = "SexyTopo";
    public static final String VERSION = "0.8";

    public static final String TAG = APP_NAME; // for writing debug log

    public static final UUID DISTO_X_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final int NUM_OF_REPEATS_FOR_NEW_STATION = 3;

    public static final String SURVEY_UPDATED_EVENT = "surveyUpdatedEvent";

    private static final String ROOT = Environment.getExternalStorageDirectory().toString();
    public static final String APP_PATH = ROOT + "/" + APP_NAME;
    public static final String SURVEY_PATH = APP_PATH + "/" + "Surveys";

    public static final String ACTIVE_SURVEY_NAME = "activeSurveyName";

    public static final String BLANK_STATION_NAME = "-";

}

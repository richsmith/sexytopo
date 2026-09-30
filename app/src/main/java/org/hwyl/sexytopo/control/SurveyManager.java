package org.hwyl.sexytopo.control;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.io.basic.Saver;
import org.hwyl.sexytopo.control.util.InputMode;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.calibration.CalibrationReading;
import org.hwyl.sexytopo.model.calibration.CalibrationReadingList;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Survey;

@SuppressWarnings("UnnecessaryLocalVariable")
public class SurveyManager {

    @SuppressLint("StaticFieldLeak") // should be OK - only ref to ApplicationContext kept
    private static SurveyManager instance;

    public static SurveyManager getInstance(Context context) {
        if (instance == null) {
            instance = new SurveyManager(context);
        }
        return instance;
    }

    private final Context context;

    private LocalBroadcastManager broadcastManager;

    // This should be created or loaded on startup
    private static Survey currentSurvey = new Survey();

    private final CalibrationReadingList calibrationReadings = new CalibrationReadingList();

    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public SurveyManager(Context Context) {
        this.context = Context;
    }

    public void updateSurvey(Leg leg) {
        updateSurvey(Collections.singletonList(leg));
    }

    /**
     * Add readings to the current survey. Instruments deliver readings on their own threads, but
     * all other survey edits (deleting stations, undo, etc.) happen on the main thread, so the
     * update is run there too; otherwise a reading arriving mid-edit can be attached to a station
     * that has just been removed from the survey.
     */
    public void updateSurvey(List<Leg> legs) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            applySurveyUpdate(legs);
        } else {
            List<Leg> copy = new ArrayList<>(legs);
            mainThreadHandler.post(() -> applySurveyUpdate(copy));
        }
    }

    private void applySurveyUpdate(List<Leg> legs) {

        if (!legs.isEmpty()) {

            InputMode inputMode = getInputMode();
            boolean stationAdded = SurveyUpdater.update(currentSurvey, legs, inputMode);

            // survey update event should be generated first so the survey can be synced before
            // dealing with any special station created events
            broadcastSurveyUpdated();

            if (stationAdded) {
                Log.i(R.string.survey_update_new_station_added);
                broadcastNewStationCreated();
            }

            autosave();
        }
    }

    public void autosave() {
        new AutosaveTask().execute(context);
    }

    public InputMode getInputMode() {
        SharedPreferences preferences =
                context.getSharedPreferences(
                        SexyTopoConstants.GENERAL_PREFS, android.content.Context.MODE_PRIVATE);
        String modeName =
                preferences.getString(
                        SexyTopoConstants.INPUT_MODE_PREFERENCE, InputMode.FORWARD.name());
        InputMode inputMode = InputMode.valueOf(modeName);
        return inputMode;
    }

    public void broadcastSurveyUpdated() {
        broadcast(new Intent(SexyTopoConstants.SURVEY_UPDATED_EVENT));
    }

    public void broadcastNewStationCreated() {
        broadcast(new Intent(SexyTopoConstants.NEW_STATION_CREATED_EVENT));
    }

    public void broadcastCalibrationUpdated() {
        broadcast(new Intent(SexyTopoConstants.CALIBRATION_UPDATED_EVENT));
    }

    private void broadcast(Intent intent) {
        if (broadcastManager == null) {
            broadcastManager = LocalBroadcastManager.getInstance(context);
        }
        broadcastManager.sendBroadcast(intent);
    }

    public static Survey getCurrentSurvey() {
        return currentSurvey;
    }

    public void setCurrentSurvey(Survey currentSurvey) {
        SurveyManager.currentSurvey = currentSurvey;
        broadcastSurveyUpdated();
    }

    public void addCalibrationReading(CalibrationReading calibrationReading) {
        calibrationReadings.add(calibrationReading);
        broadcastCalibrationUpdated();
    }

    public CalibrationReadingList getCalibrationReadings() {
        return calibrationReadings;
    }

    public void setCalibrationReadings(List<CalibrationReading> readings) {
        calibrationReadings.setAll(readings);
    }

    public void clearCalibrationReadings() {
        calibrationReadings.clear();
    }

    public void replaceCalibrationReading(int index) {
        calibrationReadings.replace(index);
    }

    public void deleteCalibrationReading(int index) {
        calibrationReadings.delete(index);
    }

    public void deleteLastCalibrationReading() {
        calibrationReadings.deleteLastAdded();
    }

    private static class AutosaveTask extends AsyncTask<Context, Void, Void> {

        @Override
        protected Void doInBackground(Context... contexts) {
            Context context = contexts[0];
            try {
                if (!currentSurvey.isAutosaved()) {
                    Saver.autosave(context, currentSurvey);
                    Log.d(R.string.file_save_autosaved);
                }
                return null;

            } catch (Exception e) {
                Log.e(R.string.file_save_autosave_error);
                Log.e(e);
                return null;
            }
        }
    }
}

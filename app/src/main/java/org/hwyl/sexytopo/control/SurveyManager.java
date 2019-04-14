package org.hwyl.sexytopo.control;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.io.basic.Saver;
import org.hwyl.sexytopo.control.util.InputMode;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.calibration.CalibrationReading;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SurveyManager {

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
    private static Survey currentSurvey = new Survey("Unset");

    private List<CalibrationReading> calibrationReadings =
            new ArrayList<CalibrationReading>();


    public SurveyManager(Context Context) {
        this.context = Context;
    }


    public void updateSurvey(Leg leg) {
        updateSurvey(Arrays.asList(new Leg[]{leg}));
    }


    public void updateSurvey(List<Leg> legs) {

        if (legs.size() > 0) {

            InputMode inputMode = getInputMode();
            boolean stationAdded = SurveyUpdater.update(currentSurvey, legs, inputMode);

            // survey update event should be generated first so the survey can be synced before
            // dealing with any special station created events
            broadcastSurveyUpdated();

            if (stationAdded) {
                Log.d("New station added");
                broadcastNewStationCreated();
            }

            new AutosaveTask().execute(context);
        }

    }


    public InputMode getInputMode() {
        SharedPreferences preferences = context.getSharedPreferences(
                SexyTopo.GENERAL_PREFS, android.content.Context.MODE_PRIVATE);
        String modeName = preferences.getString(
                SexyTopo.INPUT_MODE_PREFERENCE, InputMode.FORWARD.name());
        InputMode inputMode = InputMode.valueOf(modeName);
        return inputMode;
    }

    public void broadcastSurveyUpdated() {
        broadcast(new Intent(SexyTopo.SURVEY_UPDATED_EVENT));
    }

    public void broadcastNewStationCreated() {
        broadcast(new Intent(SexyTopo.NEW_STATION_CREATED_EVENT));
    }

    public void broadcastCalibrationUpdated() {
        broadcast(new Intent(SexyTopo.CALIBRATION_UPDATED_EVENT));
    }

    private void broadcast(Intent intent) {
        if (broadcastManager == null) {
            broadcastManager = LocalBroadcastManager.getInstance(context);
        }
        broadcastManager.sendBroadcast(intent);
    }

    public Survey getCurrentSurvey() {
        return currentSurvey;
    }

    public void setCurrentSurvey(Survey currentSurvey) {
        this.currentSurvey = currentSurvey;
        broadcastSurveyUpdated();
    }

    public void addCalibrationReading(CalibrationReading calibrationReading) {
        this.calibrationReadings.add(calibrationReading);
        broadcastCalibrationUpdated();
    }

    public List<CalibrationReading> getCalibrationReadings() {
        return calibrationReadings;
    }


    public void setCalibrationReadings(List<CalibrationReading> calibrationReadings) {
        this.calibrationReadings = calibrationReadings;
    }

    public void clearCalibrationReadings() {
        this.calibrationReadings.clear();
    }

    public void deleteLastCalibrationReading() {
        if (calibrationReadings.size() > 0) {
            calibrationReadings.remove(calibrationReadings.size() - 1);
        }
    }


    private static class AutosaveTask extends AsyncTask<Context, Void, Void> {

        @Override
        protected Void doInBackground(Context... contexts) {
            Context context = contexts[0];
            try {
                if (!currentSurvey.isAutosaved()) {
                    Log.d("Autosaving...");
                    Saver.autosave(context, currentSurvey);
                    currentSurvey.setAutosaved(true);
                    Log.d("Autosaved");
                }
            } catch (Exception e) {
                Log.e("Error autosaving survey");
                Log.e(e);
                Toast.makeText(context, "Error autosaving survey: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            } finally {
                return null;
            }
        }
    }

}

package org.hwyl.sexytopo.control.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.NewStationNotificationService;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.io.Util;
import org.hwyl.sexytopo.control.util.PreferenceHelper;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.File;


public class StartUpActivity extends SexyTopoActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hasStarted = true;

        setContentView(R.layout.activity_start_up);

        SexyTopo.context = this.getApplicationContext();

        Log.setContext(this.getApplicationContext());
        Log.load(Log.LogType.SYSTEM);
        Log.d("--------------------");
        Log.d("Starting up");

        Util.ensureDataDirectoriesExist(this);

        try {
            if (PreferenceHelper.isThereAnActiveSurvey()) {
                loadActiveSurvey();
            } else {
                createNewActiveSurvey();
            }
        } catch (Exception exception) {
            helpThereSeemsToBeNoSurveyDoSomething();
        }

        try {
            Intent serviceIntent = new Intent(this, NewStationNotificationService.class);
            startService(serviceIntent);
        } catch (Exception exception) {
            // Not mission-critical, so carry on if this doesn't work
            // (some users seem to have had problems according to the log)
            Log.e("Unable to start new station notification intent");
            Log.e(exception);
        }


        Intent intent = new Intent(this, DeviceActivity.class);
        startActivity(intent);
    }

    public void loadActiveSurvey() {

        String activeSurveyName = PreferenceHelper.activeSurveyName();
        Log.d("Active survey is <i>" + activeSurveyName + "</i>");

        if (!Util.doesSurveyExist(this, activeSurveyName)) {
            Log.e("Survey " + activeSurveyName + " does not exist");
            helpThereSeemsToBeNoSurveyDoSomething();

        } else if (isAutosaveNewerThanProperSave(this, activeSurveyName)) {
            restoreAutosave(activeSurveyName);
        } else {
            loadSurvey(activeSurveyName);
        }
    }


    private boolean isAutosaveNewerThanProperSave(Context context, String name) {

        String dataPath = Util.getPathForSurveyFile(context, name, SexyTopo.DATA_EXTENSION);
        File dataFile = new File(dataPath);

        String autosavePath = Util.getPathForSurveyFile(context, name,
        SexyTopo.DATA_EXTENSION + "." + SexyTopo.AUTOSAVE_EXTENSION);
        File autosaveFile = new File(autosavePath);


        if (!autosaveFile.exists()) {
            return false;
        } else {
            Log.d("Noticed an autosave file");
        }

        if (!dataFile.exists()) {
            Log.d("Data file seems to be missing!?");
            return true;

        } else if (autosaveFile.lastModified() > dataFile.lastModified()) {
            Log.d("Autosave file is newer than data file");
            return true;
        }

        return false;
    }


    private void helpThereSeemsToBeNoSurveyDoSomething() {
        Log.d("Can't find the survey; falling back to a new one");
        startNewSurvey();
        createNewActiveSurvey();
    }


    private void createNewActiveSurvey() {
        String defaultName = Util.getNextDefaultSurveyName(this);
        Survey survey = new Survey(defaultName);
        setSurvey(survey);
    }


}

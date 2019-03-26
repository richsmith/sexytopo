package org.hwyl.sexytopo.control.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.io.Util;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.File;


public class StartUpActivity extends SexyTopoActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_up);

        Log.setContext(this);
        Log.load(Log.LogType.SYSTEM);
        Log.d("--------------------");
        Log.d("Starting up");

        Util.ensureDataDirectoriesExist(this);

        try {
            if (isThereAnActiveSurvey()) {
                loadActiveSurvey();
            } else {
                createNewActiveSurvey();
            }
        } catch (Exception exception) {
            helpThereSeemsToBeNoSurveyDoSomething();
        }

        Intent intent = new Intent(this, DeviceActivity.class);
        startActivity(intent);
    }


    private boolean isThereAnActiveSurvey() {
        return getPreferences().contains(SexyTopo.ACTIVE_SURVEY_NAME);
    }


    public void loadActiveSurvey() {

        String activeSurveyName = getPreferences().getString(SexyTopo.ACTIVE_SURVEY_NAME, "Error");
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
        return;
    }


    private void createNewActiveSurvey() {
        String defaultName = Util.getNextDefaultSurveyName(this);
        Survey survey = new Survey(defaultName);
        setSurvey(survey);
    }


}

package org.hwyl.sexytopo.control.activity;

import android.content.Intent;
import android.os.Bundle;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.io.Util;
import org.hwyl.sexytopo.model.survey.Survey;


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

        if (isThereAnActiveSurvey()) {
            loadActiveSurvey();
        } else {
            createNewActiveSurvey();
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
            startNewSurvey();
            createNewActiveSurvey();
            return;
        }

        loadSurvey(activeSurveyName);
    }


    private void createNewActiveSurvey() {
        String defaultName = Util.getNextDefaultSurveyName(this);
        Survey survey = new Survey(defaultName);
        setSurvey(survey);
    }


}

package org.hwyl.sexytopo.control.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.io.Util;
import org.hwyl.sexytopo.control.io.basic.Loader;
import org.hwyl.sexytopo.model.survey.Survey;


public class StartUpActivity extends SexyTopoActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_up);

        Util.ensureDataDirectoriesExist(this);

        Survey survey = isThereAnActiveSurvey() ? loadActiveSurvey() : createNewActiveSurvey();
        SurveyManager.getInstance(this).setCurrentSurvey(survey);

        Log.setContext(this);

        Intent intent = new Intent(this, DeviceActivity.class);
        startActivity(intent);
    }


    private boolean isThereAnActiveSurvey() {
        return getPreferences().contains(SexyTopo.ACTIVE_SURVEY_NAME);
    }


    public Survey loadActiveSurvey() {

        String activeSurveyName = getPreferences().getString(SexyTopo.ACTIVE_SURVEY_NAME, "Error");

        if (!Util.doesSurveyExist(this, activeSurveyName)) {
            startNewSurvey();
            return createNewActiveSurvey();
        }

        Toast.makeText(getApplicationContext(),
                getString(R.string.loading_survey) + " " + activeSurveyName,
                Toast.LENGTH_SHORT).show();

        Survey survey;
        try {
            survey = Loader.loadSurvey(this, activeSurveyName);
        } catch (Exception e) {
            survey = createNewActiveSurvey();
            Toast.makeText(getApplicationContext(),
                    getString(R.string.loading_survey_error),
                    Toast.LENGTH_SHORT).show();
        }

        return survey;
    }


    private Survey createNewActiveSurvey() {
        String defaultName = Util.getNextDefaultSurveyName(this);
        Survey survey = new Survey(defaultName);
        return survey;
    }



}

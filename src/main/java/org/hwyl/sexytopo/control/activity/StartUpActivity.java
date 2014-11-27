package org.hwyl.sexytopo.control.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.io.Loader;
import org.hwyl.sexytopo.control.io.Util;
import org.hwyl.sexytopo.model.Survey;
import org.hwyl.sexytopo.control.SurveyManager;

public class StartUpActivity extends SexyTopoActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_up);

        Survey survey = isThereAnActiveSurvey()? loadActiveSurvey() : createNewActiveSurvey();
        SurveyManager.getInstance(this).setCurrentSurvey(survey);

        Intent intent = new Intent(this, DeviceActivity.class);
        startActivity(intent);
    }

    private boolean isThereAnActiveSurvey() {
        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        return preferences.contains(SexyTopo.ACTIVE_SURVEY_NAME);
    }


    public Survey loadActiveSurvey() {

        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        String activeSurveyName = preferences.getString(SexyTopo.ACTIVE_SURVEY_NAME, null);

        Toast.makeText(getApplicationContext(), "Loading Survey " + activeSurveyName,
                Toast.LENGTH_SHORT).show();



        Survey survey = Loader.loadSurvey(activeSurveyName);

        return survey;
    }

    private Survey createNewActiveSurvey() {

        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);

        String defaultNameBase = getBaseContext().getString(R.string.default_survey_name);
        String defaultName = Util.getNextDefaultSurveyName(defaultNameBase);

        Survey survey = new Survey(defaultName);

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(SexyTopo.ACTIVE_SURVEY_NAME, defaultName);
        editor.commit();

        return survey;
    }


}

package org.hwyl.sexytopo.control.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.util.SurveyStats;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.test.TestSurveyCreator;

public class SurveyActivity extends SexyTopoActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);

        Survey survey = SurveyManager.getInstance(this).getCurrentSurvey();
        TextView nameField = (TextView)(findViewById(R.id.survey_name));
        nameField.setText(survey.getName());

        View saveButton = findViewById(R.id.buttonSaveSurvey);
        saveButton.setOnClickListener(this);
        View generateButton = findViewById(R.id.buttonGenerateSurvey);
        generateButton.setOnClickListener(this);
    }

    @Override
    protected void onResume() {

        super.onResume();
        updateStats();



    }


    @Override
    public void onClick(View view) {

        switch (view.getId()) {

            case R.id.buttonGenerateSurvey:
                Survey currentSurvey = TestSurveyCreator.create(10, 5);
                SurveyManager.getInstance(this).setCurrentSurvey(currentSurvey);
                break;

        }
    }

    private void updateStats() {

        Survey survey = getSurvey();
        double longestLeg = SurveyStats.calcLongestLeg(survey);
        int numberOfStations = SurveyStats.calcNumberStations(survey);
        double length = SurveyStats.calcTotalLength(survey);
        double heightRange = SurveyStats.calcHeightRange(survey);
    }

}

package org.hwyl.sexytopo.control.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.util.SurveyStats;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.survey.Survey;

public class SurveyActivity extends SexyTopoActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);

        /*
        View saveButton = findViewById(R.id.buttonSaveSurvey);
        saveButton.setOnClickListener(this);
        */
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStats();
    }


    @Override
    public void onClick(View view) {
        /*
        switch (view.getId()) {
               case R.id.buttonSaveSurvey:
                try {
                    Saver.save(this, getSurvey());
                } catch (Exception e) {
                    showSimpleToast("Error saving survey");
                    Log.e("Error saving survey: " + e);
                }
        }*/
    }

    private void updateStats() {

        Survey survey = getSurvey();

        //TextView nameField = (TextView)(findViewById(R.id.survey_name));
        //nameField.setText(survey.getName());

        double length = SurveyStats.calcTotalLength(survey);
        setStatsField(R.id.statsFieldLength, TextTools.formatTo2dpWithComma(length));
        double heightRange = SurveyStats.calcHeightRange(survey);
        setStatsField(R.id.statsFieldDepth, TextTools.formatTo2dpWithComma(heightRange));
        int numberOfStations = SurveyStats.calcNumberStations(survey);
        setStatsField(R.id.statsFieldNumberStations, TextTools.formatWithComma(numberOfStations));
        double shortestLeg = SurveyStats.calcShortestLeg(survey);
        setStatsField(R.id.statsFieldShortestLeg, TextTools.formatTo2dpWithComma(shortestLeg));
        double longestLeg = SurveyStats.calcLongestLeg(survey);
        setStatsField(R.id.statsFieldLongestLeg, TextTools.formatTo2dpWithComma(longestLeg));

    }

    private void setStatsField(int id, String text) {
        TextView textView = (TextView)findViewById(id);
        textView.setText(text);
    }

}

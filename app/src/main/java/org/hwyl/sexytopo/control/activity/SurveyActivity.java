package org.hwyl.sexytopo.control.activity;

import android.os.Bundle;
import android.widget.TextView;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.util.SurveyStats;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.survey.Survey;

public class SurveyActivity extends SexyTopoActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStats();
    }


    private void updateStats() {

        Survey survey = getSurvey();

        double length = SurveyStats.calcTotalLength(survey);
        setStatsField(R.id.statsFieldLength, TextTools.formatTo2dpWithComma(length));
        double heightRange = SurveyStats.calcHeightRange(survey);
        setStatsField(R.id.statsFieldDepth, TextTools.formatTo2dpWithComma(heightRange));
        int numberOfStations = SurveyStats.calcNumberStations(survey);
        setStatsField(R.id.statsFieldNumberStations, TextTools.formatWithComma(numberOfStations));
        int numberOfLegs = SurveyStats.calcNumberSubFullLegs(survey.getOrigin());
        setStatsField(R.id.statsFieldNumberLegs, TextTools.formatWithComma(numberOfLegs));
        int numberOfSplays = SurveyStats.calcNumberSubSplays(survey.getOrigin());
        setStatsField(R.id.statsFieldNumberSplays, TextTools.formatWithComma(numberOfSplays));
        double shortestLeg = SurveyStats.calcShortestLeg(survey);
        setStatsField(R.id.statsFieldShortestLeg, TextTools.formatTo2dpWithComma(shortestLeg));
        double longestLeg = SurveyStats.calcLongestLeg(survey);
        setStatsField(R.id.statsFieldLongestLeg, TextTools.formatTo2dpWithComma(longestLeg));
    }

    private void setStatsField(int id, String text) {
        TextView textView = findViewById(id);
        textView.setText(text);
    }

}

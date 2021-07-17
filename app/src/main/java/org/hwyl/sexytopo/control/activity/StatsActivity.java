package org.hwyl.sexytopo.control.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TextView;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.util.SurveyStats;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.Set;


public class StatsActivity extends SexyTopoActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStats();
        updateLinkedStats();
    }


    private void updateStats() {

        Survey survey = getSurvey();

        float length = SurveyStats.calcTotalLength(survey);
        setStatsField(R.id.statsFieldLength, TextTools.formatTo2dpWithComma(length));
        float heightRange = SurveyStats.calcHeightRange(survey);
        setStatsField(R.id.statsFieldDepth, TextTools.formatTo2dpWithComma(heightRange));
        int numberOfStations = SurveyStats.calcNumberStations(survey);
        setStatsField(R.id.statsFieldNumberStations, TextTools.formatWithComma(numberOfStations));
        int numberOfLegs = SurveyStats.calcNumberSubFullLegs(survey.getOrigin());
        setStatsField(R.id.statsFieldNumberLegs, TextTools.formatWithComma(numberOfLegs));
        int numberOfSplays = SurveyStats.calcNumberSubSplays(survey.getOrigin());
        setStatsField(R.id.statsFieldNumberSplays, TextTools.formatWithComma(numberOfSplays));
        float shortestLeg = SurveyStats.calcShortestLeg(survey);
        setStatsField(R.id.statsFieldShortestLeg, TextTools.formatTo2dpWithComma(shortestLeg));
        float longestLeg = SurveyStats.calcLongestLeg(survey);
        setStatsField(R.id.statsFieldLongestLeg, TextTools.formatTo2dpWithComma(longestLeg));
    }


    private void updateLinkedStats() {

        Set<Survey> surveys = getSurvey().getRecursiveConnectedSurveys();
        surveys.add(getSurvey());

        int numberSurveys = surveys.size();

        TableLayout tableLayout = findViewById(R.id.statsAllLinkedSurveys);
        if (numberSurveys <= 1) {
            tableLayout.setVisibility(View.GONE);
        } else {
            tableLayout.setVisibility(View.VISIBLE);
        }

        float length = 0;
        float lowestHeight = Float.POSITIVE_INFINITY;
        float highestHeight = Float.NEGATIVE_INFINITY;
        int numberOfStations = 0;
        int numberOfLegs = 0;
        int numberOfSplays = 0;
        float shortestLeg = Float.POSITIVE_INFINITY;
        float longestLeg = 0;

        for (Survey survey : surveys) {
            length += SurveyStats.calcTotalLength(survey);
            float[] heightRange = SurveyStats.calcHeightRangeArray(survey);
            lowestHeight = Math.min(heightRange[0], lowestHeight);
            highestHeight = Math.max(heightRange[1], highestHeight);
            numberOfStations += SurveyStats.calcNumberStations(survey);
            numberOfLegs = SurveyStats.calcNumberSubFullLegs(survey.getOrigin());
            numberOfSplays += SurveyStats.calcNumberSubSplays(survey.getOrigin());
            longestLeg = Math.max(longestLeg, SurveyStats.calcLongestLeg(survey));
            shortestLeg = Math.min(shortestLeg, SurveyStats.calcShortestLeg(survey));
        }

        float heightRange = highestHeight - lowestHeight;
        if (Float.isInfinite(heightRange)) {
            heightRange = 0;
        }

        if (Float.isInfinite(shortestLeg)) {
            shortestLeg = 0;
        }

        setStatsField(R.id.statsFieldNumberLinkedSurveys, TextTools.formatWithComma(numberSurveys));
        setStatsField(R.id.statsFieldLinkedLength, TextTools.formatTo2dpWithComma(length));
        setStatsField(R.id.statsFieldLinkedDepth, TextTools.formatTo2dpWithComma(heightRange));
        setStatsField(R.id.statsFieldLinkedNumberStations, TextTools.formatWithComma(numberOfStations));
        setStatsField(R.id.statsFieldLinkedNumberLegs, TextTools.formatWithComma(numberOfLegs));
        setStatsField(R.id.statsFieldLinkedNumberSplays, TextTools.formatWithComma(numberOfSplays));
        setStatsField(R.id.statsFieldLinkedShortestLeg, TextTools.formatTo2dpWithComma(shortestLeg));
        setStatsField(R.id.statsFieldLinkedLongestLeg, TextTools.formatTo2dpWithComma(longestLeg));

    }


    private void setStatsField(int id, String text) {
        TextView textView = findViewById(id);
        textView.setText(text);
    }

}

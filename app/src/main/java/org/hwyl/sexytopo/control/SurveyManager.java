package org.hwyl.sexytopo.control;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.io.basic.Saver;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rls on 22/07/14.
 */
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

    private static Survey currentSurvey = new Survey("CHANGETHIS"); // FIXME, create elsewhere

    public SurveyManager(Context context) {
        this.context = context;
    }

    public void updateSurvey(List<Leg> legs) {

        /*SharedPreferences preferences =
                context.getSharedPreferences(Context.MODE_PRIVATE);*/

        if (legs.size() > 0) {

            SharedPreferences preferences =
                    context.getSharedPreferences(SexyTopo.GENERAL_PREFS, 0);
            boolean reverseLegs =
                    preferences.getBoolean(SexyTopo.REVERSE_MEASUREMENTS_PREFERENCE, false);
            if (reverseLegs) {
                legs = reverseLegs(legs);
            }

            boolean automaticBacksightPromotion =
                    preferences.getBoolean( "pref_key_highlight_latest_leg", false);

            SurveyUpdater.update(currentSurvey, legs, automaticBacksightPromotion);


            try {
                Saver.autosave(currentSurvey);
            } catch (Exception e) {
                Log.e(SexyTopo.TAG, "Error autosaving survey: " + e);
                Toast.makeText(context, "Error autosaving survey: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();

            }

        }

        broadcastSurveyUpdated();
    }

    private List<Leg> reverseLegs(List<Leg> legs) {
        List<Leg> reversed = new ArrayList<Leg>();
        for (Leg leg : legs) {
            reversed.add(leg.reverse());
        }
        return reversed;
    }

    public void broadcastSurveyUpdated() {

        Intent intent = new Intent(SexyTopo.SURVEY_UPDATED_EVENT);

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

}

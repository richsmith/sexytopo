package org.hwyl.sexytopo.control;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.io.Saver;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Survey;

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
        //broadcastManager =  LocalBroadcastManager.getInstance(context);
    }

    public void updateSurvey(List<Leg> legs) {

        if (legs.size() > 0) {
            SurveyUpdater.update(currentSurvey, legs);


            try {
                Saver.save(context, currentSurvey);
            } catch (Exception e) {
                Log.e(SexyTopo.TAG, "Error saving survey: " + e);
                Toast.makeText(context, "Error saving survey: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();

            }

        }

        broadcastSurveyUpdated();
    }

    public void broadcastSurveyUpdated() {

        Intent intent = new Intent(SexyTopo.SURVEY_UPDATED_EVENT);

        if (broadcastManager == null) {
            broadcastManager = LocalBroadcastManager.getInstance(context);
        }
        broadcastManager.sendBroadcast(intent);
    }

    public Survey getCurrentSurvey() {

        /*
        if (currentSurvey.getAllStations().size() <= 1) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (prefs.getBoolean("demo_mode", true)) {
                currentSurvey = TestSurveyCreator.create(10, 5);
            }
        }*/

        return currentSurvey;
    }

    public void setCurrentSurvey(Survey currentSurvey) {
        this.currentSurvey = currentSurvey;
        broadcastSurveyUpdated();
    }

}

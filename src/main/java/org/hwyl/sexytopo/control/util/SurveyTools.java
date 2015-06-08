package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

/**
 * Created by rls on 05/06/15.
 */
public class SurveyTools {

    public static void traverse(Survey survey, SurveyTraversalCallback callback) {
        traverse(survey.getOrigin(), callback);
    }

    public static boolean traverse(Station station, SurveyTraversalCallback callback) {

        for (Leg leg : station.getOnwardLegs()) {

            boolean isFinished = callback.call(station, leg);

            if (isFinished) {
                return isFinished;
            } else if (leg.hasDestination()) {
                return traverse(leg.getDestination(), callback);
            }
        }

        return false;
    }

    public interface SurveyTraversalCallback {
        public boolean call(Station origin, Leg leg);
    }
}

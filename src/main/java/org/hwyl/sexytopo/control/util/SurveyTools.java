package org.hwyl.sexytopo.control.util;

import com.google.common.base.Optional;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.Iterator;

/**
 * Created by rls on 05/06/15.
 */
public class SurveyTools {

    public static void traverse(Survey survey, SurveyTraversalCallback callback) {
        traverse(survey.getOrigin(), callback);
    }

    public static boolean traverseIterator(Station station, SurveyTraversalCallback callback) {

        // use an iterator because the callback might mutate the list of legs
        Iterator<Leg> i = station.getOnwardLegs().iterator();
        while (i.hasNext()) {
            Leg leg = i.next();
            boolean isFinished = callback.call(station, leg);

            if (isFinished) {
                return isFinished;
            } else if (leg.hasDestination()) {
                return traverse(leg.getDestination(), callback);
            }
        }

        return false;
    }

    public static boolean traverse(Station station, SurveyTraversalCallback callback) {

        for (Leg leg : station.getOnwardLegs()) {

            boolean isFinished = callback.call(station, leg);

            if (isFinished) {
                return true;
            } else if (leg.hasDestination()) {
                isFinished =  traverse(leg.getDestination(), callback);
                if (isFinished) {
                    return true;
                }
            }
        }

        return false;
    }

    public interface SurveyTraversalCallback {
        public boolean call(Station origin, Leg leg);
    }


}

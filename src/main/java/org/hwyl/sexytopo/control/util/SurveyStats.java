package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.Leg;
import org.hwyl.sexytopo.model.Survey;

import java.util.List;

/**
 * Created by rls on 04/12/14.
 */
public class SurveyStats {

    // This would all be so much easier with Java 8: lambdas, streaming API...
    // TODO: rewrite when Android moves to Java 8

    public static double calcTotalLength(Survey survey) {
        List<Leg> legs = survey.getAllLegs();

        double total = 0.0;
        for (Leg leg : legs) {
            total += leg.getDistance();
        }

        return total;
    }

    public static double calcLongestLeg(Survey survey) {
        List<Leg> legs = survey.getAllLegs();

        double max = 0.0;
        for (Leg leg : legs) {
            max = Math.max(leg.getDistance(), max);
        }

        return max;
    }

    public static int calcNumberStations(Survey survey) {
        return survey.getAllStations().size();
    }



}

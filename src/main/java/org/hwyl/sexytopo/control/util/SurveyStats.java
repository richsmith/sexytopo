package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

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

    public static int calcNumberSubStations(Station origin) {
        return Survey.getAllStations(origin).size() - 1;
    }

    public static int calcNumberLegs(Survey survey) {
        return survey.getAllLegs().size();
    }

    public static int calcNumberSubLegs(Station origin) {
        return Survey.getAllLegs(origin).size();
    }

    public static double calcHeightRange(Survey survey) {
        Space3DTransformer transformer = new Space3DTransformer();
        Space<Coord3D> space = transformer.transformTo3D(survey);

        double min = Double.MAX_VALUE, max = Double.MIN_VALUE;

        for (Coord3D point : space.getStationMap().values()) {
            max = Math.max(max, point.getZ());
            min = Math.min(min, point.getZ());
        }

        return max - min;
    }



}

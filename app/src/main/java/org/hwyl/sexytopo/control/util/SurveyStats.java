package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.List;
import java.util.Map;


public class SurveyStats {

    // This would all be so much easier with Java 8: lambdas, streaming API...
    // TODO: rewrite when Android moves to Java 8

    public static float calcTotalLength(Survey survey) {
        List<Leg> legs = survey.getAllLegs();

        float total = 0.0f;
        for (Leg leg : legs) {
            if (leg.hasDestination()) {
                total += leg.getDistance();
            }
        }

        return total;
    }

    public static float calcLongestLeg(Survey survey) {
        List<Leg> legs = survey.getAllLegs();
        if (legs.isEmpty()) {
            return 0;
        }

        float max = 0.0f;
        for (Leg leg : legs) {
            max = Math.max(leg.getDistance(), max);
        }

        return max;
    }

    public static float calcShortestLeg(Survey survey) {
        List<Leg> legs = survey.getAllLegs();
        if (legs.isEmpty()) {
            return 0;
        }

        float min = Float.POSITIVE_INFINITY;
        for (Leg leg : legs) {
            min = Math.min(leg.getDistance(), min);
        }

        return min;
    }

    public static int calcNumberStations(Survey survey) {
        return survey.getAllStations().size() - 1;
    }

    public static int calcNumberSubStations(Station origin) {
        return Survey.getAllStations(origin).size();
    }

    public static int calcNumberSubFullLegs(Station station) {
        List<Leg> legs = Survey.getAllLegs(station);
        int numFullLegs = 0;
        for (Leg leg : legs) {
            if (leg.hasDestination()) {
                numFullLegs++;
            }
        }
        return numFullLegs;
    }

    public static int calcNumberSubSplays(Station station) {
        List<Leg> legs = Survey.getAllLegs(station);
        int numSplays = 0;
        for (Leg leg : legs) {
            if (!leg.hasDestination()) {
                numSplays++;
            }
        }
        return numSplays;
    }

    public static int calcNumberSubLegs(Station origin) {
        return Survey.getAllLegs(origin).size();
    }

    public static float calcHeightRange(Survey survey) {
        float[] rangeArray = calcHeightRangeArray(survey);
        return rangeArray[1] - rangeArray[0];
    }


    public static float[] calcHeightRangeArray(Survey survey) {
        Space3DTransformer transformer = new Space3DTransformer();
        Space<Coord3D> space = transformer.transformTo3D(survey);
        Map<Station, Coord3D> stationsToCoords = space.getStationMap();

        if (stationsToCoords.size() <= 1) {
            return new float[]{0, 0};
        }

        float min = Float.MAX_VALUE, max = Float.MIN_VALUE;
        for (Coord3D point : space.getStationMap().values()) {
            max = Math.max(max, point.z);
            min = Math.min(min, point.z);
        }

        return new float[]{min, max};
    }


}

package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.Iterator;


public class SurveyTools {

    public static void traverseLegs(Survey survey, SurveyLegTraversalCallback callback) {
        traverseLegs(survey.getOrigin(), callback);
    }

    @SuppressWarnings("WhileLoopReplaceableByForEach")
    public static boolean traverseIterator(Station station, SurveyLegTraversalCallback callback) {

        // use an iterator because the callback might mutate the list of legs
        Iterator<Leg> i = station.getOnwardLegs().iterator();
        while (i.hasNext()) {
            Leg leg = i.next();
            boolean isFinished = callback.call(station, leg);

            if (isFinished) {
                return isFinished;
            } else if (leg.hasDestination()) {
                return traverseLegs(leg.getDestination(), callback);
            }
        }

        return false;
    }

    public static boolean traverseLegs(Station station, SurveyLegTraversalCallback callback) {

        for (Leg leg : station.getOnwardLegs()) {

            boolean isFinished = callback.call(station, leg);

            if (isFinished) {
                return true;
            } else if (leg.hasDestination()) {
                isFinished = traverseLegs(leg.getDestination(), callback);
                if (isFinished) {
                    return true;
                }
            }
        }

        return false;
    }

    public interface SurveyLegTraversalCallback {
        boolean call(Station origin, Leg leg);
    }

    public static void traverseStations(Survey survey, SurveyStationTraversalCallback callback) {
        traverseStations(survey.getOrigin(), callback);
    }

    public static boolean traverseStations(Station station, SurveyStationTraversalCallback callback) {

        boolean isFinished = callback.call(station);

        if (isFinished) {
            return true;
        }

        for (Leg leg : station.getConnectedOnwardLegs()) {
            isFinished = traverseStations(leg.getDestination(), callback);
            if (isFinished) {
                return true;
            }
        }

        return false;
    }

    public interface SurveyStationTraversalCallback {
        boolean call(Station station);
    }

    public static boolean isDescendantOf(Station potentialDescendant, Station potentialAncestor) {
        if (potentialDescendant == null || potentialAncestor == null) {
            return false;
        }

        return traverseStations(
            potentialAncestor,
            station -> {
                if (station.equals(potentialAncestor)) {
                    return false;
                }
                return station.equals(potentialDescendant);
            });
    }

}

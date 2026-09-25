package org.hwyl.sexytopo.control.util;

import java.util.Iterator;
import org.hwyl.sexytopo.model.graph.ExtendedElevationDirection;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

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

    public static boolean traverseStations(
            Station station, SurveyStationTraversalCallback callback) {

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

    public static boolean isInSubtree(Station root, Station station) {
        if (root == null || station == null) {
            return false;
        }
        return traverseStations(root, s -> s.equals(station));
    }

    /**
     * The direction the survey is heading in on the extended elevation once it leaves the given
     * station. This is the direction a newly-created station should inherit from it, and the
     * direction a cross-section at the station faces.
     *
     * <p>A direction that doesn't propagate applies to the leg into the station alone, so it says
     * nothing about where the survey goes next. In that case we walk up to the nearest ancestor
     * whose direction does propagate, so the survey resumes the direction it was heading in before.
     *
     * <p>NOTE: the walk up only happens for a station whose own direction doesn't propagate (a
     * VERTICAL leg), so the usual case is immediate. When it does happen it is a potentially
     * expensive O(n^2) operation (to keep doing survey traversals to find the parent with a
     * "standard" EE direction), but long series of VERTICAL legs ought to be very rare!
     */
    public static ExtendedElevationDirection getOnwardExtendedElevationDirection(
            Survey survey, Station station) {
        ExtendedElevationDirection direction = station.getExtendedElevationDirection();
        if (direction.propagates()) {
            return direction;
        }
        Leg referringLeg = survey.getReferringLeg(station);
        if (referringLeg == null) {
            // origin station — nothing above it to inherit from
            return ExtendedElevationDirection.DEFAULT;
        }
        Station parent = survey.getOriginatingStation(referringLeg);
        return getOnwardExtendedElevationDirection(survey, parent);
    }
}

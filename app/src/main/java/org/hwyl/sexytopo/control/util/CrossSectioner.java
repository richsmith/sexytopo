package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.graph.ExtendedElevationDirection;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

/**
 * Essentially works out the angle required for a cross-section using some heuristics and the best
 * info available.
 */
@SuppressWarnings("UnnecessaryLocalVariable")
public class CrossSectioner {

    public static CrossSection section(Survey survey, final Station station) {
        float angle = getAngleOfSection(survey, station);
        CrossSection crossSection = new CrossSection(station, angle);
        return crossSection;
    }

    /**
     * A vertical section faces the angle worked out from the survey. A horizontal one has no angle
     * to work out as it always lies flat.
     */
    public static CrossSection section(
            Survey survey, final Station station, CrossSection.Orientation orientation) {
        if (orientation == CrossSection.Orientation.HORIZONTAL) {
            return CrossSection.horizontal(station);
        }
        return section(survey, station);
    }

    public static float getAngleOfSection(Survey survey, Station station) {

        int numIncomingLegs = station == survey.getOrigin() ? 0 : 1;
        int numOutgoingLegs = station.getConnectedOnwardLegs().size();

        float angle;
        if (numIncomingLegs == 1 && numOutgoingLegs == 1) {
            float incomingAzimuth = getIncomingAzimuth(survey, station);
            float outgoingAzimuth = getOutgoingAzimuth(station);
            angle = Space2DUtils.averageAzimuths(incomingAzimuth, outgoingAzimuth);
        } else if (numIncomingLegs == 1) {
            // just consider the incoming leg (end of a passage or, lots of ways on)
            float incomingAzimuth = getIncomingAzimuth(survey, station);
            angle = incomingAzimuth;
        } else if (numOutgoingLegs == 1) {
            // just consider the outgoing leg (must be doing X-section at the origin)
            float outgoingAzimuth = getOutgoingAzimuth(station);
            angle = outgoingAzimuth;
        } else {
            // at the origin with no or lots of outgoing legs?? No idea....
            angle = 0;
        }

        return angle;
    }

    /**
     * Furthest horizontal-plane distance any splay reaches from the station. Returns 0 if the
     * station has no splays (or only purely vertical ones).
     */
    public static float getHorizontalRadius(Station station) {
        return (float)
                station.getUnconnectedOnwardLegs().stream()
                        .mapToDouble(
                                splay ->
                                        splay.getDistance()
                                                * Math.cos(Math.toRadians(splay.getInclination())))
                        .max()
                        .orElse(0);
    }

    /**
     * Whether a vertical section at the given bearing faces right on the elevation. The elevation
     * has no bearings, so this compares the bearing with the way the survey heads on from the
     * station: within 90 degrees of it the section faces along the survey, whichever way the survey
     * is drawn there, and otherwise back against it.
     */
    public static boolean isFacingRightOnElevation(Survey survey, Station station, float angle) {
        float forward = getAngleOfSection(survey, station);
        boolean facesForward = getAngleDifference(angle, forward) <= 90;
        return facesForward == isSurveyDrawnRight(survey, station);
    }

    /**
     * The bearing for a vertical section that faces right, or left, on the elevation: the way the
     * survey heads on from the station, turned round if the survey is drawn the other way there.
     */
    public static float getAngleFacingOnElevation(
            Survey survey, Station station, boolean facingRight) {
        float forward = getAngleOfSection(survey, station);
        return facingRight == isSurveyDrawnRight(survey, station)
                ? forward
                : Space2DUtils.adjustAngle(forward, 180);
    }

    private static boolean isSurveyDrawnRight(Survey survey, Station station) {
        return SurveyTraversal.getOnwardExtendedElevationDirection(survey, station)
                != ExtendedElevationDirection.LEFT;
    }

    private static float getAngleDifference(float a, float b) {
        float difference = Math.abs(a - b) % 360;
        return difference > 180 ? 360 - difference : difference;
    }

    private static float getIncomingAzimuth(Survey survey, Station station) {
        try {
            return survey.getReferringLeg(station).getAzimuth();
        } catch (NullPointerException exception) {
            return 0; // not sure how this can happen, but has been reported
        }
    }

    private static float getOutgoingAzimuth(Station station) {
        return station.getConnectedOnwardLegs().get(0).getAzimuth();
    }
}

package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

/**
 * Essentially works out the angle required for a cross-section using some heuristics and the best
 * info available.
 */
public class CrossSectioner {


    public static CrossSection section(Survey survey, final Station station) {
        double angle = getAngleOfSection(survey, station);
        CrossSection crossSection = new CrossSection(station, angle);
        return crossSection;
    }

    public static double getAngleOfSection(Survey survey, Station station) {

        int numIncomingLegs = station == survey.getOrigin()? 0 : 1;
        int numOutgoingLegs = station.getConnectedOnwardLegs().size();

        double angle;
        if (numIncomingLegs == 1 && numOutgoingLegs == 1) {
            double incomingBearing = getIncomingBearing(survey, station);
            double outgoingBearing = getOutgoingBearing(station);
            angle = (Space2DUtils.adjustAngle((incomingBearing + 180.0), outgoingBearing) / 2);
        } else if (numIncomingLegs == 1) {
            // just consider the incoming leg (end of a passage or, lots of ways on)
            double incomingBearing = getIncomingBearing(survey, station);
            angle = Space2DUtils.adjustAngle(incomingBearing, 90);
        } else if (numOutgoingLegs == 1) {
            // just consider the outgoing leg (must be doing X-section at the origin)
            double outgoingBearing = getOutgoingBearing(station);
            angle = Space2DUtils.adjustAngle(outgoingBearing, 90);
        } else {
            // at the origin with no or lots of outgoing legs?? No idea....
            angle = 0;
        }

        return angle;
    }

    private static double getIncomingBearing(Survey survey, Station station) {
        return survey.getReferringLeg(station).getBearing();
    }

    private static double getOutgoingBearing(Station station) {
        return station.getConnectedOnwardLegs().get(0).getBearing();
    }

}

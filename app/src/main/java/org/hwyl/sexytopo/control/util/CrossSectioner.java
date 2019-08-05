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
            double incomingAzimuth = getIncomingAzimuth(survey, station);
            double outgoingAzimuth = getOutgoingAzimuth(station);
            angle = (incomingAzimuth + outgoingAzimuth) / 2;
        } else if (numIncomingLegs == 1) {
            // just consider the incoming leg (end of a passage or, lots of ways on)
            double incomingAzimuth = getIncomingAzimuth(survey, station);
            angle = incomingAzimuth;
        } else if (numOutgoingLegs == 1) {
            // just consider the outgoing leg (must be doing X-section at the origin)
            double outgoingAzimuth = getOutgoingAzimuth(station);
            angle = outgoingAzimuth;
        } else {
            // at the origin with no or lots of outgoing legs?? No idea....
            angle = 0;
        }

        return angle;
    }

    private static double getIncomingAzimuth(Survey survey, Station station) {
        try {
            return survey.getReferringLeg(station).getAzimuth();
        } catch (NullPointerException exception) {
            return 0; // not sure how this can happen, but has been reported
        }
    }

    private static double getOutgoingAzimuth(Station station) {
        return station.getConnectedOnwardLegs().get(0).getAzimuth();
    }

}

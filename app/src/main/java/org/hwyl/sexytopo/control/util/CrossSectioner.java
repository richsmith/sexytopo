package org.hwyl.sexytopo.control.util;

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

    public static float getAngleOfSection(Survey survey, Station station) {

        int numIncomingLegs = station == survey.getOrigin()? 0 : 1;
        int numOutgoingLegs = station.getConnectedOnwardLegs().size();

        float angle;
        if (numIncomingLegs == 1 && numOutgoingLegs == 1) {
            float incomingAzimuth = getIncomingAzimuth(survey, station);
            float outgoingAzimuth = getOutgoingAzimuth(station);
            angle = averageTwoAzimuths(incomingAzimuth, outgoingAzimuth);
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

    /** Average two azimuth values, handling the 360/0 boundary correctly */
    private static float averageTwoAzimuths(float azimuth1, float azimuth2) {
        float min = Math.min(azimuth1, azimuth2);
        float max = Math.max(azimuth1, azimuth2);
        if (max - min > 180) {
            // Spans zero boundary - shift values below 180 up by 360
            if (azimuth1 < 180) azimuth1 += 360;
            if (azimuth2 < 180) azimuth2 += 360;
        }
        return ((azimuth1 + azimuth2) / 2) % 360;
    }

}

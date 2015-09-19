package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

/**
 * Created by rls on 15/09/15.
 */
public class CrossSectioner {


    public static CrossSection section(Survey survey, final Station station) {

        int numIncomingLegs = station == survey.getOrigin()? 0 : 1;
        int numOutgoinggLegs = station.getConnectedOnwardLegs().size();

        double angle;
        if (numIncomingLegs == 1 && numOutgoinggLegs == 1) {
            double incomingBearing = getIncomingBearing(survey, station);
            double outgoingBearing = getOutgoingBearing(station);
            angle = ((outgoingBearing - incomingBearing) / 2) % 360;
        } else if (numIncomingLegs == 1) {
            // just consider the incoming leg (end of a passage or, lots of ways on)
            double incomingBearing = getIncomingBearing(survey, station);
            angle = Space2DUtils.adjustAngle(incomingBearing, 90);
        } else if (numOutgoinggLegs == 1) {
            // just consider the outgoing leg (must be doing X-section at the origin)
            double outgoingBearing = getOutgoingBearing(station);
            angle = Space2DUtils.adjustAngle(outgoingBearing, 90);
        } else {
            // at the origin with no or lots of outgoing legs?? No idea....
            angle = 0;
        }

        CrossSection crossSection = new CrossSection(station, angle);
        return crossSection;
    }

    private static double getIncomingBearing(Survey survey, Station station) {
        return survey.getReferringLeg(station).getBearing();
    }

    private static double getOutgoingBearing(Station station) {
        return station.getConnectedOnwardLegs().get(0).getBearing();
    }

}

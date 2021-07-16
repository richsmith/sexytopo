package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;


public class StationRotator {

    public static Station rotate(Station station, double angle) {
        Station rotated = new Station(station.getName());

        // this would be a lot cleaner with the Java 8 streaming API :'(
        for (Leg leg : station.getOnwardLegs()) {
            if (leg.hasDestination()) {
                rotated.addOnwardLeg(leg.rotate(angle));
            }
        }

        return rotated;
    }
}

package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;

/**
 * Created by rls on 18/02/15.
 */
public class GraphCopier {

    public Station copy(Station station) {

        Station stationCopy = new Station(station.getName());

        for (Leg leg : station.getOnwardLegs()) {
            Leg legCopy;
            if (leg.hasDestination()) {
                Station destinationCopy = copy(leg.getDestination());
                legCopy = new Leg(leg.getDistance(), leg.getBearing(), leg.getInclination(),
                        destinationCopy);
            } else {
                legCopy = new Leg(leg.getDistance(), leg.getBearing(), leg.getInclination());
            }

            stationCopy.addOnwardLeg(legCopy);

        }

        return stationCopy;
    }
}

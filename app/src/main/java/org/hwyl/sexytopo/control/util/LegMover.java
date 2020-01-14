package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.List;


public class LegMover {

    public static List<Station> getValidDestinations(Survey survey, Leg leg) {

        List<Station> stations = survey.getAllStations();

        Station originatingStation = survey.getOriginatingStation(leg);
        stations.remove(originatingStation);

        removeDownstreamStations(leg, stations);
        return stations;
    }

    private static void removeDownstreamStations(Leg leg, List<Station> stations) {

        if (!leg.hasDestination()) {
            return;
        }

        Station station = leg.getDestination();
        stations.remove(station);

        for (Leg onwardLeg : station.getConnectedOnwardLegs()) {
            removeDownstreamStations(onwardLeg, stations);
        }

    }

}

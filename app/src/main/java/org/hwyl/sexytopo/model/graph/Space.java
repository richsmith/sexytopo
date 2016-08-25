package org.hwyl.sexytopo.model.graph;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by rls on 26/07/14.
 */
public class Space<T extends Coord> {
    private Map<Station, T> stations = new HashMap<>();
    private Map<Leg, Line<T>> legs = new HashMap<>();

    public Map<Station, T> getStationMap() {
        return stations;
    }

    public Map<Leg, Line<T>> getLegMap() {
        return legs;
    }

    public void addStation(Station station, T coord) {
        stations.put(station, coord);
    }

    public void addLeg(Leg leg, Line<T> line) {
        legs.put(leg, line);
    }

}

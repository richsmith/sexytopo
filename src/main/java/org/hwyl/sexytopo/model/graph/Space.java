package org.hwyl.sexytopo.model.graph;

import org.hwyl.sexytopo.model.Leg;
import org.hwyl.sexytopo.model.Station;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /*public Set<T> getAllCoords() {

        Set<T> all = new HashSet<>();

        for (T station : stations) {
            all.add(station);
        }

        for (Line<T> leg : legs) {
            all.add(leg.getStart());
            all.add(leg.getEnd());
        }

        return all;
    }*/

}

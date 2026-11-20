package org.hwyl.sexytopo.model.geometry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.LegFilter;
import org.hwyl.sexytopo.model.survey.Station;

public class Space<T extends Coord<T>> {
    private final Map<Station, T> stations = new HashMap<>();
    private final Map<Leg, Line<T>> legs = new HashMap<>();

    public Map<Station, T> getStationMap() {
        return stations;
    }

    public Map<Leg, Line<T>> getLegMap() {
        return legs;
    }

    /** The lines of the legs that match the filter, e.g. LegFilter.LEGS. This is a new list. */
    public List<Line<T>> getLegLines(LegFilter filter) {
        return legs.entrySet().stream()
                .filter(entry -> filter.test(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    public void addStation(Station station, T coord) {
        stations.put(station, coord);
    }

    public void addLeg(Leg leg, Line<T> line) {
        legs.put(leg, line);
    }

    public Space<T> scale(float scale) {

        Space<T> scaled = new Space<>();

        for (Map.Entry<Station, T> entry : stations.entrySet()) {
            scaled.addStation(entry.getKey(), entry.getValue().scale(scale));
        }

        for (Map.Entry<Leg, Line<T>> entry : legs.entrySet()) {
            scaled.addLeg(entry.getKey(), entry.getValue().scale(scale));
        }

        return scaled;
    }
}

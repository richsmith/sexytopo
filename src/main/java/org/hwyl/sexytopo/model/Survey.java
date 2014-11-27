package org.hwyl.sexytopo.model;

import org.hwyl.sexytopo.control.util.StationNamer;
import org.hwyl.sexytopo.model.sketch.Sketch;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by rls on 16/07/14.
 */
public class Survey {

    public static final Station NULL_STATION = new Station("-");

    private String name;

    private Sketch planSketch = new Sketch();
    private Sketch elevationSketch = new Sketch();

    public Sketch getPlanSketch() {
        return planSketch;
    }

    public Sketch getElevationSketch() {
        return elevationSketch;
    }

    private final Station origin = new Station(StationNamer.generateOriginName());
    private Station activeStation = origin;

    public Survey(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setActiveStation(Station activeStation) {
        this.activeStation = activeStation;
    }

    public Station getActiveStation() {
        return activeStation;
    }

    public Station getOrigin() {
        return origin;
    }

    public List<Station> getAllStations() {
        return getConnectedStations(origin);
    }

    private List<Station> getConnectedStations(Station root) {

        List<Station> stations = new ArrayList<>();
        stations.add(root);

        for (Leg leg : root.getConnectedOnwardLegs()) {
            Station destination = leg.getDestination();
            stations.addAll(getConnectedStations(destination));
        }

        return stations;
    }

    public void sanityCheckNoDuplicateNames() {
        List<Station> stations = getAllStations();
        Set<String> names = new HashSet<>();
        for (Station station : stations) {
            String name = station.getName();
            if (names.contains(name)) {
                throw new IllegalStateException("Duplicated station name " + name);
            } else {
                names.add(station.getName());
            }
        }

    }
}

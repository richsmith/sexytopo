package org.hwyl.sexytopo.model.survey;

import org.hwyl.sexytopo.control.util.StationNamer;
import org.hwyl.sexytopo.model.sketch.Sketch;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Created by rls on 16/07/14.
 */
public class Survey {

    public static final Station NULL_STATION = new Station("-");

    private String name;

    private Sketch planSketch = new Sketch();
    private Sketch elevationSketch = new Sketch();


    private final Station origin = new Station(StationNamer.generateOriginName());
    private Station activeStation = origin;


    private Stack<UndoEntry> undoStack = new Stack<>();

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
        if (activeStation == null) {
            return origin;
        } else {
            return activeStation;
        }
    }

    public void setPlanSketch(Sketch planSketch) {
        this.planSketch = planSketch;
    }

    public Sketch getPlanSketch() {
        return planSketch;
    }

    public void setElevationSketch(Sketch elevationSketch) {
        this.elevationSketch = elevationSketch;
    }

    public Sketch getElevationSketch() {
        return elevationSketch;
    }

    public Station getOrigin() {
        return origin;
    }

    public List<Station> getAllStations() {
        return getConnectedStations(origin);
    }

    public List<Leg> getAllLegs() {
        return getConnectedLegs(origin);
    }

    public List<Leg> getConnectedLegs(Station root) {
        List<Leg> legs = new ArrayList<>();
        legs.addAll(root.getOnwardLegs());
        for (Leg leg : root.getConnectedOnwardLegs()) {
            legs.addAll(getConnectedLegs(leg.getDestination()));
        }
        return legs;
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


    private void fixActiveStation() {
        activeStation = origin;
    }


    public void addUndoEntry(Station station, Leg leg) {
        undoStack.push(new UndoEntry(station, leg));
    }


    public void undoLeg() {
        if (!undoStack.isEmpty()) {
            UndoEntry entry = undoStack.pop();
            entry.station.getOnwardLegs().remove(entry.leg);
            if (entry.leg.hasDestination() && entry.leg.getDestination() == activeStation) {
                fixActiveStation();
            }
        }
    }



    private class UndoEntry {
        private Station station;
        private Leg leg;
        private UndoEntry(Station station, Leg leg) {
            this.station = station;
            this.leg = leg;
        }
    }


}

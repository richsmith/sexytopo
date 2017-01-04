package org.hwyl.sexytopo.model.survey;

import org.hwyl.sexytopo.control.util.StationNamer;
import org.hwyl.sexytopo.control.util.SurveyTools;
import org.hwyl.sexytopo.control.util.Wrapper;
import org.hwyl.sexytopo.model.sketch.Sketch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Created by rls on 16/07/14.
 */
public class Survey {

    public static final Station NULL_STATION = new Station("-");

    private String name;

    private Sketch planSketch = new Sketch();
    private Sketch elevationSketch = new Sketch();


    private Station origin = new Station(StationNamer.generateOriginName());
    private Station activeStation = origin;

    private Map<Station, Survey> stationsToSurveyConnections = new HashMap<>();

    private boolean isSaved = true;

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
        return activeStation;
    }

    public void setSaved(boolean isSaved) {
        this.isSaved = isSaved;
        planSketch.setSaved(isSaved);
        elevationSketch.setSaved(isSaved);

    }

    public boolean isSaved() {
        return isSaved && planSketch.isSaved() && elevationSketch.isSaved();
    }

    public Leg getMostRecentLeg() {
        return undoStack.empty()? null : undoStack.peek().leg;
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

    public void setOrigin(Station origin) {
        this.origin = origin;
    }

    public List<Station> getAllStations() {
        return getAllStations(origin);
    }

    public List<Leg> getAllLegs() {
        return getAllLegs(origin);
    }

    public static List<Leg> getAllLegs(Station root) {
        List<Leg> legs = new ArrayList<>();
        legs.addAll(root.getOnwardLegs());
        for (Leg leg : root.getConnectedOnwardLegs()) {
            legs.addAll(getAllLegs(leg.getDestination()));
        }
        return legs;
    }

    public static List<Station> getAllStations(Station root) {

        List<Station> stations = new ArrayList<>();
        stations.add(root);

        for (Leg leg : root.getConnectedOnwardLegs()) {
            Station destination = leg.getDestination();
            stations.addAll(getAllStations(destination));
        }

        return stations;
    }


    public void checkActiveStation() {
        List<Station> stations = getAllStations();
        if (!stations.contains(activeStation)) {
            activeStation = findNewActiveStation();
        }
    }

    private Station findNewActiveStation() {
        if (!undoStack.isEmpty()) {
            return undoStack.peek().station;
        } else {
            return origin;
        }
    }


    public void addUndoEntry(Station station, Leg leg) {
        undoStack.push(new UndoEntry(station, leg));
    }


    public void deleteStation(final Station toDelete) {
        if (toDelete == getOrigin()) {
            return;
        }

        SurveyTools.traverseLegs(this, new SurveyTools.SurveyLegTraversalCallback() {
            @Override
            public boolean call(Station origin, Leg leg) {
                if (leg.hasDestination() && leg.getDestination() == toDelete) {
                    origin.getOnwardLegs().remove(leg);
                    checkActiveStation();
                    return true;
                } else {
                    return false;
                }
            }
        });

        setSaved(false);
    }

    public Leg getReferringLeg(final Station station) {

        if (station == getOrigin()) {
            return null;
        }

        final Wrapper wrapper = new Wrapper();
        SurveyTools.traverseLegs(
                this,
                new SurveyTools.SurveyLegTraversalCallback() {
                    @Override
                    public boolean call(Station origin, Leg leg) {
                        if (leg.getDestination() == station) {
                            wrapper.value = leg;
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
        return (Leg)(wrapper.value);
    }


    public void undoLeg(final Leg toDelete) {
        SurveyTools.traverseLegs(
                this,
                new SurveyTools.SurveyLegTraversalCallback() {
                    @Override
                    public boolean call(Station origin, Leg leg) {
                        if (leg == toDelete) {
                            origin.getOnwardLegs().remove(toDelete);
                            return true;
                        } else {
                            return false;
                        }
                    }
                });

        List<Station> stations = getAllStations();
        List<Leg> legs = getAllLegs();
        List<UndoEntry> invalidEntries = new ArrayList<>();
        for (UndoEntry entry : undoStack) {
            if (!stations.contains(entry.station) || !legs.contains(entry.leg)) {
                invalidEntries.add(entry);
            }
        }
        undoStack.removeAll(invalidEntries);

        checkActiveStation();
    }


    public void undoLeg() {
        // FIXME; can we consolidate the two undoLeg methods?
        if (!undoStack.isEmpty()) {
            UndoEntry entry = undoStack.pop();
            entry.station.getOnwardLegs().remove(entry.leg);
            if (entry.leg.hasDestination() && entry.leg.getDestination() == activeStation) {
                checkActiveStation();
            }
        }
        setSaved(false);
    }

    public Station getStationByName(final String name) {
        final Wrapper wrapper = new Wrapper();
        SurveyTools.traverseStations(
                this,
                new SurveyTools.SurveyStationTraversalCallback() {
                    @Override
                    public boolean call(Station station) {
                        if (station.getName().equals(name)) {
                            wrapper.value = station;
                            return true;
                        } else {
                            return false;
                        }
                    }
                });

        return (Station)(wrapper.value);
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

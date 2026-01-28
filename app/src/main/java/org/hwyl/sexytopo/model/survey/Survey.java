package org.hwyl.sexytopo.model.survey;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import org.hwyl.sexytopo.control.util.StationNamer;
import org.hwyl.sexytopo.control.util.SurveyTools;
import org.hwyl.sexytopo.control.util.Wrapper;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.sketch.Sketch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


public class Survey {

    public static final Station NULL_STATION = new Station("-");
    public static final String DEFAULT_NAME = "Unsaved Survey";

    public static final char[] FORBIDDEN_CHARS = new char[]{':', '.', '\n', '\r', '/', '\\'};

    private String name;

    private Station origin = new Station(StationNamer.generateOriginName());
    private Station activeStation = origin;

    private Sketch planSketch = new Sketch();
    private Sketch elevationSketch = new Sketch();

    private Trip trip = null;

    private final Map<Station, Set<SurveyConnection>> stationsToSurveyConnections = new HashMap<>();

    private boolean isSaved = true;
    private boolean isAutosaved = true;
    private DocumentFile directory;

    private final Stack<Leg> legsInChronoOrder = new Stack<>();

    public Survey() {
        this.setName(DEFAULT_NAME);
    }

    public String getName() {
        return name;
    }

    private void setName(String name) {
        for (char c : FORBIDDEN_CHARS) {
            name = name.replace(Character.toString(c), "");
        }
        if (name.equals("")) {
            name = "blank";
        }
        this.name = name;
    }

    public void setDirectory(DocumentFile directory) {
        this.directory = directory;
        this.setName(directory.getName());
    }

    public DocumentFile getDirectory() {
        return this.directory;
    }

    public boolean hasHome() {
        return getDirectory() != null;
    }

    public Uri getUri() {
        return directory == null? null : getDirectory().getUri();
    }

    public void setActiveStation(Station activeStation) {
        this.activeStation = activeStation;
    }

    public Station getActiveStation() {

        if (activeStation == null) { // this shouldn't happen, but just in case...
            setActiveStation(origin);
        }

        return activeStation;
    }

    public void setSaved(boolean isSaved) {
        this.isSaved = isSaved;
        planSketch.setSaved(isSaved);
        elevationSketch.setSaved(isSaved);

        if (!isSaved) {
            // saving doesn't mean it's autosaved, but unsaved data means autosave required
            setAutosaved(false);
        }
    }

    public boolean isSaved() {
        return isSaved && planSketch.isSaved() && elevationSketch.isSaved();
    }

    public void setAutosaved(boolean isAutosaved) {
        this.isAutosaved = isAutosaved;
    }

    public boolean isAutosaved() {
        return isAutosaved;
    }

    public Leg getMostRecentLeg() {
        return legsInChronoOrder.empty()? null : legsInChronoOrder.peek();
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

    public Sketch getSketch(Projection2D projection) {
        if (projection == Projection2D.PLAN) {
            return getPlanSketch();
        } else if (projection == Projection2D.EXTENDED_ELEVATION) {
            return getElevationSketch();
        } else {
            throw new IllegalArgumentException("Unknown projection: " + projection);
        }
    }


    public Station getOrigin() {
        return origin;
    }

    public void setOrigin(Station origin) {
        this.origin = origin;
    }

    public boolean isOrigin(Station station) {
        return station == origin;
    }

    public List<Station> getAllStations() {
        return getAllStations(origin);
    }

    public List<Leg> getAllLegs() {
        return getAllLegs(origin);
    }

    public static List<Leg> getAllLegs(Station root) {
        List<Leg> legs = new ArrayList<>(root.getOnwardLegs());
        for (Leg leg : root.getConnectedOnwardLegs()) {
            legs.addAll(getAllLegs(leg.getDestination()));
        }
        return legs;
    }

    public List<Leg> getLastNLegs(int n) {
        int numberOfLegs = legsInChronoOrder.size();
        int start = Math.max(0, numberOfLegs - n);
        return legsInChronoOrder.subList(start, numberOfLegs);

    }

    public List<Leg> getAllLegsInChronoOrder() {
        return legsInChronoOrder;
    }

    public List<Station> getAllStationsInChronoOrder() {
        List<Station> stations = new ArrayList<>();
        for (Leg leg : getAllLegsInChronoOrder()) {
            if (leg.hasDestination()) {
                stations.add(leg.getDestination());
            }
        }
        return stations;
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


    public void connect(Station joinInThisSurvey, Survey survey, Station joinInOtherSurvey) {

        if (this.equals(survey)) {
            throw new IllegalArgumentException("Can't join a survey onto itself");
        } else if (isConnectedTo(survey)) {
            throw new IllegalArgumentException("Already connected to that survey");
        }

        SurveyConnection connection = new SurveyConnection(joinInOtherSurvey, survey);

        Set<SurveyConnection> connections;
        if (stationsToSurveyConnections.containsKey(joinInThisSurvey)) {
            connections = stationsToSurveyConnections.get(joinInThisSurvey);
        } else {
            connections = new HashSet<>();
            stationsToSurveyConnections.put(joinInThisSurvey, connections);
        }

        //noinspection ConstantConditions - this will not be null
        connections.add(connection);
    }


    public void disconnect(Station joinInThisSurvey, Survey otherSurvey) throws Exception {
        Set<SurveyConnection> connections = getSurveysConnectedTo(joinInThisSurvey);

        for (SurveyConnection connection : connections) {
            if (connection.otherSurvey == otherSurvey) {
                connections.remove(connection);
                if (connections.isEmpty()) {
                    stationsToSurveyConnections.remove(joinInThisSurvey);
                }
                return;
            }
        }
        throw new Exception("tried to disconnect unconnected survey");

    }


    public boolean isConnectedTo(Survey other) {
        for (Station station : getConnectedSurveys().keySet()) {
            if (isStationConnectedTo(station, other)) {
                return true;
            }
        }
        return false;
    }

    private boolean isStationConnectedTo(Station station, Survey other) {
        if (stationsToSurveyConnections.containsKey(station)) {
            Set<SurveyConnection> connections = getSurveysConnectedTo(station);
            for (SurveyConnection connection : connections) {
                if (connection.otherSurvey.equals(other)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Map<Station, Set<SurveyConnection>> getConnectedSurveys() {
        return stationsToSurveyConnections;
    }


    public Set<Survey> getRecursiveConnectedSurveys() {
        Set<Survey> allSurveys = new HashSet<>();
        getRecursiveConnectedSurveys(allSurveys, this);
        return allSurveys;
    }

    private void getRecursiveConnectedSurveys(Set<Survey> allSurveys, Survey survey) {
        for (Set<SurveyConnection> set : survey.stationsToSurveyConnections.values()) {
            for (SurveyConnection connection : set) {
                Survey other = connection.otherSurvey;
                if (!allSurveys.contains(other)) {
                    allSurveys.add(other);
                    getRecursiveConnectedSurveys(allSurveys, other);
                }
            }
        }
    }

    public Set<SurveyConnection> getSurveysConnectedTo(Station station) {
        Set<SurveyConnection> connections = getConnectedSurveys().get(station);
        if (connections == null) {
            connections = new HashSet<>();
        }
        return connections;
    }


    public boolean hasLinkedSurveys(Station station) {
        return stationsToSurveyConnections.containsKey(station);
    }

    public void checkSurveyIntegrity() {

        List<Station> reachableStations = getAllStations();

        List<Leg> badLegs = new ArrayList<>();
        for (Leg leg : legsInChronoOrder) {
            if (leg.hasDestination() && (!reachableStations.contains(leg.getDestination()))) {
                badLegs.add(leg);
            }
        }
        legsInChronoOrder.removeAll(badLegs);

        if (!reachableStations.contains(activeStation)) {
            activeStation = findNewActiveStation();
        }

        List<Station> unreachableStations = new ArrayList<>();
        for (Station station : stationsToSurveyConnections.keySet()) {
            if (!reachableStations.contains(station)) {
                unreachableStations.add(station);
            }
        }
        for (Station station : unreachableStations) {
            stationsToSurveyConnections.remove(station);
        }
    }

    private Station findNewActiveStation() {
        for (int i = legsInChronoOrder.size() - 1; i >= 0; i--) {
            Leg leg = legsInChronoOrder.elementAt(i);
            if (leg.hasDestination()) {
                return leg.getDestination();
            }
        }
        return origin;
    }


    public void addLegRecord(Leg leg) {
        legsInChronoOrder.push(leg);
    }

    public void replaceLegInRecord(Leg oldLeg, Leg newLeg) {
        int oldIndex = legsInChronoOrder.indexOf(oldLeg);
        legsInChronoOrder.insertElementAt(newLeg, oldIndex + 1);
        legsInChronoOrder.remove(oldIndex);
        checkSurveyIntegrity();
    }


    public void removeLegRecord(Leg leg) {
        legsInChronoOrder.remove(leg);
    }

    public Leg getReferringLeg(final Station station) {

        if (station == getOrigin()) {
            return null;
        }

        final Wrapper wrapper = new Wrapper();
        SurveyTools.traverseLegs(
            this,
            (origin, leg) -> {
                if (leg.getDestination() == station) {
                    wrapper.value = leg;
                    return true;
                } else {
                    return false;
                }
            });
        return (Leg)(wrapper.value);
    }


    public Station getOriginatingStation(final Leg leg) {
        final Wrapper wrapper = new Wrapper();
        SurveyTools.traverseStations(
            this,
            station -> {
                if (station.getOnwardLegs().contains(leg)) {
                    wrapper.value = station;
                    return true;
                } else {
                    return false;
                }
            });

        return (Station)(wrapper.value);
    }


    public void undoAddLeg() {

        if (legsInChronoOrder.isEmpty()) {
            return;
        }

        final Leg toDelete = legsInChronoOrder.pop();
        SurveyTools.traverseLegs(
            this,
            (origin, leg) -> {
                if (leg == toDelete) {
                    origin.getOnwardLegs().remove(toDelete);
                    return true;
                } else {
                    return false;
                }
            }
        );

        if (toDelete.hasDestination()) {
            checkSurveyIntegrity();
        }

        setSaved(false);
    }

    public Station getStationByName(final String name) {
        final Wrapper wrapper = new Wrapper();
        SurveyTools.traverseStations(
            this,
            station -> {
                if (station.getName().equals(name)) {
                    wrapper.value = station;
                    return true;
                } else {
                    return false;
                }
            });

        return (Station)(wrapper.value);
    }


    public Trip getTrip() {
        return trip;
    }


    public void setTrip(Trip trip) {
        this.trip = trip;
    }


    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Survey)) {
            return false;
        }

        Survey other = (Survey)object;
        Uri uri = getUri();
        Uri otherUri = other.getUri();
        if (uri == null || otherUri == null) {
            return false;
        }
        return uri.equals(otherUri);

    }


    @Override
    public int hashCode() {
        Uri uri = getUri();
        if (uri != null) {
            return uri.hashCode();
        } else {
            return getName().hashCode();
        }
    }

    @NonNull
    public String toString() {
        return "[Survey " + getName() + "]";
    }



}

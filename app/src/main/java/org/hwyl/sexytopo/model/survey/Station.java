package org.hwyl.sexytopo.model.survey;

import org.hwyl.sexytopo.model.graph.Direction;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;


public class Station extends SurveyComponent {

    private String name;
    private List<Leg> onwardLegs = new ArrayList<>();
    private String comment = "";
    private Direction extendedElevationDirection = Direction.RIGHT;

    public static final char[] FORBIDDEN_CHARS = new char[]{'\n', '\r'};


    public Station(String name) {
        this.name = sanitiseName(name);
    }

    public Station(String name, String comment) {
        this.name = sanitiseName(name);
        this.comment = comment;
    }

    private static String sanitiseName(String name) {
        for (char c : FORBIDDEN_CHARS) {
            name = name.replace(Character.toString(c), "");
        }
        return name;
    }

    public Station(Station station, String name) {
        this.name = name;

        this.onwardLegs = station.onwardLegs;
        this.comment = station.comment;
        this.extendedElevationDirection = station.extendedElevationDirection;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = sanitiseName(name);
    }

    public List<Leg> getOnwardLegs() {
        return onwardLegs;
    }

    public void addOnwardLeg(Leg leg) {
        onwardLegs.add(leg);
    }

    public List<Leg> getUnconnectedOnwardLegs() {
        List<Leg> unconnectedOnwardLegs = new ArrayList<>();
        for (Leg leg : onwardLegs) {
            if (!leg.hasDestination()) {
                unconnectedOnwardLegs.add(leg);
            }
        }
        return unconnectedOnwardLegs;
    }

    public List<Leg> getConnectedOnwardLegs() {
        List<Leg> connectedOnwardLegs = new ArrayList<>();
        for (Leg leg : onwardLegs) {
            if (leg.hasDestination()) {
                connectedOnwardLegs.add(leg);
            }
        }
        return connectedOnwardLegs;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean hasComment() {
        return !comment.isEmpty();
    }

    public Direction getExtendedElevationDirection() {
        return extendedElevationDirection;
    }

    public void setExtendedElevationDirection(Direction extendedElevationDirection) {
        this.extendedElevationDirection = extendedElevationDirection;
    }

    public void switchDirection() {
        if (extendedElevationDirection == Direction.LEFT) {
            setExtendedElevationDirection(Direction.RIGHT);
        } else {
            setExtendedElevationDirection(Direction.LEFT);
        }
    }

    @NonNull
    public String toString() {
        return name;
    }
}

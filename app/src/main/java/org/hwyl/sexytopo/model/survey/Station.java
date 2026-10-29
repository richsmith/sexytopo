package org.hwyl.sexytopo.model.survey;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import org.hwyl.sexytopo.model.geometry.ExtendedElevationDirection;

public class Station extends SurveyComponent {

    private String name;
    private List<Leg> onwardLegs = new ArrayList<>();
    private String comment = "";
    private ExtendedElevationDirection extendedElevationDirection =
            ExtendedElevationDirection.DEFAULT;

    public static final char[] FORBIDDEN_CHARS = new char[] {'\n', '\r'};

    /** The anonymous destination of a splay to a wall, shown as a dash. */
    public static final Station ANONYMOUS_WALL = new Station("-");

    /** The anonymous destination of a splay to another feature, such as a stal, shown as a dot. */
    public static final Station ANONYMOUS_FEATURE = new Station(".");

    /** The anonymous station with this name, or null if there is none. */
    public static Station getAnonymous(String name) {
        if (name.equals(ANONYMOUS_WALL.getName())) {
            return ANONYMOUS_WALL;
        } else if (name.equals(ANONYMOUS_FEATURE.getName())) {
            return ANONYMOUS_FEATURE;
        } else {
            return null;
        }
    }

    /** Whether the name is used by an anonymous station, so a real station cannot have it. */
    public static boolean isReservedName(String name) {
        return getAnonymous(name) != null;
    }

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

    /** Whether this is one of the anonymous destinations of a splay rather than a real station. */
    public boolean isAnonymous() {
        return this == ANONYMOUS_WALL || this == ANONYMOUS_FEATURE;
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

    public ExtendedElevationDirection getExtendedElevationDirection() {
        return extendedElevationDirection;
    }

    public void setExtendedElevationDirection(
            ExtendedElevationDirection extendedElevationDirection) {
        this.extendedElevationDirection = extendedElevationDirection;
    }

    @NonNull
    public String toString() {
        return name;
    }
}

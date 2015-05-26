package org.hwyl.sexytopo.model.survey;

/**
 * Created by rls on 16/07/14.
 */
public class Leg extends SurveyComponent{

    private final double distance; // in metres
    private final double bearing;
    private final double inclination;
    private final Station destination;

    public Leg(double distance,
               double bearing,
               double inclination) {
        this(distance, bearing, inclination, Survey.NULL_STATION);
    }

    public Leg(double distance,
               double bearing,
               double inclination,
               Station destination) {

        if (destination == null) {
            throw new IllegalArgumentException("Destination of leg should not be null");
        }

        this.distance = distance;
        this.bearing = bearing;
        this.inclination = inclination;
        this.destination = destination;
    }

    public static Leg upgradeSplayToConnectedLeg(Leg splay, Station destination) {
        return new Leg(splay.distance, splay.bearing, splay.inclination, destination);
    }

    public Leg reverse() {
        double reversedBearing = (getBearing() + 180.0) % 360.0;
        double reversedInclination = -getInclination();
        if (hasDestination()) {
            return new Leg(getDistance(), reversedBearing, reversedInclination, getDestination());
        } else {
            return new Leg(getDistance(), reversedBearing, reversedInclination);
        }
    }

    public double getDistance() {
        return distance;
    }

    public double getBearing() {
        return bearing;
    }

    public double getInclination() {
        return inclination;
    }

    public Station getDestination() {
        return destination;
    }

    public boolean hasDestination() {
        return destination != Survey.NULL_STATION;
    }

}

package org.hwyl.sexytopo.model;

/**
 * Created by rls on 16/07/14.
 */
public class Leg {

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
        this.distance = distance;
        this.bearing = bearing;
        this.inclination = inclination;
        this.destination = destination;
    }

    public static Leg upgradeSplayToConnectedLeg(Leg splay, Station destination) {
        return new Leg(splay.distance, splay.bearing, splay.inclination, destination);
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

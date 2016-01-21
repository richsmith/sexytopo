package org.hwyl.sexytopo.model.survey;

import org.hwyl.sexytopo.control.util.Space2DUtils;

/**
 * Created by rls on 16/07/14.
 */
public class Leg extends SurveyComponent{

    public static final int MIN_DISTANCE = 0;
    public static final int MIN_BEARING = 0;
    public static final int MAX_BEARING = 360;
    public static final int MIN_INCLINATION = -90;
    public static final int MAX_INCLINATION = 90;


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

        if (!isDistanceLegal(distance)) {
            throw new IllegalArgumentException("Distance should be positive; actual" + distance);
        }

        if (!isAzimuthLegal(bearing)) {
            throw new IllegalArgumentException(
                    "Bearing should be at least 0 and less than 360; actual=" + bearing);
        }

        if (!isInclinationLegal(inclination)) {
            throw new IllegalArgumentException(
                    "Inclination should be up to +-90; actual=" + inclination);
        }

        this.distance = distance;
        this.bearing = bearing;
        this.inclination = inclination;
        this.destination = destination;
    }

    public Leg(Leg leg, Station destination) {
        this(leg.distance, leg.bearing, leg.inclination, destination);
    }

    public static Leg upgradeSplayToConnectedLeg(Leg splay, Station destination) {
        return new Leg(splay.distance, splay.bearing, splay.inclination, destination);
    }

    public Leg reverse() {
        return rotate(180.0);
    }

    public Leg rotate(double delta) {
        double adjustedBearing = Space2DUtils.adjustAngle(getBearing(), delta);
        if (hasDestination()) {
            return new Leg(getDistance(), adjustedBearing, getInclination(), getDestination());
        } else {
            return new Leg(getDistance(), adjustedBearing, getInclination());
        }
    }

    /**
     * Produce the exact opposite backsight for this leg.
     * @param destination The new destination (aka the former source)
     * @return
     */
    public Leg asBacksight(Station destination) {
        double backAzm = (getBearing() + 180) % 360;
        return new Leg(getDistance(), backAzm, -1 * getInclination(), destination);
    }

    /** Produce the exact opposite backsight for this splay leg. */
    public Leg asBacksight() {
        return asBacksight(Survey.NULL_STATION);
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

    public static boolean isDistanceLegal(double distance) {
        return distance >= MIN_DISTANCE;
    }

    public static boolean isAzimuthLegal(double azimuth) {
        return MIN_BEARING <= azimuth && azimuth < MAX_BEARING;
    }

    public static boolean isInclinationLegal(double inclination) {
        return MIN_INCLINATION <= inclination && inclination <= MAX_INCLINATION;
    }

}

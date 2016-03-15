package org.hwyl.sexytopo.model.survey;

import org.hwyl.sexytopo.control.util.Space2DUtils;

/**
 * Created by rls on 16/07/14.
 */
public class Leg extends SurveyComponent{

    public static final int MIN_DISTANCE = 0;
    public static final int MIN_AZIMUTH = 0;
    public static final int MAX_AZIMUTH = 360;
    public static final int MIN_INCLINATION = -90;
    public static final int MAX_INCLINATION = 90;


    private final double distance; // in metres
    private final double azimuth;
    private final double inclination;
    private final Station destination;

    public Leg(double distance,
               double azimuth,
               double inclination) {
        this(distance, azimuth, inclination, Survey.NULL_STATION);
    }

    public Leg(double distance,
               double azimuth,
               double inclination,
               Station destination) {

        if (destination == null) {
            throw new IllegalArgumentException("Destination of leg should not be null");
        }

        if (!isDistanceLegal(distance)) {
            throw new IllegalArgumentException("Distance should be positive; actual" + distance);
        }

        if (!isAzimuthLegal(azimuth)) {
            throw new IllegalArgumentException(
                    "Azimuth should be at least 0 and less than 360; actual=" + azimuth);
        }

        if (!isInclinationLegal(inclination)) {
            throw new IllegalArgumentException(
                    "Inclination should be up to +-90; actual=" + inclination);
        }

        this.distance = distance;
        this.azimuth = azimuth;
        this.inclination = inclination;
        this.destination = destination;
    }

    public Leg(Leg leg, Station destination) {
        this(leg.distance, leg.azimuth, leg.inclination, destination);
    }

    public static Leg upgradeSplayToConnectedLeg(Leg splay, Station destination) {
        return new Leg(splay.distance, splay.azimuth, splay.inclination, destination);
    }

    public Leg reverse() {
        return rotate(180.0);
    }

    public Leg rotate(double delta) {
        double adjustedAzimuth = Space2DUtils.adjustAngle(getAzimuth(), delta);
        if (hasDestination()) {
            return new Leg(getDistance(), adjustedAzimuth, getInclination(), getDestination());
        } else {
            return new Leg(getDistance(), adjustedAzimuth, getInclination());
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

    public double getAzimuth() {
        return azimuth;
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
        return MIN_AZIMUTH <= azimuth && azimuth < MAX_AZIMUTH;
    }

    public static boolean isInclinationLegal(double inclination) {
        return MIN_INCLINATION <= inclination && inclination <= MAX_INCLINATION;
    }

}

package org.hwyl.sexytopo.model.survey;

import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.table.TableCol;


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
    private final Leg[] promotedFrom;
    private final boolean wasShotBackwards;

    private final static Leg[] NO_LEGS = new Leg[]{};

    public Leg(double distance,
               double azimuth,
               double inclination) {
        this(distance, azimuth, inclination, false);
    }

    public Leg(double distance,
               double azimuth,
               double inclination,
               boolean wasShotBackwards) {
        this(distance, azimuth, inclination, Survey.NULL_STATION, NO_LEGS, wasShotBackwards);
    }

    public Leg(double distance,
               double azimuth,
               double inclination,
               Station destination,
               Leg[] promotedFrom) {
        this(distance, azimuth, inclination, destination, promotedFrom, false);
    }

    public Leg(double distance,
               double azimuth,
               double inclination,
               Station destination,
               Leg[] promotedFrom,
               boolean wasShotBackwards) {

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
        this.promotedFrom = promotedFrom;
        this.wasShotBackwards = wasShotBackwards;

    }

    public Leg(Leg leg, Station destination) {
        this(leg.distance, leg.azimuth, leg.inclination, destination,
                leg.promotedFrom, leg.wasShotBackwards);
    }

    public static Leg manuallyUpgradeSplayToConnectedLeg(Leg splay, Station destination) {
        return upgradeSplayToConnectedLeg(splay, destination, NO_LEGS);
    }

    public static Leg upgradeSplayToConnectedLeg(
            Leg splay, Station destination, Leg[] promotedFrom) {
        return new Leg(
                splay.distance, splay.azimuth, splay.inclination,
                destination, promotedFrom, splay.wasShotBackwards);
    }

    public Leg reverse() {
        double adjustedAzimuth = Space2DUtils.adjustAngle(getAzimuth(), 180);
        if (hasDestination()) {
            return new Leg(
                    distance, adjustedAzimuth, -1 * inclination, destination,
                    promotedFrom, !wasShotBackwards);
        } else {
            return new Leg(distance, adjustedAzimuth, -1 * inclination, !wasShotBackwards);
        }
    }

    public Leg rotate(double delta) {
        double adjustedAzimuth = Space2DUtils.adjustAngle(getAzimuth(), delta);
        return adjustAzimuth(adjustedAzimuth);
    }

    public Leg adjustAzimuth(double newAzimuth) {
        if (hasDestination()) {
            return new Leg(getDistance(), newAzimuth, getInclination(),
                    getDestination(), getPromotedFrom());
        } else {
            return new Leg(getDistance(), newAzimuth, getInclination());
        }
    }

    public Leg asBacksight(Station destination) {
        double backAzimuth = Space2DUtils.adjustAngle(getAzimuth(), 180.0);
        return new Leg(getDistance(), backAzimuth, -1 * getInclination(),
                destination, getPromotedFrom());
    }

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

    public Leg[] getPromotedFrom() {
        return promotedFrom;
    }

    public boolean wasPromoted() {
        return promotedFrom.length > 0;
    }

    public boolean wasShotBackwards() {
        return wasShotBackwards;
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

    public String toString() {
        return
            "(D" + TableCol.DISTANCE.format(distance) +
            " A" + TableCol.AZIMUTH.format(azimuth) +
            " I" + TableCol.INCLINATION.format(inclination) + ")";
    }

}

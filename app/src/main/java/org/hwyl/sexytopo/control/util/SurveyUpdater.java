package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.model.graph.Direction;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.Arrays;
import java.util.List;


@SuppressWarnings("UnnecessaryLocalVariable")
public class SurveyUpdater {

    public static boolean update(Survey survey, List<Leg> legs, InputMode inputMode) {
        boolean anyStationsAdded = false;
        for (Leg leg : legs) {
            anyStationsAdded = anyStationsAdded || update(survey, leg, inputMode);
        }
        return anyStationsAdded;
    }


    public static boolean update(Survey survey, List<Leg> legs) {
        boolean anyStationsAdded = false;
        for (Leg leg : legs) {
            anyStationsAdded = anyStationsAdded || update(survey, leg);
        }
        return anyStationsAdded;
    }

    public static boolean update(Survey survey, Leg leg) {
        return update(survey, leg, InputMode.FORWARD);
    }

    public static synchronized boolean update(Survey survey, Leg leg, InputMode inputMode) {
        Station activeStation = survey.getActiveStation();

        Log.d("Adding leg " + leg);
        activeStation.getOnwardLegs().add(leg);
        survey.setSaved(false);
        survey.addLegRecord(leg);

        boolean justCreatedNewStation = false;
        switch(inputMode) {
            case FORWARD:
                justCreatedNewStation = createNewStationIfTripleShot(survey, false);
                break;
            case BACKWARD:
                justCreatedNewStation = createNewStationIfTripleShot(survey, true);
                break;
            case COMBO:
                justCreatedNewStation = createNewStationIfBacksight(survey) ||
                        createNewStationIfTripleShot(survey, false);
            case CALIBRATION_CHECK:
                break; // do nothing :)
        }

        if (justCreatedNewStation) {
            Log.d("Created new station " + survey.getActiveStation().getName());
        }

        return justCreatedNewStation;
    }


    public static void updateWithNewStation(Survey survey, Leg leg) {
        Station activeStation = survey.getActiveStation();

        if (!leg.hasDestination()) {
            Station newStation =
                    new Station(StationNamer.generateNextStationName(survey, activeStation));
            leg = Leg.manuallyUpgradeSplayToConnectedLeg(leg, newStation);
        }

        // FIXME; could the below be moved into Survey? And from elsewhere in this file?
        activeStation.getOnwardLegs().add(leg);
        survey.setSaved(false);
        survey.addLegRecord(leg);
        survey.setActiveStation(leg.getDestination());
    }

    public static void upgradeSplayToConnectedLeg(Survey survey, Leg leg, InputMode inputMode) {
        Station newStation = new Station(getNextStationName(survey));

        Leg newLeg = Leg.manuallyUpgradeSplayToConnectedLeg(leg, newStation);

        if (inputMode == InputMode.BACKWARD) {
            newLeg = newLeg.reverse();
        }

        editLeg(survey, leg, newLeg);
        survey.setActiveStation(newStation);
    }

    private static synchronized String getNextStationName(Survey survey) {
        return StationNamer.generateNextStationName(survey, survey.getActiveStation());
    }

    private static boolean createNewStationIfTripleShot(Survey survey, boolean backsightMode) {

        int requiredNumber = SexyTopoConstants.NUM_OF_REPEATS_FOR_NEW_STATION;

        Station activeStation = survey.getActiveStation();
        List<Leg> activeLegs = activeStation.getOnwardLegs();
        if (activeLegs.size() < requiredNumber) {
            return false;
        }

        List<Leg> lastNLegs = survey.getLastNLegs(requiredNumber);
        if (lastNLegs.size() < requiredNumber) {
            return false;
        }

        // check all the last legs are from the active station
        for (Leg leg : lastNLegs) {
            if (!activeLegs.contains(leg)) {
                return false;
            }
        }

        if (areLegsAboutTheSame(lastNLegs)) {

            Station newStation = new Station(getNextStationName(survey));
            newStation.setExtendedElevationDirection(
                    activeStation.getExtendedElevationDirection());

            Leg newLeg = averageLegs(lastNLegs);
            newLeg = Leg.upgradeSplayToConnectedLeg(
                    newLeg, newStation, lastNLegs.toArray(new Leg[]{}));

            if (backsightMode) {
                newLeg = newLeg.reverse();
            }

            for (int i = 0; i < requiredNumber; i++) {
                survey.undoAddLeg();
            }

            activeStation.getOnwardLegs().add(newLeg);
            survey.addLegRecord(newLeg);
            survey.setActiveStation(newStation);

            return true;
        }

        return false;
    }


    private static boolean createNewStationIfBacksight(Survey survey) {
        // Examine splays of the current active station to determine if the previous two were a
        // foreward and backsight; if so, promote to a new named station

        Station activeStation = survey.getActiveStation();
        List<Leg> activeLegs = activeStation.getOnwardLegs();
        if (activeLegs.size() < 2) {
            return false;
        }

        List<Leg> lastPair = survey.getLastNLegs(2);

        if (lastPair.size() < 2) {
            return false;
        }

        for (Leg leg : lastPair) {
            if (!activeLegs.contains(leg)) {
                return false;
            }
        }


        Leg fore = lastPair.get(lastPair.size() - 2);
        Leg back = lastPair.get(lastPair.size() - 1);  // TODO: check for "reverse mode" to see if backsight comes first?

        if (areLegsBacksights(fore, back)) {
            Station newStation = new Station(getNextStationName(survey));
            newStation.setExtendedElevationDirection(
                    activeStation.getExtendedElevationDirection());

            Leg newLeg = averageBacksights(fore, back);
            newLeg = Leg.manuallyUpgradeSplayToConnectedLeg(newLeg, newStation);

            survey.undoAddLeg();
            survey.undoAddLeg();

            activeStation.getOnwardLegs().add(newLeg);
            survey.addLegRecord(newLeg);

            survey.setActiveStation(newStation);
            return true;
        }

        return false;
    }


    public static synchronized void editLeg(
            final Survey survey, final Leg toEdit, final Leg edited) {
        SurveyTools.traverseLegs(
                survey,
                (origin, leg) -> {
                    if (leg == toEdit) {
                        origin.getOnwardLegs().remove(toEdit);
                        origin.getOnwardLegs().add(edited);
                        survey.replaceLegInRecord(toEdit, edited);
                        Log.d("Edited leg " + toEdit + " -> " + edited);
                        return true;
                    } else {
                        return false;
                    }
                });
        survey.setSaved(false);
    }


    public static void renameStation(Survey survey, Station station, String name) {
        String previousName = station.getName();
        Log.d("Renaming station from: " + Log.formatDebugString(previousName) + " to: " + Log.formatDebugString(name));

        Station existing = survey.getStationByName(name);
        if (existing != null) {
            throw new IllegalArgumentException("New station name is not unique");
        }

        station.setName(name);
        Log.d("Renamed station from: " + Log.formatDebugString(previousName) + " to: " + Log.formatDebugString(station.getName()));
    }


    public static void moveLeg(Survey survey, Leg leg, Station newSource) {
        Station originating = survey.getOriginatingStation(leg);
        originating.getOnwardLegs().remove(leg);
        newSource.addOnwardLeg(leg);
        survey.setSaved(false);
    }


    public static void deleteStation(final Survey survey, final Station toDelete) {
        if (toDelete == survey.getOrigin()) {
            return;
        }

        Leg referringLeg = survey.getReferringLeg(toDelete);
        survey.removeLegRecord(referringLeg);

        SurveyTools.traverseLegs(survey, (origin, leg) -> {
            if (leg.hasDestination() && leg.getDestination() == toDelete) {
                origin.getOnwardLegs().remove(leg);
                survey.checkSurveyIntegrity();
                return true;
            } else {
                return false;
            }
        });

        survey.setSaved(false);
    }


    public static void deleteSplay(Survey survey, Station station, Leg splay) {
        survey.removeLegRecord(splay);
        station.getOnwardLegs().remove(splay);
        survey.setSaved(false);
    }


    private static boolean areLegsAboutTheSame(List<Leg> legs) {

        for (Leg leg : legs) { // full legs must be unique by definition
            if (leg.hasDestination()) {
                return false;
            }
        }

        float minDistance = Float.POSITIVE_INFINITY, maxDistance = Float.NEGATIVE_INFINITY;
        float minAzimuth = Float.POSITIVE_INFINITY, maxAzimuth = Float.NEGATIVE_INFINITY;
        float minInclination = Float.POSITIVE_INFINITY, maxInclination = Float.NEGATIVE_INFINITY;

        for (Leg leg : legs) {
            minDistance = Math.min(leg.getDistance(), minDistance);
            maxDistance = Math.max(leg.getDistance(), maxDistance);
            minAzimuth = Math.min(leg.getAzimuth(), minAzimuth);
            maxAzimuth = Math.max(leg.getAzimuth(), maxAzimuth);
            minInclination = Math.min(leg.getInclination(), minInclination);
            maxInclination = Math.max(leg.getInclination(), maxInclination);
        }

        float distanceDiff = maxDistance - minDistance;
        float azimuthDiff = maxAzimuth - minAzimuth;
        float inclinationDiff = maxInclination - minInclination;

        float maxDistanceDelta = PreferenceAccess.getMaxDistanceDelta();
        float maxAngleDelta = PreferenceAccess.getMaxAngleDelta();

        return distanceDiff <= maxDistanceDelta &&
               azimuthDiff <= maxAngleDelta &&
               inclinationDiff <= maxAngleDelta;
    }



    public static boolean areLegsBacksights(Leg fore, Leg back) {
        // Given two legs, determine if they are in agreement as foresight and backsight.
        Leg correctedBack = back.asBacksight();
        return areLegsAboutTheSame(Arrays.asList(fore, correctedBack));
    }

    public static Leg averageLegs(List<Leg> repeats) {
        int count = repeats.size();
        float distance = 0.0f, inclination = 0.0f;
        float[] azimuths = new float[count];
        for (int i=0; i < count; i++) {
            Leg leg = repeats.get(i);
            distance += leg.getDistance();
            inclination += leg.getInclination();
            azimuths[i] = leg.getAzimuth();
        }
        distance /= count;
        inclination /= count;
        return new Leg(distance, averageAzimuths(azimuths), inclination);
    }


    public static Leg averageBacksights(Leg fore, Leg back) {
        // Given a foresight and backsight which may not exactly agree, produce an averaged foresight
        return averageLegs(Arrays.asList(fore, back.asBacksight()));
    }


    /** Average some azimuth values together, even if they span the 360/0 boundary */
    private static float averageAzimuths(float[] azimuths) {
        // Azimuth values jump at the 360/0 boundary, so we must be careful to ensure that
        // values {359, 1} average to 0 rather than the incorrect value 180
        float sum = 0.0f;
        float min = Leg.MAX_AZIMUTH, max = Leg.MIN_AZIMUTH;
        for (float azimuth : azimuths) {
            if (azimuth < min) {
                min = azimuth;
            }
            if (azimuth > max) {
                max = azimuth;
            }
        }
        boolean splitOverZero = max - min > 180;
        float[] correctedAzms = new float[azimuths.length];
        for (int i = 0; i < azimuths.length; i++) {
            correctedAzms[i] =
                    (splitOverZero && azimuths[i] < 180) ? azimuths[i] + 360: azimuths[i];
            sum += correctedAzms[i];
        }
        return (sum / correctedAzms.length) % 360;
    }


    public static void reverseLeg(final Survey survey, final Station toReverse) {
        Log.d("Reversing leg to " + toReverse.getName());
        SurveyTools.traverseLegs(
                survey,
                (origin, leg) -> {
                    if (leg.hasDestination() && leg.getDestination() == toReverse) {
                        Leg reversed = leg.reverse();
                        Log.d("Reversed direction of leg " + leg + " to " + reversed);
                        origin.getOnwardLegs().remove(leg);
                        origin.addOnwardLeg(reversed);
                        survey.replaceLegInRecord(leg, reversed);
                        return true;
                    } else {
                        return false;
                    }
                });
        survey.setSaved(false);
    }


    public static void setDirectionOfSubtree(Station station, Direction direction) {
        station.setExtendedElevationDirection(direction);
        for (Leg leg : station.getConnectedOnwardLegs()) {
            Station destination = leg.getDestination();
            setDirectionOfSubtree(destination, direction);
        }
    }

}

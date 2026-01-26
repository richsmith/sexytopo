package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SexyTopo;
import org.hwyl.sexytopo.model.graph.Direction;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.Arrays;
import java.util.List;


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

        Log.i(R.string.survey_update_adding_leg, leg);
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

        return justCreatedNewStation;
    }


    public static void updateWithNewStation(Survey survey, Leg leg) {
        Station activeStation = survey.getActiveStation();

        if (!leg.hasDestination()) {
            Station newStation =
                    new Station(StationNamer.generateNextStationName(survey, activeStation));
            leg = Leg.toFullLeg(leg, newStation);
        }

        addLegFromStation(survey, activeStation, leg);
    }

    public static void addLegFromStation(Survey survey, Station fromStation, Leg leg) {
        Log.i(R.string.survey_update_adding_leg, leg);
        fromStation.getOnwardLegs().add(leg);
        survey.setSaved(false);
        survey.addLegRecord(leg);
        if (leg.hasDestination()) {
            survey.setActiveStation(leg.getDestination());
        }
    }

    public static void upgradeSplay(Survey survey, Leg leg, InputMode inputMode) {
        Station newStation = new Station(getNextStationName(survey));

        Leg newLeg = Leg.toFullLeg(leg, newStation);

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
            newLeg = Leg.toFullLeg(newLeg, newStation);

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
                    Log.d(R.string.survey_update_edited_leg, toEdit, edited);
                    return true;
                } else {
                    return false;
                }
            });
        survey.setSaved(false);
    }


    public static void renameStation(Survey survey, Station station, String name) {
        String previousName = station.getName();

        Station existing = survey.getStationByName(name);
        if (existing != null) {
            String message = SexyTopo.staticGetString(R.string.survey_update_rename_error_not_unique);
            throw new IllegalArgumentException(message);
        }

        station.setName(name);
        survey.setSaved(false);
        Log.i(R.string.survey_update_renamed_station, previousName, name);
    }

    public static void renameOrigin(Survey survey, String name) {
        renameStation(survey, survey.getOrigin(), name);
    }

    public static void moveLeg(Survey survey, Leg leg, Station newSource) {
        Station originating = survey.getOriginatingStation(leg);
        originating.getOnwardLegs().remove(leg);
        newSource.addOnwardLeg(leg);
        survey.setSaved(false);
        Log.i(R.string.survey_update_moved_leg, newSource.getName());
    }


    public static void deleteStation(final Survey survey, final Station toDelete) {
        if (!survey.isOrigin(toDelete)) {
            // Station comes as a package with the leg that forms it, so
            // remove that to delete the station from the graph
            Leg referringLeg = survey.getReferringLeg(toDelete);
            Station fromStation = survey.getOriginatingStation(referringLeg);
            deleteLeg(survey, fromStation, referringLeg);
        }
    }


    public static void deleteLeg(Survey survey, Station fromStation, Leg leg) {

        // First remove all legs in the subtree from the survey record
        if (leg.hasDestination()) {
            SurveyTools.traverseLegs(
                leg.getDestination(),
                (origin, subLeg) -> {
                    survey.removeLegRecord(subLeg);
                    return false;
                });
        }

        // Remove this leg's record
        survey.removeLegRecord(leg);

        // Then remove the leg from its originating station
        fromStation.getOnwardLegs().remove(leg);
        survey.checkSurveyIntegrity();
        survey.setSaved(false);
    }

    public static void downgradeLeg(Survey survey, Leg leg) {
        if (!leg.hasDestination()) {
            // Already a splay, so nothing to do
            return;
        }

        Station destination = leg.getDestination();

        if (!destination.getOnwardLegs().isEmpty()) {
            throw new IllegalStateException(
                "Cannot downgrade leg to splay: destination station has onward legs");
        }

        Leg newSplay = leg.toSplay();
        editLeg(survey, leg, newSplay);

        survey.checkSurveyIntegrity();
        survey.setSaved(false);
    }


    public static boolean areLegsAboutTheSame(List<Leg> legs) {

        for (Leg leg : legs) { // full legs must be unique by definition
            if (leg.hasDestination()) {
                return false;
            }
        }

        float minDistance = Float.POSITIVE_INFINITY, maxDistance = Float.NEGATIVE_INFINITY;
        float minAzimuth = Float.POSITIVE_INFINITY, maxAzimuth = Float.NEGATIVE_INFINITY;
        float minInclination = Float.POSITIVE_INFINITY, maxInclination = Float.NEGATIVE_INFINITY;
        float offsetAzimuth = 540 - legs.get(0).getAzimuth();

        for (Leg leg : legs) {
            minDistance = Math.min(leg.getDistance(), minDistance);
            maxDistance = Math.max(leg.getDistance(), maxDistance);
            float shiftedAzimuth = (leg.getAzimuth() + offsetAzimuth) % 360;
            minAzimuth = Math.min(shiftedAzimuth, minAzimuth);
            maxAzimuth = Math.max(shiftedAzimuth, maxAzimuth);
            minInclination = Math.min(leg.getInclination(), minInclination);
            maxInclination = Math.max(leg.getInclination(), maxInclination);
        }

        float distanceDiff = maxDistance - minDistance;
        float azimuthDiff = maxAzimuth - minAzimuth;
        float inclinationDiff = maxInclination - minInclination;

        float maxDistanceDelta = GeneralPreferences.getMaxDistanceDelta();
        float maxAngleDelta = GeneralPreferences.getMaxAngleDelta();

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
        SurveyTools.traverseLegs(
            survey,
            (origin, leg) -> {
                if (leg.hasDestination() && leg.getDestination() == toReverse) {
                    String previousDescription = leg.toString();
                    Leg reversed = leg.reverse();
                    String newDescription = reversed.toString();
                    origin.getOnwardLegs().remove(leg);
                    origin.addOnwardLeg(reversed);
                    survey.replaceLegInRecord(leg, reversed);
                    Log.i(R.string.survey_update_reversed_leg, previousDescription, newDescription);
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

package org.hwyl.sexytopo.control.util;

import android.util.Log;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.util.StationNamer;
import org.hwyl.sexytopo.model.Leg;
import org.hwyl.sexytopo.model.Station;
import org.hwyl.sexytopo.model.Survey;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rls on 23/07/14.
 */
public class SurveyUpdater {


    private static final double MAX_DISTANCE_DIFF = 0.2;
    private static final double MAX_BEARING_DIFF = 1;
    private static final double MAX_INCLINATION_DIFF = 1;

    public static void update(Survey survey, List<Leg> legs) {
        for (Leg leg : legs) {
            update(survey, leg);
        }
    }

    public static void update(Survey survey, Leg leg) {


        Station activeStation = survey.getActiveStation();
        activeStation.getOnwardLegs().add(leg);

        createNewStationIfRequired(survey);

    }


    private static void createNewStationIfRequired(Survey survey) {

        Station activeStation = survey.getActiveStation();
        if (activeStation.getOnwardLegs().size() >= SexyTopo.NUM_OF_REPEATS_FOR_NEW_STATION) {

            List<Leg> legs = getLatestNLegs(activeStation, SexyTopo.NUM_OF_REPEATS_FOR_NEW_STATION);

            if (areLegsAboutTheSame(legs)) {
                Station newStation = new Station(StationNamer.generateNextStationName(activeStation));

                Leg selectedLeg = selectLegToKeepFromRepeats(legs);
                activeStation.getOnwardLegs().removeAll(legs);

                Leg newLeg = Leg.upgradeSplayToConnectedLeg(selectedLeg, newStation);
                activeStation.getOnwardLegs().add(newLeg);

                survey.setActiveStation(newStation);
            }
        }
    }


    //private static void replaceRepeatedLegsWithLegToNewStation(Station activeStation, List<Leg> repeatedLegs, Station newStation) {}


    private static List<Leg> getLatestNLegs(Station station, int n) {
        List<Leg> legs = station.getOnwardLegs();
        List<Leg> lastNLegs = new ArrayList<>(legs.subList(legs.size() - n, legs.size()));
        return lastNLegs;
    }

    private static boolean areLegsAboutTheSame(List<Leg> legs) {

        double minDistance = Double.POSITIVE_INFINITY, maxDistance = Double.NEGATIVE_INFINITY;
        double minBearing = Double.POSITIVE_INFINITY, maxBearing = Double.NEGATIVE_INFINITY;
        double minInclination = Double.POSITIVE_INFINITY, maxInclination = Double.NEGATIVE_INFINITY;

        for (Leg leg : legs) {
            minDistance = Math.min(leg.getDistance(), minDistance);
            maxDistance = Math.max(leg.getDistance(), maxDistance);
            minBearing = Math.min(leg.getBearing(), minBearing);
            maxBearing = Math.max(leg.getBearing(), maxBearing);
            minInclination = Math.min(leg.getInclination(), minInclination);
            maxInclination = Math.max(leg.getInclination(), maxInclination);
        }

        double distanceDiff = maxDistance - minDistance;
        double bearingDiff = maxBearing - minBearing;
        double inclinationDiff = maxInclination - minInclination;

        return distanceDiff <= MAX_DISTANCE_DIFF &&
               bearingDiff <= MAX_BEARING_DIFF &&
               inclinationDiff <= MAX_INCLINATION_DIFF;

    }



    private static Leg selectLegToKeepFromRepeats(List<Leg> repeats) {
        // assuming the first one is going to have the most care taken...
        return repeats.get(0);
    }


}

package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rls on 23/07/14.
 */
public class SurveyUpdater {

    static {
        //MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.sound_file_1);
        //mediaPlayer.start(); // no need to call prepare(); create() does that for you
    }


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

        survey.addUndoEntry(activeStation, leg);

        createNewStationIfRequired(survey);
    }


    public static void updateWithNewStation(Survey survey, Leg leg) {

        Station activeStation = survey.getActiveStation();

        if (!leg.hasDestination()) {
            Station newStation = new Station(StationNamer.generateNextStationName(activeStation));
            leg = Leg.upgradeSplayToConnectedLeg(leg, newStation);
        }


        // FIXME; could the below be moved into Survey? And from elsewhere in this file?
        activeStation.getOnwardLegs().add(leg);
        survey.addUndoEntry(activeStation, leg);
        survey.setActiveStation(leg.getDestination());
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

                for (Leg leg : legs) {
                    survey.undoLeg();
                }
                survey.addUndoEntry(activeStation, newLeg);

                survey.setActiveStation(newStation);
            }
        }
    }


    public static void editLeg(Survey survey, final Leg toEdit, final Leg edited) {
        SurveyTools.traverse(
                survey,
                new SurveyTools.SurveyTraversalCallback() {
                    @Override
                    public boolean call(Station origin, Leg leg) {
                        if (leg == toEdit) {
                            origin.getOnwardLegs().remove(toEdit);
                            origin.getOnwardLegs().add(edited);
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
    }


    public static void deleteLeg(Survey survey, final Leg toDelete) {
        survey.deleteLeg(toDelete);
    }


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

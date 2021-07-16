package org.hwyl.sexytopo.testhelpers;

import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class BasicTestSurveyCreator {


    public static Survey createStraightNorthThroughRepeats() {
        Survey survey = new Survey("Test Straight Survey North");
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Leg leg = new Leg(5, 0, 0);
                SurveyUpdater.update(survey, leg);
            }
        }
        return survey;
    }


    public static Survey createStraightNorth() {
        Survey survey = new Survey("Test Straight Survey North");

        Leg leg0 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        Leg leg1 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg1);

        Leg leg2 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg2);

        return survey;
    }


    public static Survey createStraightSouth() {
        Survey survey = new Survey("Test Straight Survey South");

        Leg leg0 = new Leg(5, 180, 0);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        Leg leg1 = new Leg(5, 180, 0);
        SurveyUpdater.updateWithNewStation(survey, leg1);

        Leg leg2 = new Leg(5, 180, 0);
        SurveyUpdater.updateWithNewStation(survey, leg2);

        return survey;

    }


    public static Survey createRightRight() {
        Survey survey = new Survey("Test n-shaped Survey");

        Leg leg0 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        Leg leg1 = new Leg(5, 90, 0);
        SurveyUpdater.updateWithNewStation(survey, leg1);

        Leg leg2 = new Leg(5, 180, 0);
        SurveyUpdater.updateWithNewStation(survey, leg2);

        return survey;

    }


    public static Survey create5MDown() {
        Survey survey = new Survey("Test Survey 1m Down");

        Leg leg0 = new Leg(5, 0, -90);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        return survey;
    }


    public static Survey create5MEast() {
        Survey survey = new Survey("Test Survey 5m east");

        Leg leg0 = new Leg(5, 90, 0);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        Leg splay0Left = new Leg(1, 0, 0);
        SurveyUpdater.update(survey, splay0Left);
        Leg splay0Right = new Leg(1, 180, 0);
        SurveyUpdater.update(survey, splay0Right);

        return survey;
    }


    public static Survey createStraightNorthWith1EBranch() {
        Survey survey = new Survey("Test Straight Survey North With 1E Branch");

        Leg leg0 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        Leg leg1 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg1);

        Leg leg2 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg2);

        survey.setActiveStation(survey.getStationByName("1"));
        Leg legBranch = new Leg(5, 90, 0);
        SurveyUpdater.updateWithNewStation(survey, legBranch);

        return survey;
    }


    public static Survey createStraightNorthWith2EBranch() {
        Survey survey = new Survey("Test Straight Survey North With 2E Branch");

        Leg leg0 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        Leg leg1 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg1);

        Leg leg2 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg2);

        survey.setActiveStation(survey.getStationByName("1"));
        Leg legBranch = new Leg(5, 90, 0);
        SurveyUpdater.updateWithNewStation(survey, legBranch);

        Leg legBranch2 = new Leg(5, 90, 0);
        SurveyUpdater.updateWithNewStation(survey, legBranch2);

        return survey;
    }


    public static Survey createStraightNorthWith2EBranchFromS2() {
        Survey survey = new Survey("Test Straight Survey North With 2E Branch");

        Leg leg0 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        Leg leg1 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg1);

        Leg leg2 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg2);

        survey.setActiveStation(survey.getStationByName("2"));
        Leg legBranch = new Leg(5, 90, 0);
        SurveyUpdater.updateWithNewStation(survey, legBranch);

        Leg legBranch2 = new Leg(5, 90, 0);
        SurveyUpdater.updateWithNewStation(survey, legBranch2);

        return survey;
    }


    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    public static Survey createStraightNorthWithTrip() {
        Survey survey = new Survey("Test Straight Survey North");

        List<Trip.TeamEntry> team = new ArrayList<>();
        team.add(new Trip.TeamEntry(
                "Alice", Arrays.asList(Trip.Role.BOOK)));
        team.add(new Trip.TeamEntry("Bob",
                Arrays.asList(Trip.Role.INSTRUMENTS, Trip.Role.DOG)));
        Trip trip = new Trip();
        trip.setTeam(team);
        survey.setTrip(trip);

        Leg leg0 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg0);

        Leg leg1 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg1);

        Leg leg2 = new Leg(5, 0, 0);
        SurveyUpdater.updateWithNewStation(survey, leg2);

        return survey;
    }
}

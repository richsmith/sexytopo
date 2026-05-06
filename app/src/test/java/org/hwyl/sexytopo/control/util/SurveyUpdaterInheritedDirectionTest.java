package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.graph.Direction;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.junit.Assert;
import org.junit.Test;

public class SurveyUpdaterInheritedDirectionTest {

    private static final Leg DUMMY_LEG = new Leg(5, 0, 0);

    /**
     * Builds a linear survey with the given number of legs and returns it.
     * Station names will be "1" (origin), "2", "3", ...
     */
    private Survey buildLinearSurvey(int legCount) {
        Survey survey = new Survey();
        for (int i = 0; i < legCount; i++) {
            SurveyUpdater.updateWithNewStation(survey, DUMMY_LEG);
        }
        return survey;
    }

    // --- Non-vertical parent ---

    @Test
    public void testNonVerticalParentInheritsOwnDirection() {
        Survey survey = buildLinearSurvey(1);
        survey.getStationByName("2").setExtendedElevationDirection(Direction.LEFT);

        SurveyUpdater.updateWithNewStation(survey, DUMMY_LEG);

        Station newStation = survey.getStationByName("3");
        Assert.assertEquals(Direction.LEFT, newStation.getExtendedElevationDirection());
    }

    @Test
    public void testRightParentInheritsRight() {
        Survey survey = buildLinearSurvey(1);
        survey.getStationByName("2").setExtendedElevationDirection(Direction.RIGHT);

        SurveyUpdater.updateWithNewStation(survey, DUMMY_LEG);

        Station newStation = survey.getStationByName("3");
        Assert.assertEquals(Direction.RIGHT, newStation.getExtendedElevationDirection());
    }

    // --- Single vertical parent ---

    @Test
    public void testVerticalParentInheritsFromGrandparent() {
        // Origin("1") = RIGHT, station "2" = VERTICAL → new station "3" should inherit RIGHT
        Survey survey = buildLinearSurvey(1);
        survey.getStationByName("2").setExtendedElevationDirection(Direction.VERTICAL);

        SurveyUpdater.updateWithNewStation(survey, DUMMY_LEG);

        Station newStation = survey.getStationByName("3");
        Assert.assertEquals(Direction.RIGHT, newStation.getExtendedElevationDirection());
    }

    @Test
    public void testVerticalParentInheritsLeftFromGrandparent() {
        // Origin("1") = LEFT, station "2" = VERTICAL → new station "3" should inherit LEFT
        Survey survey = buildLinearSurvey(1);
        survey.getOrigin().setExtendedElevationDirection(Direction.LEFT);
        survey.getStationByName("2").setExtendedElevationDirection(Direction.VERTICAL);

        SurveyUpdater.updateWithNewStation(survey, DUMMY_LEG);

        Station newStation = survey.getStationByName("3");
        Assert.assertEquals(Direction.LEFT, newStation.getExtendedElevationDirection());
    }

    // --- Multiple consecutive vertical parents (the case the old code got wrong) ---

    @Test
    public void testTwoConsecutiveVerticalsInheritsFromGreatGrandparent() {
        // "1"=RIGHT, "2"=VERTICAL, "3"=VERTICAL → new station "4" should climb to "1" and get RIGHT
        Survey survey = buildLinearSurvey(2);
        survey.getStationByName("2").setExtendedElevationDirection(Direction.VERTICAL);
        survey.getStationByName("3").setExtendedElevationDirection(Direction.VERTICAL);

        SurveyUpdater.updateWithNewStation(survey, DUMMY_LEG);

        Station newStation = survey.getStationByName("4");
        Assert.assertEquals(Direction.RIGHT, newStation.getExtendedElevationDirection());
    }

    @Test
    public void testTwoConsecutiveVerticalsInheritsLeftFromGreatGrandparent() {
        // "1"=LEFT, "2"=VERTICAL, "3"=VERTICAL → new station "4" should climb to "1" and get LEFT
        Survey survey = buildLinearSurvey(2);
        survey.getOrigin().setExtendedElevationDirection(Direction.LEFT);
        survey.getStationByName("2").setExtendedElevationDirection(Direction.VERTICAL);
        survey.getStationByName("3").setExtendedElevationDirection(Direction.VERTICAL);

        SurveyUpdater.updateWithNewStation(survey, DUMMY_LEG);

        Station newStation = survey.getStationByName("4");
        Assert.assertEquals(Direction.LEFT, newStation.getExtendedElevationDirection());
    }

    // --- Vertical at origin (edge case) ---

    @Test
    public void testVerticalOriginFallsBackToRight() {
        // Origin itself is VERTICAL — no ancestor to inherit from, should fall back to RIGHT
        Survey survey = new Survey();
        survey.getOrigin().setExtendedElevationDirection(Direction.VERTICAL);

        SurveyUpdater.updateWithNewStation(survey, DUMMY_LEG);

        Station newStation = survey.getStationByName("2");
        Assert.assertEquals(Direction.RIGHT, newStation.getExtendedElevationDirection());
    }
}

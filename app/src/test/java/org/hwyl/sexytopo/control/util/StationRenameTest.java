package org.hwyl.sexytopo.control.util;


import org.hwyl.sexytopo.SexyTopoConstants;
import org.junit.Assert;

import org.hwyl.sexytopo.testhelpers.BasicTestSurveyCreator;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;


public class StationRenameTest {

    @Test
    public void testGetStationByNameGetsExistingStation() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();
        Station s1 = testSurvey.getStationByName("1");
        Assert.assertEquals("1", s1.getName());
    }

    @Test
    public void testGetStationByNameCanFail() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();
        Station shouldBeNull = testSurvey.getStationByName("I DO NOT EXIST :P");
        Assert.assertNull(shouldBeNull);
    }

    @Test
    public void testRenameOriginStation() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();
        Station s1 = testSurvey.getStationByName("1");
        SurveyUpdater.renameStation(testSurvey, s1, "ShinyNewNameOrigin");

        Station shinyNewStation = testSurvey.getStationByName("ShinyNewNameOrigin");
        Assert.assertEquals("ShinyNewNameOrigin", shinyNewStation.getName());
    }

    @Test
    public void testRenameStation() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();
        Station s2 = testSurvey.getStationByName("2");
        SurveyUpdater.renameStation(testSurvey, s2, "ShinyNewName");

        Station shinyNewStation = testSurvey.getStationByName("ShinyNewName");
        Assert.assertEquals("ShinyNewName", shinyNewStation.getName());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRenamingToExistingNameFails() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();
        Station s2 = testSurvey.getStationByName("2");
        SurveyUpdater.renameStation(testSurvey, s2, "1");
    }

    @Test
    public void testAverageLegs() {
        Leg leg1 = new Leg(10, 359, -1);
        Leg leg2 = new Leg(20,   1, +1);
        List<Leg> legs = new ArrayList<>(2); legs.add(leg1); legs.add(leg2);
        Leg avgLeg = SurveyUpdater.averageLegs(legs);
        Assert.assertEquals(
                "Dist", 15.0, avgLeg.getDistance(), SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(
                "Azm", 0.0, avgLeg.getAzimuth(), SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
        Assert.assertEquals(
                "Inc", 0.0, avgLeg.getInclination(), SexyTopoConstants.ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testBacksights() {
        Leg fore1 = new Leg(10, 180, +42);
        Leg back1 = new Leg(10,   0, -42);
        Assert.assertTrue(
                "Legs should be perfectly-equal backsights",
                SurveyUpdater.areLegsBacksights(fore1, back1));

        Leg back2 = new Leg(15, 90, 0);
        Assert.assertFalse(
                "Legs should not be considered backsights for each other",
                SurveyUpdater.areLegsBacksights(fore1, back2));
    }

}

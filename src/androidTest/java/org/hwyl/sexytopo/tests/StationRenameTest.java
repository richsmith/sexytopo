package org.hwyl.sexytopo.tests;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.ArrayList;
import java.util.List;


public class StationRenameTest extends AndroidTestCase {

    public void testGetStationByNameGetsExistingStation() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();
        Station s1 = testSurvey.getStationByName("1");
        Assert.assertEquals("1", s1.getName());
    }

    public void testGetStationByNameCanFail() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();
        Station shouldBeNull = testSurvey.getStationByName("I DO NOT EXIST :P");
        Assert.assertEquals(null, shouldBeNull);
    }

    public void testRenameOriginStation() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();
        Station s1 = testSurvey.getStationByName("1");
        SurveyUpdater.renameStation(testSurvey, s1, "ShinyNewNameOrigin");

        Station shinyNewStation = testSurvey.getStationByName("ShinyNewNameOrigin");
        Assert.assertEquals("ShinyNewNameOrigin", shinyNewStation.getName());
    }

    public void testRenameStation() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();
        Station s2 = testSurvey.getStationByName("2");
        SurveyUpdater.renameStation(testSurvey, s2, "ShinyNewName");

        Station shinyNewStation = testSurvey.getStationByName("ShinyNewName");
        Assert.assertEquals("ShinyNewName", shinyNewStation.getName());
    }

    //@Test(expected= IllegalArgumentException.class)
    public void testRenamingToExistingNameFails() {
        try {
            Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();
            Station s2 = testSurvey.getStationByName("2");
            SurveyUpdater.renameStation(testSurvey, s2, "1");
            Assert.fail();
        } catch(IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    public void testAverageLegs() {
        Leg leg1 = new Leg(10, 359, -1);
        Leg leg2 = new Leg(20,   1, +1);
        List<Leg> legs = new ArrayList<>(2); legs.add(leg1); legs.add(leg2);
        Leg avgLeg = SurveyUpdater.averageLegs(legs);
        Assert.assertEquals("Dist", 15.0, avgLeg.getDistance());
        Assert.assertEquals("Azm", 0.0, avgLeg.getBearing());
        Assert.assertEquals("Inc", 0.0, avgLeg.getInclination());
    }

}

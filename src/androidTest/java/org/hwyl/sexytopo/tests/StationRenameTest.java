package org.hwyl.sexytopo.tests;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

/**
 * Created by rls on 14/10/15.
 */
public class StationRenameTest extends AndroidTestCase {

    public void testGetStationByNameGetsExistingStation() {
        Survey testSurvey = BasicTestSurveyCreator.createStraight();
        Station s1 = testSurvey.getStationByName("S1");
        Assert.assertEquals("S1", s1.getName());
    }

    public void testGetStationByNameCanFail() {
        Survey testSurvey = BasicTestSurveyCreator.createStraight();
        Station shouldBeNull = testSurvey.getStationByName("I DO NOT EXIST :P");
        Assert.assertEquals(null, shouldBeNull);
    }

    public void testRenameOriginStation() {
        Survey testSurvey = BasicTestSurveyCreator.createStraight();
        Station s1 = testSurvey.getStationByName("S1");
        SurveyUpdater.renameStation(testSurvey, s1, "ShinyNewNameOrigin");

        Station shinyNewStation = testSurvey.getStationByName("ShinyNewNameOrigin");
        Assert.assertEquals("ShinyNewNameOrigin", shinyNewStation.getName());
    }

    public void testRenameStation() {
        Survey testSurvey = BasicTestSurveyCreator.createStraight();
        Station s2 = testSurvey.getStationByName("S2");
        SurveyUpdater.renameStation(testSurvey, s2, "ShinyNewName");

        Station shinyNewStation = testSurvey.getStationByName("ShinyNewName");
        Assert.assertEquals("ShinyNewName", shinyNewStation.getName());
    }

    //@Test(expected= IllegalArgumentException.class)
    public void testRenamingToExistingNameFails() {
        try {
            Survey testSurvey = BasicTestSurveyCreator.createStraight();
            Station s2 = testSurvey.getStationByName("S2");
            SurveyUpdater.renameStation(testSurvey, s2, "S1");
            Assert.fail();
        } catch(IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }
}

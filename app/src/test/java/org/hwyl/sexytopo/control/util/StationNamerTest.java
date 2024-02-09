package org.hwyl.sexytopo.control.util;

import org.junit.Assert;

import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Test;


public class StationNamerTest {

    @Test
    public void testNameAdvancesDigitInStraightLine() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();
        String newName = StationNamer.generateNextStationName(
                testSurvey, testSurvey.getActiveStation());
        int numberOfStations = testSurvey.getAllStations().size();
        Assert.assertEquals(Integer.toString(numberOfStations + 1), newName);
    }


    @Test
    public void testNameAdvancesNumberOnPotentialBranch() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();
        String newName =
                StationNamer.generateNextStationName(testSurvey, testSurvey.getStationByName("1"));
        int numberOfStations = testSurvey.getAllStations().size();
        Assert.assertEquals(Integer.toString(numberOfStations + 1), newName);
    }


    @Test
    public void testNameAdvancesNumberOnEstablishedBranch() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorthWith1EBranch();
        String newName = StationNamer.generateNextStationName(
                testSurvey, testSurvey.getStationByName("5"));
        int numberOfStations = testSurvey.getAllStations().size();
        Assert.assertEquals(Integer.toString(numberOfStations + 1), newName);
    }

    @Test
    public void testNameAdvancesNumberFromMiddleOfBranch() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorthWith2EBranch();
        String newName = StationNamer.generateNextStationName(
                testSurvey, testSurvey.getStationByName("5"));
        int numberOfStations = testSurvey.getAllStations().size();
        Assert.assertEquals(Integer.toString(numberOfStations + 1), newName);
    }
}

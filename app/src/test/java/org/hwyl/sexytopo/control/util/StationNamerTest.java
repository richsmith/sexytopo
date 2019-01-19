package org.hwyl.sexytopo.control.util;

import junit.framework.Assert;

import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testhelpers.BasicTestSurveyCreator;
import org.junit.Test;


public class StationNamerTest {

    @Test
    public void testNameAdvancesDigitInStraightLine() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();
        String newName = StationNamer.generateNextStationName(
                testSurvey, testSurvey.getActiveStation());
        Assert.assertEquals("5", newName);
    }


    @Test
    public void testNameAddsDotSomethingOnBranch() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();
        String newName =
                StationNamer.generateNextStationName(testSurvey, testSurvey.getStationByName("1"));
        Assert.assertEquals("1.1", newName);
    }


    @Test
    public void testNameAdvancesDigitInBranch() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorthWith1EBranch();
        String newName = StationNamer.generateNextStationName(
                testSurvey, testSurvey.getStationByName("1.1"));
        Assert.assertEquals("1.2", newName);
    }

    @Test
    public void testNameAddsDotSomethingOnBranchOffBranch() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorthWith2EBranch();
        String newName = StationNamer.generateNextStationName(
                testSurvey, testSurvey.getStationByName("1.1"));
        Assert.assertEquals("1.1.1", newName);
    }
}

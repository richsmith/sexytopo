package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;

public class SurveyToolsTest {

    @Test
    public void testIsInSubtreeReturnsFalseForNull() {
        Survey survey = new Survey();
        Station station = survey.getOrigin();

        Assert.assertFalse(SurveyTools.isInSubtree(station, null));
        Assert.assertFalse(SurveyTools.isInSubtree(null, station));
        Assert.assertFalse(SurveyTools.isInSubtree(null, null));
    }

    @Test
    public void testIsInSubtreeReturnsTrueForSameStation() {
        Survey survey = new Survey();
        Station station = survey.getOrigin();

        Assert.assertTrue(SurveyTools.isInSubtree(station, station));
    }

    @Test
    public void testIsInSubtreeReturnsTrueForDirectChild() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station origin = survey.getOrigin();
        Station station2 = survey.getStationByName("2");

        Assert.assertTrue(SurveyTools.isInSubtree(origin, station2));
    }

    @Test
    public void testIsInSubtreeReturnsTrueForGrandchild() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station origin = survey.getOrigin();
        Station station3 = survey.getStationByName("3");

        Assert.assertTrue(SurveyTools.isInSubtree(origin, station3));
    }

    @Test
    public void testIsInSubtreeReturnsFalseForAncestor() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station origin = survey.getOrigin();
        Station station2 = survey.getStationByName("2");

        Assert.assertFalse(SurveyTools.isInSubtree(station2, origin));
    }

    @Test
    public void testIsInSubtreeReturnsFalseForUnrelatedStations() {
        Survey survey = BasicTestSurveyCreator.createStraightNorthWith1EBranch();
        Station station2 = survey.getStationByName("2");
        Station station5 = survey.getStationByName("5");

        Assert.assertFalse(SurveyTools.isInSubtree(station5, station2));
        Assert.assertFalse(SurveyTools.isInSubtree(station2, station5));
    }

    @Test
    public void testIsInSubtreeWorksWithBranches() {
        Survey survey = BasicTestSurveyCreator.createStraightNorthWith1EBranch();
        Station station1 = survey.getStationByName("1");
        Station station5 = survey.getStationByName("5");

        Assert.assertTrue(SurveyTools.isInSubtree(station1, station5));
    }

    @Test
    public void testIsInSubtreeReturnsTrueForDeepDescendant() {
        Survey survey = BasicTestSurveyCreator.createStraightNorthWith2EBranch();
        Station origin = survey.getOrigin();
        Station station6 = survey.getStationByName("6");

        Assert.assertTrue(SurveyTools.isInSubtree(origin, station6));
    }
}

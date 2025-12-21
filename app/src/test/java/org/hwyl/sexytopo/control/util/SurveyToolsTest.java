package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;

public class SurveyToolsTest {

    @Test
    public void testIsDescendantOfReturnsFalseForNull() {
        Survey survey = new Survey();
        Station station = survey.getOrigin();

        Assert.assertFalse(SurveyTools.isDescendantOf(null, station));
        Assert.assertFalse(SurveyTools.isDescendantOf(station, null));
        Assert.assertFalse(SurveyTools.isDescendantOf(null, null));
    }

    @Test
    public void testIsDescendantOfReturnsFalseForSameStation() {
        Survey survey = new Survey();
        Station station = survey.getOrigin();

        Assert.assertFalse(SurveyTools.isDescendantOf(station, station));
    }

    @Test
    public void testIsDescendantOfReturnsTrueForDirectChild() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station origin = survey.getOrigin();
        Station station2 = survey.getStationByName("2");

        Assert.assertTrue(SurveyTools.isDescendantOf(station2, origin));
    }

    @Test
    public void testIsDescendantOfReturnsTrueForGrandchild() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station origin = survey.getOrigin();
        Station station3 = survey.getStationByName("3");

        Assert.assertTrue(SurveyTools.isDescendantOf(station3, origin));
    }

    @Test
    public void testIsDescendantOfReturnsFalseForAncestor() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station origin = survey.getOrigin();
        Station station2 = survey.getStationByName("2");

        Assert.assertFalse(SurveyTools.isDescendantOf(origin, station2));
    }

    @Test
    public void testIsDescendantOfReturnsFalseForUnrelatedStations() {
        Survey survey = BasicTestSurveyCreator.createStraightNorthWith1EBranch();
        Station station2 = survey.getStationByName("2");
        Station station5 = survey.getStationByName("5");

        Assert.assertFalse(SurveyTools.isDescendantOf(station2, station5));
        Assert.assertFalse(SurveyTools.isDescendantOf(station5, station2));
    }

    @Test
    public void testIsDescendantOfWorksWithBranches() {
        Survey survey = BasicTestSurveyCreator.createStraightNorthWith1EBranch();
        Station station1 = survey.getStationByName("1");
        Station station5 = survey.getStationByName("5");

        Assert.assertTrue(SurveyTools.isDescendantOf(station5, station1));
    }

    @Test
    public void testIsDescendantOfReturnsTrueForDeepDescendant() {
        Survey survey = BasicTestSurveyCreator.createStraightNorthWith2EBranch();
        Station origin = survey.getOrigin();
        Station station6 = survey.getStationByName("6");

        Assert.assertTrue(SurveyTools.isDescendantOf(station6, origin));
    }
}

package org.hwyl.sexytopo.control.util;

import org.junit.Assert;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Test;


public class SurveyStatsTest {

    @Test
    public void testSurveyComponentCountsFromOrigin() {
        Survey survey = BasicTestSurveyCreator.createStraightNorthWith2EBranchFromS2();
        Assert.assertEquals(6, survey.getAllStations().size());

        Assert.assertEquals(6, SurveyStats.calcNumberSubStations(survey.getOrigin()));
        Assert.assertEquals(5, SurveyStats.calcNumberSubLegs(survey.getOrigin()));
    }

    @Test
    public void testSurveyComponentCountsFromIntermediateStation() {
        Survey survey = BasicTestSurveyCreator.createStraightNorthWith2EBranchFromS2();
        Station subStation = survey.getStationByName("2");
        Assert.assertEquals(5, SurveyStats.calcNumberSubStations(subStation));
        Assert.assertEquals(4, SurveyStats.calcNumberSubLegs(subStation));
    }

    @Test
    public void testLegCountsWithSplays() {
        Survey survey = BasicTestSurveyCreator.createStraightNorthWith2EBranchFromS2();
        Station subStation = survey.getStationByName("2");
        survey.setActiveStation(subStation);
        Leg leg2 = new Leg(5, 0, 0);
        SurveyUpdater.update(survey, leg2);
        Assert.assertEquals(5, SurveyStats.calcNumberSubStations(subStation));
        Assert.assertEquals(5, SurveyStats.calcNumberSubLegs(subStation));
    }
}

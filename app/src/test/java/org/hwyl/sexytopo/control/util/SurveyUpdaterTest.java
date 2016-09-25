package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Survey;
import org.junit.Assert;
import org.junit.Test;

import static org.hwyl.sexytopo.SexyTopo.ALLOWED_DOUBLE_DELTA_FOR_TESTS;


public class SurveyUpdaterTest {

    @Test
    public void testUpdateWithOneLegAddsOneLegToSurvey() {
        Leg leg = new Leg(5, 0, 0);
        Survey survey = new Survey("Test Survey");
        SurveyUpdater.update(survey, leg);
        Assert.assertEquals(survey.getAllLegs().size(), 1);
    }

    @Test
    public void testUpdateWithThreeSimilarLegsLeadsToNewStation() {
        Leg leg = new Leg(5, 0, 0);
        Leg similarLeg = new Leg(5, 0.001, 0);
        Leg anotherSimilarLeg = new Leg(5, 0, 0.001);
        Survey survey = new Survey("Test Survey");
        SurveyUpdater.update(survey, leg);
        SurveyUpdater.update(survey, similarLeg);
        SurveyUpdater.update(survey, anotherSimilarLeg);
        Assert.assertEquals(survey.getAllStations().size(), 2);
    }

    @Test
    public void testEditLegWorks() {
        Leg leg = new Leg(5, 0, 0);
        Survey survey = new Survey("Test Survey");
        SurveyUpdater.update(survey, leg);

        Leg newEdit = new Leg(6, 0, 0);
        SurveyUpdater.editLeg(survey, leg, newEdit);

        Assert.assertEquals(survey.getAllLegs().size(), 1);
        Assert.assertEquals(survey.getAllLegs().get(0).getDistance(), 6,
                ALLOWED_DOUBLE_DELTA_FOR_TESTS);
    }

    @Test
    public void testEditStationWorks() {
        Leg leg = new Leg(5, 0, 0);
        Survey survey = new Survey("Test Survey");
        SurveyUpdater.update(survey, leg);

        Leg newEdit = new Leg(6, 0, 0);
        SurveyUpdater.editLeg(survey, leg, newEdit);

        Assert.assertEquals(survey.getAllLegs().size(), 1);
        Assert.assertEquals(survey.getAllLegs().get(0).getDistance(), 6,
                ALLOWED_DOUBLE_DELTA_FOR_TESTS);
    }

}

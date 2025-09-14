package org.hwyl.sexytopo.control.util;

import static org.hwyl.sexytopo.SexyTopoConstants.ALLOWED_DOUBLE_DELTA;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;


public class SurveyUpdaterTest {

    @Test
    public void testUpdateWithOneLegAddsOneLegToSurvey() {
        Leg leg = new Leg(5, 0, 0);
        Survey survey = new Survey();
        SurveyUpdater.update(survey, leg);
        Assert.assertEquals(survey.getAllLegs().size(), 1);
    }

    @Test
    public void testUpdateWithThreeSimilarLegsLeadsToNewStation() {
        Leg leg = new Leg(5, 0, 0);
        Leg similarLeg = new Leg(5, 0.001f, 0);
        Leg anotherSimilarLeg = new Leg(5, 0, 0.001f);
        Survey survey = new Survey();
        SurveyUpdater.update(survey, leg);
        SurveyUpdater.update(survey, similarLeg);
        SurveyUpdater.update(survey, anotherSimilarLeg);
        Assert.assertEquals(survey.getAllStations().size(), 2);
    }

    @Test
    public void testEditLegWorks() {
        Leg leg = new Leg(5, 0, 0);
        Survey survey = new Survey();
        SurveyUpdater.update(survey, leg);

        Leg newEdit = new Leg(6, 0, 0);
        SurveyUpdater.editLeg(survey, leg, newEdit);

        Assert.assertEquals(survey.getAllLegs().size(), 1);
        Assert.assertEquals(survey.getAllLegs().get(0).getDistance(), 6,
                ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testEditStationWorks() {
        Leg leg = new Leg(5, 0, 0);
        Survey survey = new Survey();
        SurveyUpdater.update(survey, leg);

        Leg newEdit = new Leg(6, 0, 0);
        SurveyUpdater.editLeg(survey, leg, newEdit);

        Assert.assertEquals(survey.getAllLegs().size(), 1);
        Assert.assertEquals(survey.getAllLegs().get(0).getDistance(), 6,
                ALLOWED_DOUBLE_DELTA);
    }

    @Test
    public void testMoveLegWorks() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Leg toMove = survey.getStationByName("2").getOnwardLegs().get(0);
        Station originatingStation = survey.getOriginatingStation(toMove);
        Station destinationStation = survey.getStationByName("1");
        Assert.assertNotEquals(originatingStation, destinationStation);
        SurveyUpdater.moveLeg(survey, toMove, destinationStation);
        Assert.assertTrue(destinationStation.getOnwardLegs().contains(toMove));
        Assert.assertFalse(originatingStation.getOnwardLegs().contains(toMove));
    }

    @Test
    public void testAreLegsAboutTheSame() {
        Assert.assertTrue(SurveyUpdater.areLegsAboutTheSame(Arrays.asList(
                new Leg(10, 159.5f, 0),
                new Leg(10, 160.0f, 0),
                new Leg(10, 160.5f, 0)
        )));
        Assert.assertFalse(SurveyUpdater.areLegsAboutTheSame(Arrays.asList(
                new Leg(10, 119.5f, 0),
                new Leg(10, 110.0f, 0),
                new Leg(10, 110.5f, 0)
        )));
        Assert.assertFalse(SurveyUpdater.areLegsAboutTheSame(Arrays.asList(
                new Leg(10, 349.5f, 0),
                new Leg(10, 10.0f, 0),
                new Leg(10, 10.5f, 0)
        )));
        Assert.assertTrue(SurveyUpdater.areLegsAboutTheSame(Arrays.asList(
                new Leg(10, 359.5f, 0),
                new Leg(10, 0.0f, 0),
                new Leg(10, 0.5f, 0)
        )));
    }

}

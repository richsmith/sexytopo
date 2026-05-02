package org.hwyl.sexytopo.control.io.basic;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.hwyl.sexytopo.testutils.ExampleSurveyCreator;
import org.hwyl.sexytopo.testutils.SurveyChecker;
import org.junit.Assert;
import org.junit.Test;

public class SurveyJsonTranslaterTest {

    @Test
    public void testEmptySurveyResultsIn1Station() throws Exception {
        Survey survey = new Survey();
        String text = SurveyJsonTranslater.toText(survey, "test", 0);

        Survey newSurvey = new Survey();
        SurveyJsonTranslater.populateSurvey(survey, text);
        assert newSurvey.getAllStations().size() == 1;
    }

    @Test
    public void testSimpleSurveyIsParsed() throws Exception {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        String text = SurveyJsonTranslater.toText(survey, "test", 0);

        Survey newSurvey = new Survey();
        SurveyJsonTranslater.populateSurvey(survey, text);

        SurveyChecker.areEqual(survey, newSurvey);
    }

    @Test
    public void testSlightlyBiggerSurveyIsParsed() throws Exception {
        Survey survey = BasicTestSurveyCreator.createRightRight();
        String text = SurveyJsonTranslater.toText(survey, "test", 0);

        Survey newSurvey = new Survey();
        SurveyJsonTranslater.populateSurvey(survey, text);

        SurveyChecker.areEqual(survey, newSurvey);
    }

    @Test
    public void testRandomSurveyIsParsed() throws Exception {
        Survey survey = ExampleSurveyCreator.create(10, 10);
        String text = SurveyJsonTranslater.toText(survey, "test", 0);

        Survey newSurvey = new Survey();
        SurveyJsonTranslater.populateSurvey(survey, text);

        SurveyChecker.areEqual(survey, newSurvey);
    }

    @Test
    public void testSurveyWithTripsAreParsed() throws Exception {
        Survey survey = BasicTestSurveyCreator.createStraightNorthWithTrip();
        String text = SurveyJsonTranslater.toText(survey, "test", 0);

        Survey newSurvey = new Survey();
        SurveyJsonTranslater.populateSurvey(survey, text);

        SurveyChecker.areEqual(survey, newSurvey);
    }

    @Test
    public void testCrossedOutLegIsPreservedInRoundTrip() throws Exception {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Leg legToCrossOut = survey.getAllLegs().get(0);
        legToCrossOut.setCrossedOut(true);

        String text = SurveyJsonTranslater.toText(survey, "test", 0);

        Survey reloaded = new Survey();
        SurveyJsonTranslater.populateSurvey(reloaded, text);

        Leg reloadedLeg = reloaded.getAllLegs().get(0);
        Assert.assertTrue(reloadedLeg.isCrossedOut());
    }

    @Test
    public void testUncrossedLegIsPreservedInRoundTrip() throws Exception {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        // All legs default to not crossed out
        String text = SurveyJsonTranslater.toText(survey, "test", 0);

        Survey reloaded = new Survey();
        SurveyJsonTranslater.populateSurvey(reloaded, text);

        for (Leg leg : reloaded.getAllLegs()) {
            Assert.assertFalse(leg.isCrossedOut());
        }
    }

    @Test
    public void testLegMissingIsCrossedOutFieldDefaultsFalse() throws Exception {
        // Simulates loading an old file that predates the isCrossedOut field
        String oldJson = "{\"versionName\":\"test\",\"versionCode\":0,\"name\":\"Unsaved Survey\","
                + "\"stations\":[{\"name\":\"0\",\"eeDirection\":\"right\",\"comment\":\"\","
                + "\"legs\":[{\"distance\":5.0,\"azimuth\":0.0,\"inclination\":0.0,"
                + "\"destination\":\"1\",\"wasShotBackwards\":false,\"index\":0,"
                + "\"promotedFrom\":[]}]},"
                + "{\"name\":\"1\",\"eeDirection\":\"right\",\"comment\":\"\",\"legs\":[]}]}";

        Survey survey = new Survey();
        SurveyJsonTranslater.populateSurvey(survey, oldJson);

        for (Leg leg : survey.getAllLegs()) {
            Assert.assertFalse(leg.isCrossedOut());
        }
    }
}

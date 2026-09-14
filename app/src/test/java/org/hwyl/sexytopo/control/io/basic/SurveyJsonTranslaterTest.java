package org.hwyl.sexytopo.control.io.basic;

import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.hwyl.sexytopo.testutils.ExampleSurveyCreator;
import org.hwyl.sexytopo.testutils.SurveyChecker;
import org.json.JSONObject;
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
    public void testTripInstrumentRoundTrip() throws Exception {
        Trip trip = new Trip();
        trip.setInstrument("DistoX BLE");

        JSONObject json = SurveyJsonTranslater.toJson(trip);
        Trip loaded = SurveyJsonTranslater.toTrip(json);

        Assert.assertEquals("DistoX BLE", loaded.getInstrument());
        Assert.assertTrue(loaded.hasInstrument());
    }

    @Test
    public void testTripCopyrightAndLicenceRoundTrip() throws Exception {
        Trip trip = new Trip();
        trip.setCopyrightHolder("Jane Caver");
        trip.setLicence("CC BY 4.0");

        JSONObject json = SurveyJsonTranslater.toJson(trip);
        Trip loaded = SurveyJsonTranslater.toTrip(json);

        Assert.assertEquals("Jane Caver", loaded.getCopyrightHolder());
        Assert.assertEquals("CC BY 4.0", loaded.getLicence());
        Assert.assertTrue(loaded.hasCopyrightHolder());
        Assert.assertTrue(loaded.hasLicence());
    }

    @Test
    public void testOldTripJsonWithoutCopyrightOrLicenceDefaultsToEmptyStrings() throws Exception {
        Trip trip = new Trip();
        trip.setInstrument("DistoX BLE");

        JSONObject json = SurveyJsonTranslater.toJson(trip);
        json.remove(SurveyJsonTranslater.COPYRIGHT_HOLDER_TAG);
        json.remove(SurveyJsonTranslater.LICENCE_TAG);

        Trip loaded = SurveyJsonTranslater.toTrip(json);

        Assert.assertEquals("", loaded.getCopyrightHolder());
        Assert.assertEquals("", loaded.getLicence());
        Assert.assertFalse(loaded.hasCopyrightHolder());
        Assert.assertFalse(loaded.hasLicence());
    }
}

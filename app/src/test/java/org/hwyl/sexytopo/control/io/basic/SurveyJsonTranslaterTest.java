package org.hwyl.sexytopo.control.io.basic;

import java.util.HashMap;
import java.util.Map;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
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

    @Test
    public void testSplayStoredAsShotBackwardsIsLoadedAsForwardSplayWithSameNumbers()
            throws Exception {
        // Older files can hold a splay with the flag set, but a splay can never be shot backwards
        Leg flaggedSplay = new Leg(5.5f, 123.0f, -12.0f, true);
        JSONObject json = SurveyJsonTranslater.toJson(flaggedSplay, 0);
        Assert.assertTrue(json.getBoolean(SurveyJsonTranslater.WAS_SHOT_BACKWARDS_TAG));

        Leg loaded = SurveyJsonTranslater.toLeg(new HashMap<>(), json);

        Assert.assertFalse(loaded.hasDestination());
        Assert.assertFalse(loaded.wasShotBackwards());
        Assert.assertEquals(5.5f, loaded.getDistance(), 0.0001f);
        Assert.assertEquals(123.0f, loaded.getAzimuth(), 0.0001f);
        Assert.assertEquals(-12.0f, loaded.getInclination(), 0.0001f);
    }

    @Test
    public void testBackwardPromotedLegRoundTripsWithReadingsAndFlag() throws Exception {
        Station destination = new Station("2");
        Leg[] readings =
                new Leg[] {
                    new Leg(5.0f, 270.0f, 10.0f),
                    new Leg(5.0f, 270.0f, 10.0f),
                    new Leg(6.0f, 272.0f, 12.0f)
                };
        Leg backwardLeg = new Leg(5.3333f, 90.6667f, -10.6667f, destination, readings, true);
        Map<String, Station> namesToStations = new HashMap<>();
        namesToStations.put("2", destination);

        JSONObject json = SurveyJsonTranslater.toJson(backwardLeg, 0);
        Leg loaded = SurveyJsonTranslater.toLeg(namesToStations, json);

        Assert.assertTrue(loaded.wasShotBackwards());
        Assert.assertEquals(5.3333f, loaded.getDistance(), 0.0001f);
        Assert.assertEquals(90.6667f, loaded.getAzimuth(), 0.0001f);
        Assert.assertEquals(-10.6667f, loaded.getInclination(), 0.0001f);
        Leg[] loadedReadings = loaded.getPromotedFrom();
        Assert.assertEquals(3, loadedReadings.length);
        for (int i = 0; i < readings.length; i++) {
            Assert.assertFalse(loadedReadings[i].hasDestination());
            Assert.assertFalse(loadedReadings[i].wasShotBackwards());
            Assert.assertEquals(readings[i].getAzimuth(), loadedReadings[i].getAzimuth(), 0.0001f);
        }
    }

    @Test
    public void testSplayDestinationIsADashForAWallAndADotForAnotherFeature() throws Exception {
        Leg toWall = new Leg(5.0f, 45.0f, 10.0f);
        Leg toFeature = toWall.withToWall(false);

        JSONObject wallJson = SurveyJsonTranslater.toJson(toWall, 0);
        JSONObject featureJson = SurveyJsonTranslater.toJson(toFeature, 0);

        Assert.assertEquals("-", wallJson.getString(SurveyJsonTranslater.DESTINATION_TAG));
        Assert.assertEquals(".", featureJson.getString(SurveyJsonTranslater.DESTINATION_TAG));
        Assert.assertTrue(SurveyJsonTranslater.toLeg(new HashMap<>(), wallJson).isToWall());
        Leg loaded = SurveyJsonTranslater.toLeg(new HashMap<>(), featureJson);
        Assert.assertFalse(loaded.hasDestination());
        Assert.assertFalse(loaded.isToWall());
        Assert.assertEquals(5.0f, loaded.getDistance(), 0.0001f);
    }

    @Test
    public void testPromotedReadingsKeepWhatTheyGoTo() throws Exception {
        Station destination = new Station("2");
        Leg[] readings = {
            new Leg(5.0f, 270.0f, 10.0f),
            new Leg(5.0f, 270.0f, 10.0f).withToWall(false),
            new Leg(5.0f, 270.0f, 10.0f)
        };
        Leg leg = new Leg(5.0f, 270.0f, 10.0f, destination, readings);
        Map<String, Station> namesToStations = new HashMap<>();
        namesToStations.put("2", destination);

        Leg loaded =
                SurveyJsonTranslater.toLeg(namesToStations, SurveyJsonTranslater.toJson(leg, 0));

        Assert.assertEquals(3, loaded.getPromotedFrom().length);
        Assert.assertTrue(loaded.getPromotedFrom()[0].isToWall());
        Assert.assertFalse(loaded.getPromotedFrom()[1].isToWall());
        Assert.assertTrue(loaded.getPromotedFrom()[2].isToWall());
    }
}

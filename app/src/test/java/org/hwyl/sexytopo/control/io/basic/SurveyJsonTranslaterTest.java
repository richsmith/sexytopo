package org.hwyl.sexytopo.control.io.basic;

import java.util.HashMap;
import java.util.Map;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
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

    private static Leg hiddenSplayWithComment() {
        Leg splay = new Leg(2.5f, 90.0f, 10.0f);
        splay.setComment("Boulder");
        splay.setHidden(true);
        return splay;
    }

    @Test
    public void testHiddenSplayIsWrittenAsHidden() throws Exception {
        JSONObject json = SurveyJsonTranslater.toJson(hiddenSplayWithComment(), null);
        Assert.assertTrue(json.getBoolean(SurveyJsonTranslater.HIDDEN_TAG));
    }

    @Test
    public void testVisibleSplayHasNoHiddenKey() throws Exception {
        JSONObject json = SurveyJsonTranslater.toJson(new Leg(2.5f, 90.0f, 10.0f), null);
        Assert.assertFalse(json.has(SurveyJsonTranslater.HIDDEN_TAG));
    }

    @Test
    public void testFullLegHasNoHiddenKey() throws Exception {
        Leg leg = new Leg(2.5f, 90.0f, 10.0f, new Station("1"), new Leg[] {});
        JSONObject json = SurveyJsonTranslater.toJson(leg, null);
        Assert.assertFalse(json.has(SurveyJsonTranslater.HIDDEN_TAG));
    }

    @Test
    public void testHiddenSplayAndCommentRoundTrip() throws Exception {
        JSONObject json = SurveyJsonTranslater.toJson(hiddenSplayWithComment(), null);

        Leg loaded = SurveyJsonTranslater.toLeg(new HashMap<>(), json);

        Assert.assertTrue(loaded.isHidden());
        Assert.assertEquals("Boulder", loaded.getComment());
        Assert.assertEquals(2.5f, loaded.getDistance(), 0.0001f);
    }

    @Test
    public void testLegWithoutHiddenKeyLoadsAsVisible() throws Exception {
        JSONObject json = SurveyJsonTranslater.toJson(hiddenSplayWithComment(), null);
        json.remove(SurveyJsonTranslater.HIDDEN_TAG);

        Leg loaded = SurveyJsonTranslater.toLeg(new HashMap<>(), json);

        Assert.assertFalse(loaded.isHidden());
    }

    @Test
    public void testHiddenKeyOnFullLegIsIgnored() throws Exception {
        Station destination = new Station("1");
        Leg leg = new Leg(2.5f, 90.0f, 10.0f, destination, new Leg[] {});
        JSONObject json = SurveyJsonTranslater.toJson(leg, null);
        json.put(SurveyJsonTranslater.HIDDEN_TAG, true);
        Map<String, Station> stations = new HashMap<>();
        stations.put("1", destination);

        Leg loaded = SurveyJsonTranslater.toLeg(stations, json);

        Assert.assertFalse(loaded.isHidden());
    }

    @Test
    public void testSurveyWithHiddenSplayRoundTrips() throws Exception {
        Survey survey = new Survey();
        Leg splay = new Leg(2.5f, 90.0f, 10.0f);
        splay.setComment("Boulder");
        SurveyUpdater.update(survey, splay);
        SurveyUpdater.setSplayHidden(survey, splay, true);
        String text = SurveyJsonTranslater.toText(survey, "test", 0);

        Survey loaded = new Survey();
        SurveyJsonTranslater.populateSurvey(loaded, text);

        Leg loadedSplay = loaded.getOrigin().getOnwardLegs().get(0);
        Assert.assertTrue(loadedSplay.isHidden());
        Assert.assertEquals("Boulder", loadedSplay.getComment());
    }
}

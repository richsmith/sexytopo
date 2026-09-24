package org.hwyl.sexytopo.control.io.thirdparty.survex;

import java.util.Arrays;
import java.util.List;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionImporter;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionUtil;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurveyFormat;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;

public class SurvexImporterTest {

    @Test
    public void testBasicImport() throws Exception {
        final String testContent = "1\t2\t5.0\t0.0\t0.0";
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline(testContent, survey);
        Assert.assertEquals(2, survey.getAllStations().size());
    }

    @Test
    public void testBasicImportRecordsPromotions() throws Exception {
        final String testContent =
                "1\t2\t5.0\t0.0\t0.0\t; {from: 5.0 0.0 0.0, 5.0 0.0 0.0, 5.0 0.0 0.0}";
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline(testContent, survey);
        Leg leg = survey.getOrigin().getConnectedOnwardLegs().get(0);
        Assert.assertEquals(3, leg.getPromotedFrom().length);
    }

    @Test
    public void testBasicImportHandlesComments() throws Exception {
        final String testContent =
                "1\t2\t5.0\t0.0\t0.0\t; {from: 5.0 0.0 0.0, 5.0 0.0 0.0, 5.0 0.0 0.0} testComment";
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline(testContent, survey);
        Station created = survey.getStationByName("2");
        Assert.assertEquals("testComment", created.getComment());
    }

    // --- Leg comment tests ---

    @Test
    public void testLegCommentWithSemicolonAppliedToToStation() throws Exception {
        // Survex style: comment character present
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline("1\t2\t5.0\t0.0\t0.0\t; My Chamber", survey);
        Assert.assertEquals("My Chamber", survey.getStationByName("2").getComment());
    }

    @Test
    public void testLegCommentWithHashAppliedToToStation() throws Exception {
        // Therion style: hash comment character
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline("1\t2\t5.0\t0.0\t0.0\t# My Chamber", survey);
        Assert.assertEquals("My Chamber", survey.getStationByName("2").getComment());
    }

    @Test
    public void testLegCommentWithoutCommentCharAppliedToToStation() throws Exception {
        // Bare comment with no comment character — the bug case
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline("1\t2\t5.0\t0.0\t0.0\tMy Chamber", survey);
        Assert.assertEquals("My Chamber", survey.getStationByName("2").getComment());
    }

    @Test
    public void testLegCommentMultiWordBareAppliedToToStation() throws Exception {
        // Multi-word bare comment
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline("1\t2\t3.34\t34.0\t-4.0\tBig Sandy Chamber", survey);
        Assert.assertEquals("Big Sandy Chamber", survey.getStationByName("2").getComment());
    }

    @Test
    public void testLegWithNoCommentLeavesStationCommentEmpty() throws Exception {
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline("1\t2\t5.0\t0.0\t0.0", survey);
        Station s = survey.getStationByName("2");
        Assert.assertTrue(s.getComment() == null || s.getComment().isEmpty());
    }

    @Test
    public void testPassageAndLegCommentsAreConcatenated() throws Exception {
        // Leg comment sets station comment, then passage comment is merged on top
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline("1\t2\t5.0\t0.0\t0.0\tThe Squeeze", survey);

        java.util.Map<String, String> passageComments = new java.util.HashMap<>();
        passageComments.put("2", "Tight rift");
        SurvexTherionImporter.mergePassageComments(survey, passageComments);

        // passage comment first, then leg comment
        Assert.assertEquals("Tight rift :: The Squeeze", survey.getStationByName("2").getComment());
    }

    // --- Version-gated leg comment routing ---

    @Test
    public void testLegCommentGoesToLegWhenVersionAfterCutoff() throws Exception {
        // 1.11.3 → useLegComments = true → comment on the leg, not the station
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline(
                "1\t2\t5.0\t0.0\t0.0\tMy Chamber", survey, /* useLegComments= */ true);
        Leg leg = survey.getOrigin().getConnectedOnwardLegs().get(0);
        Assert.assertEquals("My Chamber", leg.getComment());
        Assert.assertTrue(
                survey.getStationByName("2").getComment() == null
                        || survey.getStationByName("2").getComment().isEmpty());
    }

    @Test
    public void testLegCommentGoesToStationWhenVersionAtCutoff() throws Exception {
        // 1.11.2 → useLegComments = false → legacy station-comment path
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline(
                "1\t2\t5.0\t0.0\t0.0\tMy Chamber", survey, /* useLegComments= */ false);
        Assert.assertEquals("My Chamber", survey.getStationByName("2").getComment());
        Leg leg = survey.getOrigin().getConnectedOnwardLegs().get(0);
        Assert.assertTrue(leg.getComment() == null || leg.getComment().isEmpty());
    }

    @Test
    public void testLegCommentGoesToLegWhenNoVersionHeader() throws Exception {
        // No SexyTopo header → useLegComments = true (same as new format)
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline(
                "1\t2\t5.0\t0.0\t0.0\tMy Chamber", survey, /* useLegComments= */ true);
        Leg leg = survey.getOrigin().getConnectedOnwardLegs().get(0);
        Assert.assertEquals("My Chamber", leg.getComment());
    }

    @Test
    public void testPromotedLegPrecursorCommentImportedOnLeg() throws Exception {
        // In new format, a trailing comment on a promoted-leg precursor line goes on the Leg
        final String text =
                "1\t2\t5.541\t253.93\t4.67\n" + "; 1\t2\t5.542\t73.95\t-4.64\tBack sight\n";
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline(text, survey, /* useLegComments= */ true);
        Leg mainLeg = survey.getOrigin().getConnectedOnwardLegs().get(0);
        Assert.assertEquals(1, mainLeg.getPromotedFrom().length);
        Assert.assertEquals("Back sight", mainLeg.getPromotedFrom()[0].getComment());
    }

    @Test
    public void testPromotedLegPrecursorCommentIgnoredInLegacyMode() throws Exception {
        // In legacy mode, the precursor comment is not captured
        final String text =
                "1\t2\t5.541\t253.93\t4.67\n" + "; 1\t2\t5.542\t73.95\t-4.64\tBack sight\n";
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline(text, survey, /* useLegComments= */ false);
        Leg mainLeg = survey.getOrigin().getConnectedOnwardLegs().get(0);
        Assert.assertEquals(1, mainLeg.getPromotedFrom().length);
        Assert.assertTrue(
                mainLeg.getPromotedFrom()[0].getComment() == null
                        || mainLeg.getPromotedFrom()[0].getComment().isEmpty());
    }

    // --- Splay token recognition (Survex anonymous-station conventions) ---

    @Test
    public void testDoubleDotSplayImportsAsSplayNotStation() throws Exception {
        // ".." is Survex's own anonymous-wall-point convention, and the one SexyTopo itself
        // writes for Survex export - this is the case the old "-"-only check silently broke.
        // A trailing real leg is needed so station 1 becomes reachable from the origin -
        // origin is only ever set from a non-splay leg.
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline("1\t..\t1.0\t0.0\t0.0\n1\t2\t5.0\t0.0\t0.0", survey);

        Assert.assertNull(survey.getStationByName(".."));
        Assert.assertEquals(1, survey.getOrigin().getUnconnectedOnwardLegs().size());
    }

    @Test
    public void testSingleDotSplayImportsAsSplayNotStation() throws Exception {
        // "." is Survex's anonymous non-wall-point convention (and also a valid Therion token)
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline("1\t.\t1.0\t0.0\t0.0\n1\t2\t5.0\t0.0\t0.0", survey);

        Assert.assertNull(survey.getStationByName("."));
        Assert.assertEquals(1, survey.getOrigin().getUnconnectedOnwardLegs().size());
    }

    @Test
    public void testTripleDotSplayImportsAsSplayNotStation() throws Exception {
        // "..." is Survex's anonymous point with no implicit flags
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline("1\t...\t1.0\t0.0\t0.0\n1\t2\t5.0\t0.0\t0.0", survey);

        Assert.assertNull(survey.getStationByName("..."));
        Assert.assertEquals(1, survey.getOrigin().getUnconnectedOnwardLegs().size());
    }

    @Test
    public void testConsecutiveAnonymousSplaysStayAsSeparateSplays() throws Exception {
        // Several splays sharing the same anonymous token must not be confused with each
        // other or merged into a single named station
        final String text =
                "1\t..\t1.0\t0.0\t0.0\n"
                        + "1\t..\t1.2\t90.0\t0.0\n"
                        + "1\t..\t0.8\t180.0\t0.0\n"
                        + "1\t2\t5.0\t0.0\t0.0\n";
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline(text, survey);

        Assert.assertEquals(2, survey.getAllStations().size()); // origin "1" and "2"
        Assert.assertEquals(3, survey.getOrigin().getUnconnectedOnwardLegs().size());
    }

    @Test
    public void testHyphenSplayStillImportsAsSplay() throws Exception {
        // Regression: the original "-" convention (Therion's, and the internal blank-station
        // sentinel) must keep working
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline("1\t-\t1.0\t0.0\t0.0\n1\t2\t5.0\t0.0\t0.0", survey);

        Assert.assertNull(survey.getStationByName("-"));
        Assert.assertEquals(1, survey.getOrigin().getUnconnectedOnwardLegs().size());
    }

    // --- Repeated real lines (Survex-style averaging on import) ---

    @Test
    public void testRepeatedRealLinesAreAveragedIntoOnePromotedLeg() throws Exception {
        final String text =
                "1\t2\t5.001\t0.02\t0.01\n"
                        + "1\t2\t4.999\t359.98\t-0.01\n"
                        + "1\t2\t5.000\t0.00\t0.02\n";
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline(text, survey, true);

        Assert.assertEquals(2, survey.getAllStations().size());
        Leg leg = survey.getOrigin().getConnectedOnwardLegs().get(0);
        Assert.assertEquals(3, leg.getPromotedFrom().length);
    }

    @Test
    public void testRepeatedRealLinesMatchDirectAveraging() throws Exception {
        List<Leg> raw =
                Arrays.asList(
                        new Leg(5.001f, 0.02f, 0.01f),
                        new Leg(4.999f, 359.98f, -0.01f),
                        new Leg(5.000f, 0.00f, 0.02f));
        Leg expected = SurveyUpdater.averageLegs(raw);

        final String text =
                "1\t2\t5.001\t0.02\t0.01\n"
                        + "1\t2\t4.999\t359.98\t-0.01\n"
                        + "1\t2\t5.000\t0.00\t0.02\n";
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline(text, survey, true);
        Leg leg = survey.getOrigin().getConnectedOnwardLegs().get(0);

        Assert.assertEquals(expected.getDistance(), leg.getDistance(), 0.001);
        Assert.assertEquals(expected.getAzimuth(), leg.getAzimuth(), 0.001);
        Assert.assertEquals(expected.getInclination(), leg.getInclination(), 0.001);
    }

    @Test
    public void testSingleLineIsNotTreatedAsAGroup() throws Exception {
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline("1\t2\t5.0\t0.0\t0.0", survey);
        Leg leg = survey.getOrigin().getConnectedOnwardLegs().get(0);
        Assert.assertFalse(leg.wasPromoted());
    }

    @Test
    public void testGroupCommentGoesOnLegOwnComment() throws Exception {
        final String text =
                "1\t2\t5.001\t0.02\t0.01\tBig Sandy Chamber\n" + "1\t2\t4.999\t359.98\t-0.01\n";
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline(text, survey, true);
        Leg leg = survey.getOrigin().getConnectedOnwardLegs().get(0);
        Assert.assertEquals("Big Sandy Chamber", leg.getComment());
    }

    @Test
    public void testSecondLineCommentGoesOnThatRawReadingNotTheLeg() throws Exception {
        final String text =
                "1\t2\t5.001\t0.02\t0.01\n" + "1\t2\t4.999\t359.98\t-0.01\tcold draught\n";
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline(text, survey, true);
        Leg leg = survey.getOrigin().getConnectedOnwardLegs().get(0);

        Assert.assertTrue(leg.getComment() == null || leg.getComment().isEmpty());
        Assert.assertEquals("cold draught", leg.getPromotedFrom()[1].getComment());
    }

    @Test
    public void testGroupCommentGoesToStationInLegacyMode() throws Exception {
        final String text =
                "1\t2\t5.001\t0.02\t0.01\tBig Sandy Chamber\n" + "1\t2\t4.999\t359.98\t-0.01\n";
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline(text, survey, false);

        Assert.assertEquals("Big Sandy Chamber", survey.getStationByName("2").getComment());
        Leg leg = survey.getOrigin().getConnectedOnwardLegs().get(0);
        Assert.assertTrue(leg.getComment() == null || leg.getComment().isEmpty());
        Assert.assertEquals(2, leg.getPromotedFrom().length);
    }

    @Test
    public void testSecondLineCommentIgnoredInLegacyMode() throws Exception {
        final String text =
                "1\t2\t5.001\t0.02\t0.01\n" + "1\t2\t4.999\t359.98\t-0.01\tcold draught\n";
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline(text, survey, false);

        Leg leg = survey.getOrigin().getConnectedOnwardLegs().get(0);
        Assert.assertEquals(2, leg.getPromotedFrom().length);
        Assert.assertTrue(
                leg.getPromotedFrom()[1].getComment() == null
                        || leg.getPromotedFrom()[1].getComment().isEmpty());
    }

    @Test
    public void testRepeatedRealLinesGroupingRespectsBackwardLegDirection() throws Exception {
        final String text =
                "1\t2\t5.0\t0.0\t0.0\n" // establishes stations 1 and 2
                        + "3\t2\t5.001\t180.02\t-0.01\n" // backward: 3 is new, 2 already exists
                        + "3\t2\t4.999\t179.98\t0.01\n";
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline(text, survey, true);

        Station two = survey.getStationByName("2");
        Assert.assertEquals(1, two.getConnectedOnwardLegs().size());
        Leg backwardLeg = two.getConnectedOnwardLegs().get(0);
        Assert.assertEquals(2, backwardLeg.getPromotedFrom().length);
    }

    @Test
    public void testLegsAfterARepeatedGroupAreStillParsedCorrectly() throws Exception {
        final String text =
                "1\t2\t5.001\t0.02\t0.01\n"
                        + "1\t2\t4.999\t359.98\t-0.01\n"
                        + "2\t3\t5.0\t0.0\t0.0\n";
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline(text, survey, true);

        Assert.assertEquals(3, survey.getAllStations().size());
        Assert.assertNotNull(survey.getStationByName("3"));
    }

    @Test
    public void testSurvexPromotedLegRoundTrips() throws Exception {
        Survey original = BasicTestSurveyCreator.createStraightNorthThroughRepeats();
        String centreline = SurvexTherionUtil.getCentrelineData(original, SurveyFormat.SURVEX);

        Survey reimported = new Survey();
        SurvexTherionImporter.parseCentreline(centreline, reimported, true);

        Leg leg = reimported.getOrigin().getConnectedOnwardLegs().get(0);
        Assert.assertEquals(3, leg.getPromotedFrom().length);
        Assert.assertEquals(5.0f, leg.getDistance(), 0.001);
    }

    @Test
    public void testSurvexPromotedLegRoundTripIsLossy() throws Exception {
        // The .svx holds only raw readings, so two things change on a round trip: the leg is
        // re-averaged with the current algorithm, and the leg's comment and the first reading's
        // comment share one line, so come back merged onto the leg
        Leg reading1 = new Leg(5.1f, 1.0f, 2.0f);
        reading1.setComment("cold draught");
        Leg reading2 = new Leg(4.9f, 359.0f, -1.0f);
        Leg reading3 = new Leg(5.0f, 0.5f, 0.0f);
        Leg[] readings = {reading1, reading2, reading3};

        Survey original = new Survey();
        Leg promoted =
                Leg.upgradeSplayToConnectedLeg(
                        new Leg(5.0f, 0.0f, 0.0f), new Station("2"), readings);
        promoted.setComment("Big Sandy Chamber");
        original.getOrigin().addOnwardLeg(promoted);

        String centreline = SurvexTherionUtil.getCentrelineData(original, SurveyFormat.SURVEX);
        Survey reimported = new Survey();
        SurvexTherionImporter.parseCentreline(centreline, reimported, true);
        Leg leg = reimported.getOrigin().getConnectedOnwardLegs().get(0);

        Leg expected = SurveyUpdater.averageLegs(Arrays.asList(readings));
        Assert.assertEquals(expected.getDistance(), leg.getDistance(), 0.001);
        Assert.assertEquals(expected.getAzimuth(), leg.getAzimuth(), 0.001);
        Assert.assertEquals(expected.getInclination(), leg.getInclination(), 0.001);

        Assert.assertEquals("Big Sandy Chamber :: cold draught", leg.getComment());
        Assert.assertFalse(leg.getPromotedFrom()[0].hasComment());
    }

    @Test
    public void testBadLineAfterLegIsReportedAgainstItself() {
        final String text = "1\t2\t5.0\t0.0\t0.0\n" + "1\t2\t5.0\tnorth\t0.0\n";
        try {
            SurvexTherionImporter.parseCentreline(text, new Survey(), true);
            Assert.fail("Expected an import error");
        } catch (Exception exception) {
            Assert.assertTrue(exception.getMessage().endsWith("1\t2\t5.0\tnorth\t0.0"));
        }
    }

    // --- Metadata date parsing ---

    @Test
    public void testMetadataImportAcceptsHyphenSeparatedSurveyDate() throws Exception {
        String survexText = "*date 2026-01-05\n";
        Trip trip = SurvexTherionImporter.parseMetadata(survexText, SurveyFormat.SURVEX);
        Assert.assertNotNull(trip);
        Assert.assertNotNull(trip.getSurveyDate());
    }
}

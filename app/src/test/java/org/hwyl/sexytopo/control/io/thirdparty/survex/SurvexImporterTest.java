package org.hwyl.sexytopo.control.io.thirdparty.survex;

import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionImporter;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
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
    public void testLegCommentAppliedToFromStationOnBackwardLeg() throws Exception {
        // Station 1 is established first; leg "2 1 ..." is backward so comment goes on station 2
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline(
                "1\t2\t5.0\t0.0\t0.0\n2\t1\t3.0\t90.0\t0.0\tThe Squeeze", survey);
        // "2 1" is backward (1 already exists, 2 is new in from position)
        // comment should go on station 2 (the newer station, in the from position here)
        Assert.assertEquals("The Squeeze", survey.getStationByName("2").getComment());
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
}

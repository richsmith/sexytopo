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

    @Test
    public void testCrossedOutSplayIsImported() throws Exception {
        final String testContent = ";0\t-\t5.0\t90.0\t0.0";
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline(testContent, survey);
        Assert.assertEquals(1, survey.getAllLegs().size());
        Assert.assertTrue(survey.getAllLegs().get(0).isCrossedOut());
    }

    @Test
    public void testCrossedOutLegIsImported() throws Exception {
        final String testContent = ";0\t1\t5.0\t0.0\t0.0";
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline(testContent, survey);
        Assert.assertEquals(1, survey.getAllLegs().size());
        Leg leg = survey.getAllLegs().get(0);
        Assert.assertTrue(leg.isCrossedOut());
        Assert.assertNotNull(survey.getStationByName("1"));
    }

    @Test
    public void testNormalLegIsNotCrossedOut() throws Exception {
        final String testContent = "0\t1\t5.0\t0.0\t0.0";
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline(testContent, survey);
        Assert.assertFalse(survey.getAllLegs().get(0).isCrossedOut());
    }

    @Test
    public void testPlainCommentLineIsSkipped() throws Exception {
        final String testContent = "; this is just a comment";
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline(testContent, survey);
        Assert.assertEquals(0, survey.getAllLegs().size());
    }

    @Test
    public void testCrossedOutPromotedLegPreservesPromotedFrom() throws Exception {
        final String testContent =
                ";0\t1\t5.541\t253.93\t4.67\n"
                + ";;0\t1\t5.542\t73.95\t-4.64\n"
                + ";;0\t1\t5.541\t73.93\t-4.69";
        Survey survey = new Survey();
        SurvexTherionImporter.parseCentreline(testContent, survey);
        Assert.assertEquals(1, survey.getAllLegs().size());
        Leg leg = survey.getAllLegs().get(0);
        Assert.assertTrue(leg.isCrossedOut());
        Assert.assertEquals(2, leg.getPromotedFrom().length);
    }
}

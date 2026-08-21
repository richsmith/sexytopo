package org.hwyl.sexytopo.control.io.thirdparty.svg;

import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;
import org.hwyl.sexytopo.testutils.ExampleSurveyCreator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SvgExporterTest {

    @Before
    public void setUp() {
        GeneralPreferences.initialise(RuntimeEnvironment.getApplication());
    }

    private static Survey surveyWithTrip(Trip trip) {
        Survey survey = ExampleSurveyCreator.create(5, 2);
        survey.setTrip(trip);
        return survey;
    }

    @Test
    public void testCopyrightAndLicenceAppearInLegendAndDesc() throws Exception {
        Trip trip = new Trip();
        trip.setCopyrightHolder("Caver Jane");
        trip.setLicence("CC BY 4.0");
        Survey survey = surveyWithTrip(trip);

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertTrue(content.contains("Caver Jane"));
        Assert.assertTrue(content.contains("CC BY 4.0"));
        Assert.assertTrue(content.contains("<desc>"));
        Assert.assertTrue(content.contains("<title>"));
    }

    @Test
    public void testCopyrightLineWrittenWithoutASurveyDate() throws Exception {
        Trip trip = new Trip();
        trip.setCopyrightHolder("Caver Jane");
        trip.setSurveyDate(null);
        Survey survey = surveyWithTrip(trip);

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertTrue(content.contains("Caver Jane"));
    }

    @Test
    public void testCopyrightLineOmittedFromLegendButDescStillPresentWhenOptionDisabled()
            throws Exception {
        SharedPreferences prefs = GeneralPreferences.getRawPreferences();
        Assert.assertNotNull(prefs);
        prefs.edit().putBoolean("pref_export_svg_copyright", false).apply();

        Trip trip = new Trip();
        trip.setCopyrightHolder("Caver Jane");
        trip.setLicence("CC BY 4.0");
        Survey survey = surveyWithTrip(trip);

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        // The desc element (non-visual metadata) is independent of the legend toggle, so it
        // should still be present...
        Assert.assertTrue(content.contains("<desc>"));

        // ...but the copyright text should now appear only once (inside <desc>), not a second
        // time as a visible legend line.
        int occurrences = content.split("Caver Jane", -1).length - 1;
        Assert.assertEquals(1, occurrences);
    }

    @Test
    public void testNoTitleOrDescWhenNeitherCopyrightNorLicenceSet() throws Exception {
        Survey survey = ExampleSurveyCreator.create(5, 2); // no trip set at all

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertFalse(content.contains("<desc>"));
        Assert.assertFalse(content.contains("<title>"));
    }

    @Test
    public void testCopyrightOnlyOmitsLicenceText() throws Exception {
        Trip trip = new Trip();
        trip.setCopyrightHolder("Caver Jane");
        Survey survey = surveyWithTrip(trip);

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertTrue(content.contains("Caver Jane"));
        Assert.assertTrue(content.contains("\u00A9"));
        Assert.assertFalse(content.contains("CC BY"));
    }

    @Test
    public void testLicenceOnlyOmitsCopyrightSymbol() throws Exception {
        Trip trip = new Trip();
        trip.setLicence("CC BY 4.0");
        Survey survey = surveyWithTrip(trip);

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertTrue(content.contains("CC BY 4.0"));
        Assert.assertFalse(content.contains("\u00A9"));
    }

    @Test
    public void testCopyrightIncludesYearAfterSymbol() throws Exception {
        Trip trip = new Trip();
        trip.setSurveyDate(new SimpleDateFormat("yyyy-MM-dd").parse("2024-06-15"));
        trip.setCopyrightHolder("Caver Jane");
        trip.setLicence("CC BY 4.0");
        Survey survey = surveyWithTrip(trip);

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertTrue(content.contains("\u00A9 2024 Caver Jane"));
    }

    @Test
    public void testCopyrightYearOmittedWhenSurveyDateNull() throws Exception {
        Trip trip = new Trip();
        trip.setSurveyDate(null);
        trip.setCopyrightHolder("Caver Jane");
        Survey survey = surveyWithTrip(trip);

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertTrue(content.contains("\u00A9 Caver Jane"));
    }
}

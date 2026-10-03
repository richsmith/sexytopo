package org.hwyl.sexytopo.control.io.thirdparty.survextherion;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;
import org.junit.Assert;
import org.junit.Test;

public class SurvexTherionUtilTest {

    @Test
    public void testCopyrightLineWithoutASurveyDateOmitsTheYear() {
        // Regression test: this used to NPE, since formatYear was called unguarded here while
        // the SVG exporter guarded it. An imported survey can arrive with no date.
        Survey survey = new Survey();
        Trip trip = new Trip();
        trip.setCopyrightHolder("Caver Jane");
        trip.setSurveyDate(null);
        survey.setTrip(trip);

        String line = SurvexTherionUtil.getCopyrightLine(survey, SurveyFormat.SURVEX);

        Assert.assertEquals("*copyright \"Caver Jane\"\n", line);
    }

    @Test
    public void testSurvexLineWithBothCopyrightAndLicence() {
        Survey survey = surveyWithTrip("Caver Jane", "CC BY 4.0", 2026);

        String line = SurvexTherionUtil.getCopyrightLine(survey, SurveyFormat.SURVEX);

        Assert.assertEquals("*copyright 2026 \"Caver Jane\" ;\"CC BY 4.0\"\n", line);
    }

    @Test
    public void testTherionLineWithBothCopyrightAndLicence() {
        Survey survey = surveyWithTrip("Caver Jane", "CC BY 4.0", 2026);

        String line = SurvexTherionUtil.getCopyrightLine(survey, SurveyFormat.THERION);

        Assert.assertEquals("copyright 2026 \"Caver Jane\" #\"CC BY 4.0\"\n", line);
    }

    @Test
    public void testCopyrightOnlyOmitsTrailingComment() {
        Survey survey = surveyWithTrip("Caver Jane", "", 2026);

        String line = SurvexTherionUtil.getCopyrightLine(survey, SurveyFormat.THERION);

        Assert.assertEquals("copyright 2026 \"Caver Jane\"\n", line);
    }

    @Test
    public void testLicenceOnlyFillsCopyrightWithEmptyQuotes() {
        Survey survey = surveyWithTrip("", "CC BY 4.0", 2026);

        String line = SurvexTherionUtil.getCopyrightLine(survey, SurveyFormat.THERION);

        Assert.assertEquals("copyright 2026 \"\" #\"CC BY 4.0\"\n", line);
    }

    @Test
    public void testNeitherCopyrightNorLicenceSetReturnsEmptyString() {
        Survey survey = surveyWithTrip("", "", 2026);

        Assert.assertEquals("", SurvexTherionUtil.getCopyrightLine(survey, SurveyFormat.SURVEX));
        Assert.assertEquals("", SurvexTherionUtil.getCopyrightLine(survey, SurveyFormat.THERION));
    }

    @Test
    public void testNullTripReturnsEmptyString() {
        Survey survey = new Survey();

        Assert.assertEquals("", SurvexTherionUtil.getCopyrightLine(survey, SurveyFormat.SURVEX));
        Assert.assertEquals("", SurvexTherionUtil.getCopyrightLine(survey, SurveyFormat.THERION));
    }

    @Test
    public void testYearIsTakenFromTripSurveyDate() {
        Survey survey = surveyWithTrip("Caver Jane", "", 1998);

        String line = SurvexTherionUtil.getCopyrightLine(survey, SurveyFormat.THERION);

        Assert.assertEquals("copyright 1998 \"Caver Jane\"\n", line);
    }

    private static Survey surveyWithTrip(String copyright, String licence, int year) {
        Survey survey = new Survey();
        Trip trip = new Trip();
        trip.setSurveyDate(dateForYear(year));
        trip.setCopyrightHolder(copyright);
        trip.setLicence(licence);
        survey.setTrip(trip);
        return survey;
    }

    private static Date dateForYear(int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        return calendar.getTime();
    }

    private static Survey surveyWithAHiddenSplay() {
        Survey survey = new Survey();
        Leg visibleSplay = new Leg(1.0f, 0.0f, 0.0f);
        Leg hiddenSplay = new Leg(2.5f, 90.0f, 10.0f);
        hiddenSplay.setComment("Boulder");
        SurveyUpdater.update(survey, visibleSplay);
        SurveyUpdater.update(survey, hiddenSplay);
        SurveyUpdater.updateWithNewStation(survey, new Leg(5.0f, 0.0f, 0.0f));
        SurveyUpdater.setSplayHidden(survey, hiddenSplay, true);
        return survey;
    }

    private static List<String> getDataLines(Survey survey, SurveyFormat format) {
        String data = SurvexTherionUtil.getCentrelineData(survey, format);
        return Arrays.asList(data.split("\n"));
    }

    @Test
    public void testHiddenSplayIsCommentedOutInSurvex() {
        List<String> lines = getDataLines(surveyWithAHiddenSplay(), SurveyFormat.SURVEX);

        Assert.assertTrue(lines.contains(";1\t..\t2.500\t90.00\t10.00\tBoulder"));
    }

    @Test
    public void testHiddenSplayIsCommentedOutInTherion() {
        List<String> lines = getDataLines(surveyWithAHiddenSplay(), SurveyFormat.THERION);

        Assert.assertTrue(lines.contains("#1\t-\t2.500\t90.00\t10.00\tBoulder"));
    }

    private static void assertOnlyTheHiddenSplayIsCommentedOut(
            SurveyFormat format, String splayName) {
        List<String> lines = getDataLines(surveyWithAHiddenSplay(), format);

        Assert.assertTrue(lines.contains("1\t" + splayName + "\t1.000\t0.00\t0.00\t"));
        Assert.assertTrue(lines.contains("1\t2\t5.000\t0.00\t0.00\t"));
        long commentedLines =
                lines.stream()
                        .filter(line -> line.startsWith(String.valueOf(format.getCommentChar())))
                        .count();
        Assert.assertEquals(1, commentedLines);
    }

    @Test
    public void testOnlyHiddenSplaysAreCommentedOut() {
        assertOnlyTheHiddenSplayIsCommentedOut(SurveyFormat.SURVEX, "..");
        assertOnlyTheHiddenSplayIsCommentedOut(SurveyFormat.THERION, "-");
    }

    @Test
    public void testHiddenSplayKeepsItsPlaceInTheData() {
        List<String> lines = getDataLines(surveyWithAHiddenSplay(), SurveyFormat.SURVEX);

        int visibleSplay = lines.indexOf("1\t..\t1.000\t0.00\t0.00\t");
        int hiddenSplay = lines.indexOf(";1\t..\t2.500\t90.00\t10.00\tBoulder");
        int leg = lines.indexOf("1\t2\t5.000\t0.00\t0.00\t");

        Assert.assertTrue(visibleSplay >= 0);
        Assert.assertTrue(visibleSplay < hiddenSplay);
        Assert.assertTrue(hiddenSplay < leg);
    }
}

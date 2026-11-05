package org.hwyl.sexytopo.control.io.thirdparty.survextherion;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
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

    // ---- splays to a wall and to another feature ----

    @Test
    public void testAnonymousStationNameForEachFormatAndType() {
        Object[][] cases = {
            {SurveyFormat.SURVEX, true, ".."},
            {SurveyFormat.SURVEX, false, "."},
            {SurveyFormat.THERION, true, "-"},
            {SurveyFormat.THERION, false, "."},
        };
        for (Object[] c : cases) {
            SurveyFormat format = (SurveyFormat) c[0];
            boolean toWall = (Boolean) c[1];
            Assert.assertEquals(
                    format + " " + toWall, c[2], format.getAnonymousStationName(toWall));
        }
    }

    @Test
    public void testCentrelineWritesEachSplayTypeAndLeavesLegsAlone() {
        Survey survey = new Survey();
        SurveyUpdater.updateWithNewStation(survey, new Leg(5, 0, -10));
        SurveyUpdater.update(survey, new Leg(2, 90, 5));
        SurveyUpdater.update(survey, new Leg(3, 270, 5).withToWall(false));
        SurveyUpdater.updateWithNewStation(survey, new Leg(4, 180, -10, true));

        Object[][] cases = {
            {SurveyFormat.SURVEX, "*", ".."},
            {SurveyFormat.THERION, "", "-"},
        };
        for (Object[] c : cases) {
            SurveyFormat format = (SurveyFormat) c[0];
            String[] expected = {
                c[1] + "data normal from to tape compass clino ignoreall",
                "1\t2\t5.000\t0.00\t-10.00\t",
                "2\t" + c[2] + "\t2.000\t90.00\t5.00\t",
                "2\t.\t3.000\t270.00\t5.00\t",
                "3\t2\t4.000\t0.00\t10.00\t",
            };
            String[] lines = SurvexTherionUtil.getCentrelineData(survey, format).split("\n");
            Assert.assertArrayEquals(format.toString(), expected, lines);
        }
    }

    @Test
    public void testPromotedLegFromAFeatureReadingIsWrittenToItsStation() {
        Leg[] readings = {new Leg(5, 0, 0), new Leg(5, 0, 0).withToWall(false)};
        Survey survey = new Survey();
        survey.getOrigin().addOnwardLeg(new Leg(5, 0, 0, new Station("2"), readings));

        for (SurveyFormat format : SurveyFormat.values()) {
            String data = SurvexTherionUtil.getCentrelineData(survey, format);
            String[] lines = data.split("\n");
            for (int i = 1; i < lines.length; i++) {
                Assert.assertEquals(format + " line " + i, "2", lines[i].split("\t")[1]);
            }
        }
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

    private static Survey surveyWithADisabledSplay() {
        Survey survey = new Survey();
        Leg enabledSplay = new Leg(1.0f, 0.0f, 0.0f);
        Leg disabledSplay = new Leg(2.5f, 90.0f, 10.0f);
        disabledSplay.setComment("Boulder");
        SurveyUpdater.update(survey, enabledSplay);
        SurveyUpdater.update(survey, disabledSplay);
        SurveyUpdater.updateWithNewStation(survey, new Leg(5.0f, 0.0f, 0.0f));
        SurveyUpdater.setSplayEnabled(survey, disabledSplay, false);
        return survey;
    }

    private static List<String> getDataLines(Survey survey, SurveyFormat format) {
        String data = SurvexTherionUtil.getCentrelineData(survey, format);
        return Arrays.asList(data.split("\n"));
    }

    @Test
    public void testDisabledSplayIsCommentedOutInSurvex() {
        List<String> lines = getDataLines(surveyWithADisabledSplay(), SurveyFormat.SURVEX);

        Assert.assertTrue(lines.contains(";1\t..\t2.500\t90.00\t10.00\tBoulder"));
    }

    @Test
    public void testDisabledSplayIsCommentedOutInTherion() {
        List<String> lines = getDataLines(surveyWithADisabledSplay(), SurveyFormat.THERION);

        Assert.assertTrue(lines.contains("#1\t-\t2.500\t90.00\t10.00\tBoulder"));
    }

    private static void assertOnlyTheDisabledSplayIsCommentedOut(
            SurveyFormat format, String splayName) {
        List<String> lines = getDataLines(surveyWithADisabledSplay(), format);

        Assert.assertTrue(lines.contains("1\t" + splayName + "\t1.000\t0.00\t0.00\t"));
        Assert.assertTrue(lines.contains("1\t2\t5.000\t0.00\t0.00\t"));
        long commentedLines =
                lines.stream()
                        .filter(line -> line.startsWith(String.valueOf(format.getCommentChar())))
                        .count();
        Assert.assertEquals(1, commentedLines);
    }

    @Test
    public void testOnlyDisabledSplaysAreCommentedOut() {
        assertOnlyTheDisabledSplayIsCommentedOut(SurveyFormat.SURVEX, "..");
        assertOnlyTheDisabledSplayIsCommentedOut(SurveyFormat.THERION, "-");
    }

    @Test
    public void testDisabledSplayKeepsItsPlaceInTheData() {
        List<String> lines = getDataLines(surveyWithADisabledSplay(), SurveyFormat.SURVEX);

        int enabledSplay = lines.indexOf("1\t..\t1.000\t0.00\t0.00\t");
        int disabledSplay = lines.indexOf(";1\t..\t2.500\t90.00\t10.00\tBoulder");
        int leg = lines.indexOf("1\t2\t5.000\t0.00\t0.00\t");

        Assert.assertTrue(enabledSplay >= 0);
        Assert.assertTrue(enabledSplay < disabledSplay);
        Assert.assertTrue(disabledSplay < leg);
    }
}

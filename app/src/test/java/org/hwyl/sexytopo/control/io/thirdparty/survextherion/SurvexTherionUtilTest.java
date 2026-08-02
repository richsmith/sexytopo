package org.hwyl.sexytopo.control.io.thirdparty.survextherion;

import java.util.Calendar;
import java.util.Date;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;
import org.junit.Assert;
import org.junit.Test;

public class SurvexTherionUtilTest {

    @Test
    public void testSurvexLineWithBothCopyrightAndLicense() {
        Survey survey = surveyWithTrip("Caver Jane", "CC BY 4.0", 2026);

        String line = SurvexTherionUtil.getCopyrightLine(survey, SurveyFormat.SURVEX);

        Assert.assertEquals("*copyright 2026 \"Caver Jane\" ;\"CC BY 4.0\"\n", line);
    }

    @Test
    public void testTherionLineWithBothCopyrightAndLicense() {
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
    public void testLicenseOnlyFillsCopyrightWithEmptyQuotes() {
        Survey survey = surveyWithTrip("", "CC BY 4.0", 2026);

        String line = SurvexTherionUtil.getCopyrightLine(survey, SurveyFormat.THERION);

        Assert.assertEquals("copyright 2026 \"\" #\"CC BY 4.0\"\n", line);
    }

    @Test
    public void testNeitherCopyrightNorLicenseSetReturnsEmptyString() {
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

    private static Survey surveyWithTrip(String copyright, String license, int year) {
        Survey survey = new Survey();
        Trip trip = new Trip();
        trip.setSurveyDate(dateForYear(year));
        trip.setCopyright(copyright);
        trip.setLicense(license);
        survey.setTrip(trip);
        return survey;
    }

    private static Date dateForYear(int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        return calendar.getTime();
    }
}

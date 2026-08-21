package org.hwyl.sexytopo.control.io.thirdparty.survextherion;

import java.util.Calendar;
import java.util.Date;
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
}

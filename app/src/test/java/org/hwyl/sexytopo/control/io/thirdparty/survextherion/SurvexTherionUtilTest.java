package org.hwyl.sexytopo.control.io.thirdparty.survextherion;

import org.hwyl.sexytopo.model.graph.Direction;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;

public class SurvexTherionUtilTest {

    // -------------------------------------------------------------------------
    // Survex extended elevation  .espec path: *start, *eleft, *eright
    // -------------------------------------------------------------------------

    @Test
    public void testEspecExtensionsContainsStartForOrigin() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        String originName = survey.getOrigin().getName();

        String espec = SurvexTherionUtil.getEspecExtendedElevationExtensions(survey);

        Assert.assertTrue(
                "espec must contain '*start' for the origin station",
                espec.contains("*start " + originName));
    }

    @Test
    public void testEspecExtensionsContainsEleftForLeftStation() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station changedStation = survey.getActiveStation();
        changedStation.setExtendedElevationDirection(Direction.LEFT);

        String espec = SurvexTherionUtil.getEspecExtendedElevationExtensions(survey);

        Assert.assertTrue(
                "espec must contain '*eleft' for a station whose direction changed to left",
                espec.contains("*eleft " + changedStation.getName()));
    }

    @Test
    public void testEspecExtensionsContainsErightForRightStation() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        // Set intermediate to LEFT so the subsequent RIGHT is a change
        Station intermediate = survey.getOrigin().getConnectedOnwardLegs().get(0).getDestination();
        intermediate.setExtendedElevationDirection(Direction.LEFT);
        Station lastStation = survey.getActiveStation();
        lastStation.setExtendedElevationDirection(Direction.RIGHT);

        String espec = SurvexTherionUtil.getEspecExtendedElevationExtensions(survey);

        Assert.assertTrue(
                "espec must contain '*eright' for a station whose direction changed back to right",
                espec.contains("*eright " + lastStation.getName()));
    }

    @Test
    public void testEspecExtensionsOmitsUnchangedStations() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        // All stations left at default RIGHT — no direction changes

        String espec = SurvexTherionUtil.getEspecExtendedElevationExtensions(survey);

        Assert.assertFalse(
                "espec must not emit direction commands for stations with unchanged direction",
                espec.contains("*eleft") || espec.contains("*eright"));
    }

    @Test
    public void testEspecExtensionsDoesNotContainExtendKeyword() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        survey.getActiveStation().setExtendedElevationDirection(Direction.LEFT);

        String espec = SurvexTherionUtil.getEspecExtendedElevationExtensions(survey);

        Assert.assertFalse(
                "espec output must not contain the word 'extend'", espec.contains("extend"));
    }

    // -------------------------------------------------------------------------
    // Therion extended elevation:  extend start/left/right
    // -------------------------------------------------------------------------

    @Test
    public void testTherionExtendContainsExtendStartForOrigin() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        String originName = survey.getOrigin().getName();

        String result =
                SurvexTherionUtil.getExtendedElevationExtensions(survey, SurveyFormat.THERION);

        Assert.assertTrue(
                "Therion output must contain 'extend start' for the origin station",
                result.contains("extend start " + originName));
    }

    @Test
    public void testTherionExtendContainsExtendLeftForLeftStation() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station changedStation = survey.getActiveStation();
        changedStation.setExtendedElevationDirection(Direction.LEFT);

        String result =
                SurvexTherionUtil.getExtendedElevationExtensions(survey, SurveyFormat.THERION);

        Assert.assertTrue(
                "Therion output must contain 'extend left' for a station changed to left",
                result.contains("extend left " + changedStation.getName()));
    }

    @Test
    public void testTherionExtendContainsExtendRightForRightStation() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station intermediate = survey.getOrigin().getConnectedOnwardLegs().get(0).getDestination();
        intermediate.setExtendedElevationDirection(Direction.LEFT);
        Station lastStation = survey.getActiveStation();
        lastStation.setExtendedElevationDirection(Direction.RIGHT);

        String result =
                SurvexTherionUtil.getExtendedElevationExtensions(survey, SurveyFormat.THERION);

        Assert.assertTrue(
                "Therion output must contain 'extend right' for a station changed back to right",
                result.contains("extend right " + lastStation.getName()));
    }
}

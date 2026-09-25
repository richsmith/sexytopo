package org.hwyl.sexytopo.control.io.thirdparty.survex;

import org.hwyl.sexytopo.model.geometry.ExtendedElevationDirection;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;

public class EspecParserTest {

    @Test
    public void testEleftAppliesLeftDirectionToStation() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        String stationName = survey.getActiveStation().getName();

        EspecParser.applyEspecText("*start 1\n*eleft " + stationName + "\n", survey);

        Assert.assertEquals(
                ExtendedElevationDirection.LEFT,
                survey.getActiveStation().getExtendedElevationDirection());
    }

    @Test
    public void testErightAppliesRightDirectionToStation() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        String stationName = survey.getActiveStation().getName();
        // First set it to LEFT so we can confirm RIGHT is applied
        survey.getActiveStation().setExtendedElevationDirection(ExtendedElevationDirection.LEFT);

        EspecParser.applyEspecText("*start 1\n*eright " + stationName + "\n", survey);

        Assert.assertEquals(
                ExtendedElevationDirection.RIGHT,
                survey.getActiveStation().getExtendedElevationDirection());
    }

    @Test
    public void testStartLineDoesNotChangeAnyDirection() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        String originName = survey.getOrigin().getName();

        EspecParser.applyEspecText("*start " + originName + "\n", survey);

        Assert.assertEquals(
                ExtendedElevationDirection.RIGHT,
                survey.getOrigin().getExtendedElevationDirection());
    }

    @Test
    public void testUnknownStationIsSkippedWithoutException() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();

        EspecParser.applyEspecText("*start 1\n*eleft nonexistent\n", survey);

        // All stations should remain at default RIGHT
        Assert.assertEquals(
                ExtendedElevationDirection.RIGHT,
                survey.getOrigin().getExtendedElevationDirection());
    }

    @Test
    public void testMissingEspecLeavesAllStationsRight() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();

        // Applying empty text simulates no espec being present
        EspecParser.applyEspecText("", survey);

        Assert.assertEquals(
                ExtendedElevationDirection.RIGHT,
                survey.getOrigin().getExtendedElevationDirection());
        Assert.assertEquals(
                ExtendedElevationDirection.RIGHT,
                survey.getActiveStation().getExtendedElevationDirection());
    }

    @Test
    public void testMalformedLineIsSkippedWithoutException() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();

        EspecParser.applyEspecText("*start 1\nthis line is malformed extra tokens here\n", survey);

        // All stations should remain at default RIGHT — no exception thrown
        Assert.assertEquals(
                ExtendedElevationDirection.RIGHT,
                survey.getOrigin().getExtendedElevationDirection());
    }

    @Test
    public void testCommentLinesAreIgnored() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        String stationName = survey.getActiveStation().getName();

        EspecParser.applyEspecText(
                "; this is a comment\n*start 1\n*eleft " + stationName + "\n", survey);

        Assert.assertEquals(
                ExtendedElevationDirection.LEFT,
                survey.getActiveStation().getExtendedElevationDirection());
    }

    @Test
    public void testUnknownCommandIsSkippedWithoutException() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();

        EspecParser.applyEspecText("*start 1\n*unknown 2\n", survey);

        Assert.assertEquals(
                ExtendedElevationDirection.RIGHT,
                survey.getOrigin().getExtendedElevationDirection());
    }

    @Test
    public void testCommentedEverticalIsApplied() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        // Station 2 is from, station 3 is to (destination)
        String fromName =
                survey.getOrigin().getConnectedOnwardLegs().get(0).getDestination().getName();
        String toName =
                survey.getOrigin()
                        .getConnectedOnwardLegs()
                        .get(0)
                        .getDestination()
                        .getConnectedOnwardLegs()
                        .get(0)
                        .getDestination()
                        .getName();

        EspecParser.applyEspecText(
                "*start 1\n; *evertical " + fromName + " " + toName + "\n", survey);

        Assert.assertEquals(
                "destination station must be set to VERTICAL",
                ExtendedElevationDirection.VERTICAL,
                survey.getStationByName(toName).getExtendedElevationDirection());
    }

    @Test
    public void testCommentedEverticalSetsOnlyDestinationNotFrom() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        String fromName =
                survey.getOrigin().getConnectedOnwardLegs().get(0).getDestination().getName();
        String toName =
                survey.getOrigin()
                        .getConnectedOnwardLegs()
                        .get(0)
                        .getDestination()
                        .getConnectedOnwardLegs()
                        .get(0)
                        .getDestination()
                        .getName();

        EspecParser.applyEspecText(
                "*start 1\n; *evertical " + fromName + " " + toName + "\n", survey);

        Assert.assertEquals(
                "from station must remain RIGHT",
                ExtendedElevationDirection.RIGHT,
                survey.getStationByName(fromName).getExtendedElevationDirection());
    }

    @Test
    public void testCommentedEverticalDoesNotPropagateToStationBelow() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        String fromName =
                survey.getOrigin().getConnectedOnwardLegs().get(0).getDestination().getName();
        String toName =
                survey.getOrigin()
                        .getConnectedOnwardLegs()
                        .get(0)
                        .getDestination()
                        .getConnectedOnwardLegs()
                        .get(0)
                        .getDestination()
                        .getName();
        // Station below toName is the active station
        String belowName = survey.getActiveStation().getName();

        EspecParser.applyEspecText(
                "*start 1\n; *evertical " + fromName + " " + toName + "\n", survey);

        Assert.assertEquals(
                "station below destination must remain RIGHT — VERTICAL must not propagate",
                ExtendedElevationDirection.RIGHT,
                survey.getStationByName(belowName).getExtendedElevationDirection());
    }

    @Test
    public void testOtherCommentedLinesAreStillSkipped() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        String stationName = survey.getActiveStation().getName();

        // A commented *eleft must be ignored — it was commented out for a reason
        EspecParser.applyEspecText("*start 1\n; *eleft " + stationName + "\n", survey);

        Assert.assertEquals(
                "commented *eleft must not be applied",
                ExtendedElevationDirection.RIGHT,
                survey.getActiveStation().getExtendedElevationDirection());
    }
}

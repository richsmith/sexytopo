package org.hwyl.sexytopo.control.io.thirdparty.therion;

import java.util.Arrays;
import java.util.Collections;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionUtil;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurveyFormat;
import org.hwyl.sexytopo.model.graph.Direction;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;

public class ThExporterTest {

    final String TEST_CONTENT =
            "encoding  utf-8\n"
                    + "survey dafung-down-west\n"
                    + "\n"
                    + "input dafung-down-west.th2\n"
                    + "input dafung-down-westEe.th2\n"
                    + "\n"
                    + "centreline"
                    + "\n"
                    + "data normal from to tape compass clino ignoreall\n"
                    + "  extend right\n"
                    + "1.4  8.0  0.0  0.0  0.0  \n"
                    + "#8.0  8.1  13.883  283.04  -5.5  \n"
                    + "#8.0  8.1  13.885  283.08  -5.5  \n"
                    + "#8.0  8.1  13.882  283.08  -5.61  \n"
                    + "8.0  8.1  13.883  283.06  -5.54  Calculated leg from 3 above\n"
                    + "8.1  -  1.566  106.33  82.08  \n"
                    + "endcentreline\n"
                    + "endsurvey";

    @Test
    public void testReplaceCentreline() {
        String updated = ThExporter.replaceCentreline(TEST_CONTENT, "replacement");
        Assert.assertTrue(updated.contains("replacement"));
        Assert.assertFalse(updated.contains("Calculated"));
    }

    @Test
    public void testReplaceInputs() {
        String updated = ThExporter.replaceInputsText(TEST_CONTENT, "input replacement");
        Assert.assertTrue(updated.contains("input replacement"));
        Assert.assertFalse(updated.contains("dafung-down-west.th2"));
    }

    @Test
    public void testTeamLinesExcludeExplorerRole() {
        Trip trip =
                createTripWithTeam(
                        entry("Alice", Trip.Role.INSTRUMENTS, Trip.Role.EXPLORATION),
                        entry("Bob", Trip.Role.BOOK));
        String result = ThExporter.formatTeamLines(trip);
        Assert.assertTrue(result.contains("team \"Alice\" instruments"));
        Assert.assertFalse(result.contains("explorer"));
        Assert.assertTrue(result.contains("team \"Bob\" notes"));
    }

    @Test
    public void testTeamLinesSkipExplorerOnlyMembers() {
        Trip trip =
                createTripWithTeam(
                        entry("Alice", Trip.Role.INSTRUMENTS), entry("Bob", Trip.Role.EXPLORATION));
        String result = ThExporter.formatTeamLines(trip);
        Assert.assertTrue(result.contains("team \"Alice\" instruments"));
        Assert.assertFalse(result.contains("Bob"));
    }

    @Test
    public void testExploTeamLines() {
        Trip trip =
                createTripWithTeam(
                        entry("Alice", Trip.Role.INSTRUMENTS, Trip.Role.EXPLORATION),
                        entry("Bob", Trip.Role.BOOK));
        String result = ThExporter.formatExploTeamLines(trip);
        Assert.assertTrue(result.contains("explo-team \"Alice\""));
        Assert.assertFalse(result.contains("Bob"));
    }

    @Test
    public void testExploTeamForExplorerOnlyMember() {
        Trip trip = createTripWithTeam(entry("Bob", Trip.Role.EXPLORATION));
        String result = ThExporter.formatExploTeamLines(trip);
        Assert.assertTrue(result.contains("explo-team \"Bob\""));
    }

    @Test
    public void testTeamLinesSkipMembersWithNoRoles() {
        Trip trip = createTripWithTeam(entry("Alice", Trip.Role.INSTRUMENTS), entryNoRoles("Bob"));
        String result = ThExporter.formatTeamLines(trip);
        Assert.assertTrue(result.contains("team \"Alice\" instruments"));
        Assert.assertFalse(result.contains("Bob"));
    }

    @Test
    public void testTeamLinesDogRoleOutputsAssistant() {
        Trip trip = createTripWithTeam(entry("Fido", Trip.Role.DOG));
        String result = ThExporter.formatTeamLines(trip);
        Assert.assertTrue(result.contains("assistant"));
    }

    @Test
    public void testTherionMetadataIncludesInstrumentWhenPresent() {
        Survey survey = new Survey();
        Trip trip = new Trip();
        trip.setInstrument("DistoX2");
        survey.setTrip(trip);

        String metadata = SurvexTherionUtil.getMetadata(survey, SurveyFormat.THERION, "", "");

        Assert.assertTrue(metadata.contains("instrument insts \"DistoX2\""));
        Assert.assertFalse(metadata.contains("#instrument insts \"\""));
    }

    @Test
    public void testTherionMetadataUsesCommentedEmptyInstrumentWhenBlank() {
        Survey survey = new Survey();
        Trip trip = new Trip();
        trip.setInstrument("   ");
        survey.setTrip(trip);

        String metadata = SurvexTherionUtil.getMetadata(survey, SurveyFormat.THERION, "", "");

        Assert.assertTrue(metadata.contains("#instrument insts \"\""));
    }

    @Test
    public void testTherionMetadataExploDateLinkedUsesSurveyDate() {
        Survey survey = new Survey();
        Trip trip = new Trip();
        trip.setExplorationDateLinked(true); // default, but explicit for clarity
        survey.setTrip(trip);

        String metadata = SurvexTherionUtil.getMetadata(survey, SurveyFormat.THERION, "", "");

        Assert.assertTrue(metadata.contains("explo-date "));
        Assert.assertFalse(metadata.contains("#explo-date "));
    }

    @Test
    public void testTherionMetadataExploDateUnlinkedWithDateSet() {
        Survey survey = new Survey();
        Trip trip = new Trip();
        trip.setExplorationDateLinked(false);
        trip.setExplorationDate(new java.util.Date(0)); // 1970.01.01
        survey.setTrip(trip);

        String metadata = SurvexTherionUtil.getMetadata(survey, SurveyFormat.THERION, "", "");

        Assert.assertTrue(metadata.contains("explo-date 1970.01.01"));
        Assert.assertFalse(metadata.contains("#explo-date "));
    }

    @Test
    public void testTherionMetadataExploDateUnlinkedEmptyUsesCommentedPlaceholder() {
        Survey survey = new Survey();
        Trip trip = new Trip();
        trip.setExplorationDateLinked(false);
        trip.setExplorationDate(null);
        survey.setTrip(trip);

        String metadata = SurvexTherionUtil.getMetadata(survey, SurveyFormat.THERION, "", "");

        Assert.assertTrue(metadata.contains("#explo-date "));
    }

    // -------------------------------------------------------------------------
    // Therion extend commands: extend start/left/right
    // -------------------------------------------------------------------------

    @Test
    public void testTherionExtendContainsExtendStartForOrigin() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        String originName = survey.getOrigin().getName();

        String content = ThExporter.getExtendedElevationExtensions(survey);

        Assert.assertTrue(
                "Therion output must contain 'extend start' for the origin station",
                content.contains("extend start " + originName));
    }

    @Test
    public void testTherionExtendContainsExtendLeftForLeftStation() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station changedStation = survey.getActiveStation();
        changedStation.setExtendedElevationDirection(Direction.LEFT);

        String content = ThExporter.getExtendedElevationExtensions(survey);

        Assert.assertTrue(
                "Therion output must contain 'extend left' for a station changed to left",
                content.contains("extend left " + changedStation.getName()));
    }

    @Test
    public void testTherionExtendContainsExtendRightForRightStation() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Station intermediate = survey.getOrigin().getConnectedOnwardLegs().get(0).getDestination();
        intermediate.setExtendedElevationDirection(Direction.LEFT);
        Station lastStation = survey.getActiveStation();
        lastStation.setExtendedElevationDirection(Direction.RIGHT);

        String content = ThExporter.getExtendedElevationExtensions(survey);

        Assert.assertTrue(
                "Therion output must contain 'extend right' for a station changed back to right",
                content.contains("extend right " + lastStation.getName()));
    }

    private static Trip.TeamEntry entry(String name, Trip.Role... roles) {
        return new Trip.TeamEntry(name, Arrays.asList(roles));
    }

    private static Trip.TeamEntry entryNoRoles(String name) {
        return new Trip.TeamEntry(name, Collections.<Trip.Role>emptyList());
    }

    private static Trip createTripWithTeam(Trip.TeamEntry... entries) {
        Trip trip = new Trip();
        trip.setTeam(Arrays.asList(entries));
        return trip;
    }
}

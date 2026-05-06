package org.hwyl.sexytopo.control.io.thirdparty.survex;

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

public class SurvexExporterTest {

    @Test
    public void testBasicExport() {
        SurvexExporter survexExporter = new SurvexExporter();
        Survey oneNorth = BasicTestSurveyCreator.createStraightNorth();
        String content = survexExporter.getContent(oneNorth);
        Assert.assertTrue(content.contains("1\t2\t5.000\t0.00\t0.00"));
        Assert.assertTrue(content.contains("2\t3\t5.000\t0.00\t0.00"));
        Assert.assertTrue(content.contains("3\t4\t5.000\t0.00\t0.00"));
    }

    @Test
    public void testBasicExportWithPromotedLegs() {
        SurvexExporter survexExporter = new SurvexExporter();
        Survey oneNorth = BasicTestSurveyCreator.createStraightNorthThroughRepeats();
        String content = survexExporter.getContent(oneNorth);
        Assert.assertTrue(content.contains("1\t2\t5.000\t0.00\t0.00"));
        // Promoted leg original readings are output as commented lines
        Assert.assertTrue(content.contains(";1\t2\t5.000\t0.00\t0.00"));
    }

    @Test
    public void testCommentsAreIncluded() {
        SurvexExporter survexExporter = new SurvexExporter();
        Survey oneNorth = BasicTestSurveyCreator.createStraightNorthThroughRepeats();
        Station latest = oneNorth.getActiveStation();
        String testComment = "Comment McComment Face";
        latest.setComment(testComment);
        String content = survexExporter.getContent(oneNorth);
        Assert.assertTrue(content.contains(testComment));
    }

    @Test
    public void testTeamLinesIncludeExplorerRole() {
        Trip trip =
                createTripWithTeam(
                        entry("Alice", Trip.Role.INSTRUMENTS, Trip.Role.EXPLORATION),
                        entry("Bob", Trip.Role.BOOK));
        String result = SurvexExporter.formatTeamLines(trip);
        Assert.assertTrue(result.contains("*team \"Alice\" instruments explorer"));
        Assert.assertTrue(result.contains("*team \"Bob\" notes"));
    }

    @Test
    public void testTeamLinesIncludeExplorerOnlyMembers() {
        Trip trip =
                createTripWithTeam(
                        entry("Alice", Trip.Role.INSTRUMENTS), entry("Bob", Trip.Role.EXPLORATION));
        String result = SurvexExporter.formatTeamLines(trip);
        Assert.assertTrue(result.contains("*team \"Alice\" instruments"));
        Assert.assertTrue(result.contains("*team \"Bob\" explorer"));
    }

    @Test
    public void testTeamLinesSkipMembersWithNoRoles() {
        Trip trip = createTripWithTeam(entry("Alice", Trip.Role.INSTRUMENTS), entryNoRoles("Bob"));
        String result = SurvexExporter.formatTeamLines(trip);
        Assert.assertTrue(result.contains("*team \"Alice\" instruments"));
        Assert.assertFalse(result.contains("Bob"));
    }

    @Test
    public void testTeamLinesDogRoleOutputsAssistant() {
        Trip trip = createTripWithTeam(entry("Fido", Trip.Role.DOG));
        String result = SurvexExporter.formatTeamLines(trip);
        Assert.assertTrue(result.contains("assistant"));
    }

    @Test
    public void testExportIncludesInstrumentWhenPresent() {
        SurvexExporter survexExporter = new SurvexExporter();
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Trip trip = new Trip();
        trip.setInstrument("DistoX2");
        survey.setTrip(trip);

        String content = survexExporter.getContent(survey);

        Assert.assertTrue(content.contains("*instrument insts \"DistoX2\""));
    }

    @Test
    public void testExportUsesCommentedEmptyInstrumentWhenBlank() {
        SurvexExporter survexExporter = new SurvexExporter();
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Trip trip = new Trip();
        trip.setInstrument("   ");
        survey.setTrip(trip);

        String content = survexExporter.getContent(survey);

        Assert.assertTrue(content.contains(";*instrument insts \"\""));
    }

    @Test
    public void testSurvexMetadataExploDateLinkedUsesSurveyDate() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Trip trip = new Trip();
        trip.setExplorationDateLinked(true); // default, but explicit for clarity
        survey.setTrip(trip);

        String metadata = SurvexTherionUtil.getMetadata(survey, SurveyFormat.SURVEX, "", "");

        Assert.assertTrue(metadata.contains("*date explored "));
        Assert.assertFalse(metadata.contains(";*date explored "));
    }

    @Test
    public void testSurvexMetadataExploDateUnlinkedWithDateSet() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Trip trip = new Trip();
        trip.setExplorationDateLinked(false);
        trip.setExplorationDate(new java.util.Date(0)); // 1970.01.01
        survey.setTrip(trip);

        String metadata = SurvexTherionUtil.getMetadata(survey, SurveyFormat.SURVEX, "", "");

        Assert.assertTrue(metadata.contains("*date explored 1970.01.01"));
        Assert.assertFalse(metadata.contains(";*date explored "));
    }

    @Test
    public void testSurvexMetadataExploDateUnlinkedEmptyUsesCommentedPlaceholder() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Trip trip = new Trip();
        trip.setExplorationDateLinked(false);
        trip.setExplorationDate(null);
        survey.setTrip(trip);

        String metadata = SurvexTherionUtil.getMetadata(survey, SurveyFormat.SURVEX, "", "");

        Assert.assertTrue(metadata.contains(";*date explored "));
    }

    @Test
    public void testGetContentDoesNotContainExtendCommands() {
        SurvexExporter survexExporter = new SurvexExporter();
        Survey survey = BasicTestSurveyCreator.createStraightNorth();

        String content = survexExporter.getContent(survey);

        Assert.assertFalse("*extend must not appear in .svx content", content.contains("*extend"));
    }

    @Test
    public void testGetContentDoesNotContainExtendCommandsWhenDirectionChanges() {
        SurvexExporter survexExporter = new SurvexExporter();
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        // Flip the active station to LEFT so there is a real direction change to emit
        survey.getActiveStation().setExtendedElevationDirection(Direction.LEFT);

        String content = survexExporter.getContent(survey);

        Assert.assertFalse(
                "*extend must not appear in .svx content even when directions change",
                content.contains("*extend"));
    }

    @Test
    public void testGetEspecContentContainsExtendStart() {
        SurvexExporter survexExporter = new SurvexExporter();
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        String originName = survey.getOrigin().getName();

        String espec = survexExporter.getEspecContent(survey);

        Assert.assertTrue(
                "espec must contain '*start' for the origin station",
                espec.contains("*start " + originName));
    }

    @Test
    public void testGetEspecContentContainsExtendLeftWhenStationSetToLeft() {
        SurvexExporter survexExporter = new SurvexExporter();
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        // Origin defaults to RIGHT; set the active (last) station to LEFT to trigger a change
        Station changedStation = survey.getActiveStation();
        changedStation.setExtendedElevationDirection(Direction.LEFT);

        String espec = survexExporter.getEspecContent(survey);

        Assert.assertTrue(
                "espec must contain '*eleft' for a station whose direction changed to left",
                espec.contains("*eleft " + changedStation.getName()));
    }

    @Test
    public void testGetEspecContentContainsExtendRightWhenStationSetToRight() {
        SurvexExporter survexExporter = new SurvexExporter();
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        // Set stations 2 and 3 to LEFT so the direction change back to RIGHT occurs at station 4
        Station intermediate = survey.getOrigin().getConnectedOnwardLegs().get(0).getDestination();
        intermediate.setExtendedElevationDirection(Direction.LEFT);
        Station intermediate2 = intermediate.getConnectedOnwardLegs().get(0).getDestination();
        intermediate2.setExtendedElevationDirection(Direction.LEFT);
        // The active (last) station should remain RIGHT (the default), producing an eright command
        Station lastStation = survey.getActiveStation();
        lastStation.setExtendedElevationDirection(Direction.RIGHT);

        String espec = survexExporter.getEspecContent(survey);

        Assert.assertTrue(
                "espec must contain '*eright' for a station whose direction changed back to right",
                espec.contains("*eright " + lastStation.getName()));
    }

    @Test
    public void testGetEspecContentOmitsStationsWithUnchangedDirection() {
        SurvexExporter survexExporter = new SurvexExporter();
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        // Leave all stations at the default RIGHT — only the origin start command should appear
        String originName = survey.getOrigin().getName();

        String espec = survexExporter.getEspecContent(survey);

        Assert.assertTrue(espec.contains("*start " + originName));
        Assert.assertFalse(
                "espec must not emit stations whose direction has not changed",
                espec.contains("*eleft") || espec.contains("*eright"));
    }

    @Test
    public void testEspecFileExtensionConstant() {
        Assert.assertEquals("espec", SurvexExporter.ESPEC_EXTENSION);
    }

    @Test
    public void testEspecMimeTypeIsOctetStream() {
        // application/octet-stream prevents Android from appending a .txt suffix
        Assert.assertEquals("application/octet-stream", SurvexExporter.ESPEC_MIME_TYPE);
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

package org.hwyl.sexytopo.control.io.thirdparty.survex;

import java.util.Arrays;
import java.util.Collections;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionUtil;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurveyFormat;
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

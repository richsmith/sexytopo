package org.hwyl.sexytopo.control.io.thirdparty.survex;

import org.junit.Assert;

import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;


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
        Trip trip = createTripWithTeam(
            entry("Alice", Trip.Role.INSTRUMENTS, Trip.Role.EXPLORATION),
            entry("Bob", Trip.Role.BOOK)
        );
        String result = SurvexExporter.formatTeamLines(trip);
        Assert.assertTrue(result.contains("*team \"Alice\" instruments explorer"));
        Assert.assertTrue(result.contains("*team \"Bob\" notes"));
    }

    @Test
    public void testTeamLinesIncludeExplorerOnlyMembers() {
        Trip trip = createTripWithTeam(
            entry("Alice", Trip.Role.INSTRUMENTS),
            entry("Bob", Trip.Role.EXPLORATION)
        );
        String result = SurvexExporter.formatTeamLines(trip);
        Assert.assertTrue(result.contains("*team \"Alice\" instruments"));
        Assert.assertTrue(result.contains("*team \"Bob\" explorer"));
    }

    @Test
    public void testTeamLinesSkipMembersWithNoRoles() {
        Trip trip = createTripWithTeam(
            entry("Alice", Trip.Role.INSTRUMENTS),
            entryNoRoles("Bob")
        );
        String result = SurvexExporter.formatTeamLines(trip);
        Assert.assertTrue(result.contains("*team \"Alice\" instruments"));
        Assert.assertFalse(result.contains("Bob"));
    }

    @Test
    public void testTeamLinesDogRoleOutputsAssistant() {
        Trip trip = createTripWithTeam(
            entry("Fido", Trip.Role.DOG)
        );
        String result = SurvexExporter.formatTeamLines(trip);
        Assert.assertTrue(result.contains("assistant"));
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


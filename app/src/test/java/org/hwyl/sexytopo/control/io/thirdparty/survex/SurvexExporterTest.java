package org.hwyl.sexytopo.control.io.thirdparty.survex;

import java.util.Arrays;
import java.util.Collections;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionUtil;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurveyFormat;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.survey.Leg;
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
    public void testBackwardsLegExportedAsTaken() {
        // Regression test: a leg shot backwards must be exported with the stations and
        // reading as they were physically taken, not as they are stored internally.
        // Internally stored: 1 -> 2, azimuth 225, inclination -10.
        // As taken: from 2 to 1, azimuth 45, inclination 10.
        SurvexExporter survexExporter = new SurvexExporter();
        Survey survey = BasicTestSurveyCreator.createEmptySurvey();
        SurveyUpdater.updateWithNewStation(survey, new Leg(5, 225, -10, true));

        String content = survexExporter.getContent(survey);

        Assert.assertTrue(content.contains("2\t1\t5.000\t45.00\t10.00"));
        Assert.assertFalse(content.contains("1\t2\t5.000\t225.00\t-10.00"));
    }

    @Test
    public void testBasicExportWithPromotedLegs() {
        // Survex can average repeat legs between the same station pair itself, so a promoted
        // leg is now exported as its raw readings, as real data lines - no averaged summary
        // line, and no comments.
        SurvexExporter survexExporter = new SurvexExporter();
        Survey oneNorth = BasicTestSurveyCreator.createStraightNorthThroughRepeats();
        String content = survexExporter.getContent(oneNorth);

        Assert.assertFalse(content.contains(";1\t2"));
        long matchingLines =
                Arrays.stream(content.split("\n"))
                        .filter(line -> line.startsWith("1\t2\t5.000\t0.00\t0.00"))
                        .count();
        Assert.assertEquals(3, matchingLines);
    }

    @Test
    public void testPromotedLegCommentGoesOnFirstRawLine() {
        // The leg's own comment has no single "main" line to sit on any more, now that each
        // raw reading is written as its own real line - it goes on the first one.
        SurvexExporter survexExporter = new SurvexExporter();
        Survey survey = BasicTestSurveyCreator.createStraightNorthThroughRepeats();
        Leg promoted = survey.getOrigin().getConnectedOnwardLegs().get(0);
        promoted.setComment("Big Sandy Chamber");

        String content = survexExporter.getContent(survey);
        String[] matchingLines =
                Arrays.stream(content.split("\n"))
                        .filter(line -> line.startsWith("1\t2"))
                        .toArray(String[]::new);

        Assert.assertEquals(3, matchingLines.length);
        Assert.assertTrue(matchingLines[0].endsWith("Big Sandy Chamber"));
        Assert.assertFalse(matchingLines[1].contains("Big Sandy Chamber"));
        Assert.assertFalse(matchingLines[2].contains("Big Sandy Chamber"));
    }

    @Test
    public void testPromotedLegCommentAndFirstRawReadingCommentAreCombined() {
        // Rare edge case: the leg's own comment and its first raw reading's own comment both
        // want the one trailing-comment slot the first line has - they're joined with " :: ",
        // matching the convention already used for merging passage and leg comments on import.
        Survey survey = new Survey();
        Station destination = new Station("2");
        Leg rawReading1 = new Leg(5, 0, 0);
        rawReading1.setComment("cold draught");
        Leg rawReading2 = new Leg(5, 0, 0);
        Leg promoted =
                Leg.upgradeSplayToConnectedLeg(
                        rawReading1, destination, new Leg[] {rawReading1, rawReading2});
        promoted.setComment("Big Sandy Chamber");
        survey.getOrigin().addOnwardLeg(promoted);

        String content = new SurvexExporter().getContent(survey);

        Assert.assertTrue(content.contains("Big Sandy Chamber :: cold draught"));
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
    public void testSurvexMetadataSurveyDateUsesDotSeparator() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Trip trip = new Trip();
        trip.setSurveyDate(new java.util.Date(0)); // 1970.01.01
        survey.setTrip(trip);

        String metadata = SurvexTherionUtil.getMetadata(survey, SurveyFormat.SURVEX, "", "");

        Assert.assertTrue(metadata.contains("*date 1970.01.01"));
        Assert.assertFalse(metadata.contains("1970-01-01"));
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
    public void testCreationCommentThenCopyrightOrder() {
        // Regression test: the created-with comment must be the first line inside the
        // survey block, with the copyright/licence line immediately after it.
        SurvexExporter survexExporter = new SurvexExporter();
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Trip trip = new Trip();
        trip.setCopyrightHolder("Caver Jane");
        trip.setLicence("CC BY 4.0");
        survey.setTrip(trip);

        String content = survexExporter.getContent(survey);
        String[] lines = content.split("\n");

        Assert.assertTrue(lines[0].startsWith("*begin "));
        Assert.assertTrue(lines[1].startsWith("; Created with SexyTopo"));

        String expectedCopyrightLine =
                SurvexTherionUtil.getCopyrightLine(survey, SurveyFormat.SURVEX).trim();
        Assert.assertEquals(expectedCopyrightLine, lines[2]);
    }

    @Test
    public void testCreationCommentImmediatelyAfterBeginWhenNoCopyright() {
        // The creation comment's position must not depend on whether a copyright/licence
        // is set.
        SurvexExporter survexExporter = new SurvexExporter();
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Trip trip = new Trip();
        survey.setTrip(trip);

        String content = survexExporter.getContent(survey);
        String[] lines = content.split("\n");

        Assert.assertTrue(lines[0].startsWith("*begin "));
        Assert.assertTrue(lines[1].startsWith("; Created with SexyTopo"));
    }

    @Test
    public void testBlankLineSeparatesHeaderFromMetadata() {
        SurvexExporter survexExporter = new SurvexExporter();
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Trip trip = new Trip();
        trip.setCopyrightHolder("Caver Jane");
        trip.setLicence("CC BY 4.0");
        survey.setTrip(trip);

        String content = survexExporter.getContent(survey);
        String[] lines = content.split("\n");

        // lines[0] = *begin, lines[1] = creation comment, lines[2] = copyright,
        // lines[3] must be blank, separating the header from the metadata block.
        Assert.assertEquals("", lines[3]);
        Assert.assertTrue(lines[4].startsWith("*date "));
    }

    @Test
    public void testNoCopyrightLineWhenTripHasNeitherCopyrightNorLicence() {
        SurvexExporter survexExporter = new SurvexExporter();
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        Trip trip = new Trip();
        survey.setTrip(trip);

        String content = survexExporter.getContent(survey);

        Assert.assertFalse(content.contains("*copyright"));
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

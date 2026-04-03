package org.hwyl.sexytopo.control.io.thirdparty.survextherion;

import android.annotation.SuppressLint;
import org.hwyl.sexytopo.control.util.GraphToListTranslator;
import org.hwyl.sexytopo.model.graph.Direction;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;
import org.hwyl.sexytopo.model.table.TableCol;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SurvexTherionUtil {

    public static final String TRIP_DATE_PATTERN = "yyyy.MM.dd";

    public static String getCreationComment(char commentChar, String versionInfo) {
        String dateOnly = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        return commentChar + " Created with " + versionInfo + " on " + dateOnly;
    }

    public static String getInputText(List<String> th2Files) {
        if (th2Files == null || th2Files.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String filename : th2Files) {
            builder.append("input \"").append(filename).append("\"\n");
        }
        return builder.toString();
    }

    public static String getMetadata(Survey survey, SurveyFormat format,
                                      String teamLines, String exploTeamLines) {
        StringBuilder builder = new StringBuilder();

        Trip trip = survey.getTrip();
        if (trip != null) {
            String marker = format.getCommandChar();
            char commentChar = format.getCommentChar();
            String exploDateKeyword = format.getExplorationDateKeyword();

            // Date
            builder.append(marker).append(formatDate(trip.getDate())).append("\n");

            // Instrument line - commented ONLY if field is empty
            if (trip.hasInstrument()) {
                builder.append(marker).append("instrument inst \"").append(trip.getInstrument()).append("\"\n");
            } else {
                builder.append(commentChar).append(marker).append("instrument inst \"\"\n");
            }

            // Team members
            builder.append(teamLines);

            // Blank line before explo block
            builder.append("\n");

            // Exploration date handling:
            // - If "same as survey" is ticked: use survey date as exploration date
            // - If "same as survey" NOT ticked AND exploration date provided: use exploration date
            // - If "same as survey" NOT ticked AND no date provided: commented placeholder
            boolean sameAsSurvey = trip.isExplorationDateSameAsSurvey();
            Date exploDate = trip.getExplorationDate();

            if (sameAsSurvey) {
                String formattedDate = formatDate(trip.getDate()).substring(5); // Remove "date " prefix
                builder.append(marker).append(exploDateKeyword).append(formattedDate).append("\n");
            } else if (exploDate != null) {
                String formattedExploDate = formatDate(exploDate).substring(5); // Remove "date " prefix
                builder.append(marker).append(exploDateKeyword).append(formattedExploDate).append("\n");
            } else {
                // No specific date provided - output commented placeholder
                builder.append(commentChar).append(marker).append(exploDateKeyword)
                        .append("yyyy.mm.dd\n");
            }

            // Explo-team lines
            builder.append(exploTeamLines);

            // Trip comments block if any
            if (trip.getComments() != null && !trip.getComments().isEmpty()) {
                builder.append("\n");
                builder.append(commentChar).append("Comment from SexyTopo trip information\n");
                builder.append(commentMultiline(trip.getComments(), commentChar));
            }
        }

        return builder.toString();
    }

    public static String getStationCommentsData(Survey survey, SurveyFormat format) {

        StringBuilder builder = new StringBuilder();
        String marker = format.getCommandChar();

        // Collect all stations with comments
        List<Station> stationsWithComments = new ArrayList<>();
        collectStationsWithComments(survey.getOrigin(), stationsWithComments);

        if (stationsWithComments.isEmpty()) {
            return "";
        }

        // Output the data passage block
        builder.append(marker).append("data passage station ignoreall\n");

        for (Station station : stationsWithComments) {
            builder.append(station.getName()).append("\t");
            formatComment(builder, station.getComment());
            builder.append("\n");
        }

        builder.append("\n");

        return builder.toString();
    }

    private static void collectStationsWithComments(Station station, List<Station> result) {
        if (station.hasComment()) {
            result.add(station);
        }
        for (Leg leg : station.getConnectedOnwardLegs()) {
            collectStationsWithComments(leg.getDestination(), result);
        }
    }

    public static String getCentrelineData(Survey survey, SurveyFormat format) {

        GraphToListTranslator graphToListTranslator = new GraphToListTranslator();
        StringBuilder builder = new StringBuilder();

        // Add data declaration line with optional syntax marker
        String marker = format.getCommandChar();
        builder.append(marker).append("data normal from to tape compass clino ignoreall\n");

        // Get chronological list of survey entries
        List<GraphToListTranslator.SurveyListEntry> list =
                graphToListTranslator.toChronoListOfSurveyListEntries(survey);

        // Format each entry
        for (GraphToListTranslator.SurveyListEntry entry : list) {
            formatEntry(builder, entry, format);
            builder.append("\n");
        }

        // Blank line after data block
        builder.append("\n");

        return builder.toString();
    }

    @SuppressLint("SimpleDateFormat")
    private static String formatDate(Date date) {
        DateFormat dateFormat = new SimpleDateFormat(TRIP_DATE_PATTERN);
        String dateString = dateFormat.format(date);
        return "date " + dateString;
    }

    private static String commentMultiline(String raw, char commentChar) {
        StringBuilder builder = new StringBuilder();
        String[] lines = raw.split("\n");
        for (String line : lines) {
            builder.append(commentChar).append(line).append("\n");
        }
        return builder.toString();
    }

    public static void formatEntry(
            StringBuilder builder,
            GraphToListTranslator.SurveyListEntry entry,
            SurveyFormat format) {

        Station from = entry.getFrom();
        String fromName = from.getName();

        Leg leg = entry.getLeg();
        Station to = leg.getDestination();
        String toName = to.getName();

        // Replace splay station name with format-specific syntax
        if (toName.equals("-")) {
            toName = format.getSplayStationName();
        }

        formatField(builder, fromName);
        formatField(builder, toName);
        formatField(builder, TableCol.DISTANCE.format(leg.getDistance(), Locale.UK));
        formatField(builder, TableCol.AZIMUTH.format(leg.getAzimuth(), Locale.UK));
        formatField(builder, TableCol.INCLINATION.format(leg.getInclination(), Locale.UK));

        // Handle promoted legs - put readings on subsequent lines
        if (leg.wasPromoted()) {
            char commentChar = format.getCommentChar();
            Leg[] precursors = leg.getPromotedFrom();
            for (Leg precursor : precursors) {
                builder.append("\n");
                builder.append(commentChar);
                builder.append(fromName).append("\t");
                builder.append(toName).append("\t");
                builder.append(TableCol.DISTANCE.format(precursor.getDistance(), Locale.UK)).append("\t");
                builder.append(TableCol.AZIMUTH.format(precursor.getAzimuth(), Locale.UK)).append("\t");
                builder.append(TableCol.INCLINATION.format(precursor.getInclination(), Locale.UK));
            }
        }
    }

    private static void formatField(StringBuilder builder, Object value) {
        builder.append(value.toString());
        builder.append("\t");
    }

    private static void formatComment(StringBuilder builder, String comment) {
        String formatted = comment.replaceAll("(\\r|\\n|\\r\\n)+", "\\\\n");
        builder.append(formatted);
    }

    public static String getExtendedElevationExtensions(Survey survey, SurveyFormat format) {
        StringBuilder builder = new StringBuilder();
        String marker = format.getCommandChar();
        generateExtendCommandsFromStation(builder, survey.getOrigin(), null, marker);
        return builder.toString();
    }

    private static void generateExtendCommandsFromStation(
            StringBuilder builder, Station station, Direction lastDirection, String marker) {

        Direction currentDirection = station.getExtendedElevationDirection();
        if (lastDirection == null) {
            builder.append(getExtendCommand(station, "start", marker));
        } else if (currentDirection != lastDirection) {
            builder.append(getExtendCommand(station, currentDirection.name().toLowerCase(), marker));
        }

        for (Leg leg : station.getConnectedOnwardLegs()) {
            generateExtendCommandsFromStation(
                    builder, leg.getDestination(), station.getExtendedElevationDirection(), marker);
        }
    }

    private static String getExtendCommand(Station station, String direction, String marker) {
        return marker + "extend " + direction + " " + station.getName() + "\n";
    }
}

package org.hwyl.sexytopo.control.io.thirdparty.survextherion;

import android.annotation.SuppressLint;
import android.text.TextUtils;

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

    public static String getMetadata(Survey survey, char commentChar, SurveyFormat format) {
        StringBuilder builder = new StringBuilder();

        Trip trip = survey.getTrip();
        if (trip != null) {
            String marker = (format == SurveyFormat.SURVEX) ? "*" : "";
            boolean isSurvex = (format == SurveyFormat.SURVEX);

            // Date
            builder.append(marker).append(formatDate(trip.getDate())).append("\n");

            // Instrument line - commented ONLY if field is empty
            if (trip.hasInstrument()) {
                builder.append(marker).append("instrument inst \"").append(trip.getInstrument()).append("\"\n");
            } else {
                builder.append(commentChar).append(marker).append("instrument inst \"\"\n");
            }

            // Team members - ONLY SKIP if they have NO ROLES (empty roles list)
            for (Trip.TeamEntry entry : trip.getTeam()) {
                if (!entry.hasRoles()) {
                    continue;  // Skip team members with no roles selected
                }
                String teamLine = formatMember(entry, format);
                if (teamLine != null) {
                    builder.append(marker).append(teamLine).append("\n");
                }
            }

            // Blank line before explo block
            builder.append("\n");

            // Exploration date handling:
            // - If "same as survey" is ticked: use survey date as exploration date
            // - If "same as survey" NOT ticked AND exploration date provided: use exploration date
            // - If "same as survey" NOT ticked AND no date provided: commented placeholder
            boolean sameAsSurvey = trip.isExplorationDateSameAsSurvey();
            Date exploDate = trip.getExplorationDate();

            if (sameAsSurvey) {
                // Use survey date as exploration date
                String formattedDate = formatDate(trip.getDate()).substring(5); // Remove "date " prefix
                if (isSurvex) {
                    builder.append(marker).append("date explored ").append(formattedDate).append("\n");
                } else {
                    builder.append("explo-date ").append(formattedDate).append("\n");
                }
            } else if (exploDate != null) {
                // User has unchecked "same as survey" AND provided a specific date
                String formattedExploDate = formatDate(exploDate).substring(5); // Remove "date " prefix
                if (isSurvex) {
                    builder.append(marker).append("date explored ").append(formattedExploDate).append("\n");
                } else {
                    builder.append("explo-date ").append(formattedExploDate).append("\n");
                }
            } else {
                // No specific date provided - output commented placeholder
                if (isSurvex) {
                    builder.append(commentChar).append("*date explored yyyy.mm.dd\n");
                } else {
                    builder.append(commentChar).append("explo-date yyyy.mm.dd\n");
                }
            }

            // Explo-team lines for Therion (not commented) - only for members with roles
            if (!isSurvex) {
                for (Trip.TeamEntry entry : trip.getTeam()) {
                    if (entry.hasRoles() && hasExplorerRole(entry)) {
                        builder.append("explo-team \"").append(entry.name).append("\"\n");
                    }
                }
            }

            // Trip comments block if any
            if (trip.getComments() != null && !trip.getComments().isEmpty()) {
                builder.append("\n");
                builder.append(commentChar).append("Comment from SexyTopo trip information\n");
                builder.append(commentMultiline(trip.getComments(), commentChar));
            }
        }

        return builder.toString();
    }

    private static boolean hasExplorerRole(Trip.TeamEntry entry) {
        for (Trip.Role role : entry.roles) {
            if (role == Trip.Role.EXPLORATION) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasOnlyExplorerRole(Trip.TeamEntry entry) {
        return entry.roles.size() == 1 && entry.roles.contains(Trip.Role.EXPLORATION);
    }

    public static String getStationCommentsData(Survey survey, SurveyFormat format) {

        StringBuilder builder = new StringBuilder();
        String marker = (format == SurveyFormat.SURVEX) ? "*" : "";

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

    public static String getCentrelineData(
            Survey survey,
            char commentChar,
            SurveyFormat format) {

        GraphToListTranslator graphToListTranslator = new GraphToListTranslator();
        StringBuilder builder = new StringBuilder();

        // Add data declaration line with optional syntax marker
        String marker = (format == SurveyFormat.SURVEX) ? "*" : "";
        builder.append(marker).append("data normal from to tape compass clino ignoreall\n");

        // Get chronological list of survey entries
        List<GraphToListTranslator.SurveyListEntry> list =
                graphToListTranslator.toChronoListOfSurveyListEntries(survey);

        // Format each entry
        for (GraphToListTranslator.SurveyListEntry entry : list) {
            formatEntry(builder, entry, commentChar, format);
            builder.append("\n");
        }

        // Blank line after data block
        builder.append("\n");

        return builder.toString();
    }

    private static String formatMember(Trip.TeamEntry member, SurveyFormat format) {
        boolean isSurvex = (format == SurveyFormat.SURVEX);
        // For Therion, skip team members who only have EXPLORATION role
        if (!isSurvex && hasOnlyExplorerRole(member)) {
            return null;
        }

        List<String> fields = new ArrayList<>();
        fields.add("team");
        fields.add("\"" + member.name + "\"");
        for (Trip.Role role : member.roles) {
            String roleDesc = getRoleDescription(role, format);
            if (roleDesc != null) {
                fields.add(roleDesc);
            }
        }
        return TextUtils.join(" ", fields);
    }

    private static String getRoleDescription(Trip.Role role, SurveyFormat format) {
        switch(role) {
            case BOOK:
                return "notes";
            case INSTRUMENTS:
                return "instruments";
            case EXPLORATION:
                // Survex can use "explorer", Therion cannot (uses explo-team separately)
                return (format == SurveyFormat.SURVEX) ? "explorer" : null;
            case DOG:
            default:
                return "assistant";
        }
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
            char commentChar,
            SurveyFormat format) {

        Station from = entry.getFrom();
        String fromName = from.getName();

        Leg leg = entry.getLeg();
        Station to = leg.getDestination();
        String toName = to.getName();

        // Replace "-" with ".." for Survex splay syntax
        if (format == SurveyFormat.SURVEX && toName.equals("-")) {
            toName = "..";
        }

        formatField(builder, fromName);
        formatField(builder, toName);
        formatField(builder, TableCol.DISTANCE.format(leg.getDistance(), Locale.UK));
        formatField(builder, TableCol.AZIMUTH.format(leg.getAzimuth(), Locale.UK));
        formatField(builder, TableCol.INCLINATION.format(leg.getInclination(), Locale.UK));

        // Handle promoted legs - put readings on subsequent lines
        if (leg.wasPromoted()) {
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
        String marker = (format == SurveyFormat.SURVEX) ? "*" : "";
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

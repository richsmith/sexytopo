package org.hwyl.sexytopo.control.io.thirdparty.therion;

import android.text.TextUtils;

import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionUtil;
import org.hwyl.sexytopo.control.util.GraphToListTranslator;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.graph.Direction;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class ThExporter {

    public static final char COMMENT_CHAR = '#';
    public static final String DATE_PATTERN = "yyyy.MM.dd";


    public static String getContent(Survey survey, List<String> th2Files) {

        String text =
            TherionExporter.getEncodingText() + "\n" +
            getSurveyText(survey, th2Files) + "\n";

        return text;
    }

    public static String updateOriginalContent(
            Survey survey, String originalFileContent, List<String> th2Files) {
        String centrelineText = getCentrelineText(survey);
        String newContent = replaceCentreline(originalFileContent, centrelineText);

        String inputText = getInputText(th2Files);
        newContent = replaceInputsText(newContent, inputText);

        return newContent;
    }

    public static String replaceCentreline(String original, String replacementText) {
        String newContent = original.replaceFirst(
                "(?s)(\\s*?(centreline|centerline)(.*)(endcentreline|endcenterline)\\s*)",
                replacementText);
        return newContent;
    }

    public static String replaceInputsText(String original, String replacementText) {
        String newContent = original.replaceFirst("(?m)(^input .*\\n)+", replacementText);
        return newContent;
    }

    private static String getSurveyText(Survey survey, List<String> th2Files) {

        String surveyText =
            "survey " + survey.getName() + "\n\n" +
            getInputText(th2Files) + "\n\n" +
            indent(getCentrelineText(survey)) +
            "\n\nendsurvey";
        return surveyText;
    }

    private static String getInputText(List<String> th2Files) {
        List<String> lines = new ArrayList<>();
        for (String filename: th2Files) {
            lines.add("input \"" + filename + "\"");
        }
        return TextTools.join("\n", lines);
    }

    private static String getCentrelineText(Survey survey) {
        String centrelineText =
            "\ncentreline\n" +
            indent(getCentreline(survey)) + "\n\n" +
            indent(getExtendedElevationExtensions(survey)) + "\n\n" +
            "endcentreline\n";
        return centrelineText;
    }

    public static String indent(String text) {
        String indented = "";

        if (text.trim().equals("")) {
            return "";
        }

        String[] lines = text.split("\n");
        for (String line : lines) {
            indented += "\t" + line + "\n";
        }
        return indented;
    }

    private static String getCentreline(Survey survey) {

        GraphToListTranslator graphToListTranslator = new GraphToListTranslator();

        StringBuilder builder = new StringBuilder();

        Trip trip = survey.getTrip();
        if (trip != null) {
            builder.append(formatTrip(trip));
        }

        builder.append("data normal from to length compass clino\n\n");

        List<GraphToListTranslator.SurveyListEntry> list =
                graphToListTranslator.toListOfSurveyListEntries(survey);

        for (GraphToListTranslator.SurveyListEntry entry : list) {
            SurvexTherionUtil.formatEntry(builder, entry, COMMENT_CHAR);
            builder.append("\n");
        }

        return builder.toString();
    }


    private static String formatTrip(Trip trip) {

        StringBuilder builder = new StringBuilder();

        if (trip.getComments().length() > 0) {
            builder.append(commentMultiline(trip.getComments()) + "\n\n");
        }

        builder.append(formatDate(trip.getDate()) + "\n\n");

        for (Trip.TeamEntry entry : trip.getTeam()) {
            builder.append(formatMember(entry) + "\n");
        }
        return builder.toString();
    }

    private static String formatMember(Trip.TeamEntry member) {
        List<String> fields = new ArrayList<>();
        fields.add("team");
        fields.add("\"" + member.name + "\"");
        for (Trip.Role role : member.roles) {
            fields.add(getRoleDescription(role));
        }
        return TextUtils.join(" ", fields);
    }

    private static String getRoleDescription(Trip.Role role) {
        switch(role) {
            case BOOK:
                return "book";
            case INSTRUMENTS:
                return "instruments";
            case DOG:
                return "assistant";
            case EXPLORATION:
            default:
                return "explo-team";
        }
    }

    private static String commentMultiline(String raw) {
        StringBuilder builder = new StringBuilder();
        String[] lines = raw.split("\n");
        for (String line : lines) {
            builder.append("# " + line + "\n");
        }
        return builder.toString();
    }


    private static String formatDate(Date date) {

        DateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
        String dateString = dateFormat.format(date);
        return "date " + dateString;
    }

    private static String getExtendedElevationExtensions(Survey survey) {
        StringBuilder builder = new StringBuilder();
        generateExtendCommandsFromStation(builder, survey.getOrigin(), null);
        return builder.toString();
    }

    private static void generateExtendCommandsFromStation(
            StringBuilder builder, Station station, Direction lastDirection) {

        Direction currentDirection = station.getExtendedElevationDirection();
        if (lastDirection == null) {
            builder.append(getExtendCommand(station, "start"));
        } else if (currentDirection != lastDirection) {
            builder.append(getExtendCommand(station, currentDirection.name().toLowerCase()));
        }

        for (Leg leg : station.getConnectedOnwardLegs()) {
            generateExtendCommandsFromStation(
                    builder, leg.getDestination(), station.getExtendedElevationDirection());
        }
    }

    private static String getExtendCommand(Station station, String direction) {
        return "extend " + direction + " " + station.getName() + "\n";
    }

}

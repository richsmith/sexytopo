package org.hwyl.sexytopo.control.io.thirdparty.therion;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;

import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.activity.SexyTopoActivity;
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


@SuppressWarnings("UnnecessaryLocalVariable")
public class ThExporter {

    public static final String TRIP_DATE_PATTERN = "yyyy.MM.dd";


    public static String getContent(Context context, Survey survey, List<String> th2Files) {
        String text =
            TherionExporter.getEncodingText() + "\n" +
            getSurveyText(context, survey, th2Files) + "\n";

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

    private static String getSurveyText(Context context, Survey survey, List<String> th2Files) {

        String attribution = "created with " +
                SexyTopoConstants.APP_NAME + " " + SexyTopoActivity.getVersionName(context);

        String surveyText =
            TherionExporter.COMMENT_CHAR + " " + attribution + "\n\n" +
            "survey " + survey.getName() + "\n\n" +
            getInputText(th2Files) + "\n\n" +
            indent(getCentrelineText(survey)) + "\n\n" +
            "endsurvey";
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
            "\ncentreline\n\n" +
            indent(getCentreline(survey)) + "\n\n" +
            indent(getExtendedElevationExtensions(survey)) + "\n\n" +
            "endcentreline\n";
        return centrelineText;
    }

    public static String indent(String text) {
        StringBuilder indented = new StringBuilder();

        if (text.trim().equals("")) {
            return "";
        }

        String[] lines = text.split("\n");
        for (String line : lines) {
            indented.append("\t").append(line).append("\n");
        }
        return indented.toString();
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
                graphToListTranslator.toChronoListOfSurveyListEntries(survey);

        for (GraphToListTranslator.SurveyListEntry entry : list) {
            SurvexTherionUtil.formatEntry(builder, entry, TherionExporter.COMMENT_CHAR);
            builder.append("\n");
        }

        return builder.toString();
    }


    private static String formatTrip(Trip trip) {
        StringBuilder builder = new StringBuilder();

        builder.append(formatDate(trip.getDate()));
        builder.append("\n");

        if (!trip.getComments().isEmpty()) {
            builder.append(commentMultiline(trip.getComments()));
        }

        for (Trip.TeamEntry entry : trip.getTeam()) {
            builder.append(formatMember(entry));
            builder.append("\n");
        }
        builder.append("\n");
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
                return "notes";
            case INSTRUMENTS:
                return "instruments";
            case DOG:
            case EXPLORATION: // FIXME Therion actually handles explo-team differently
            default:
                return "assistant";
        }
    }

    private static String commentMultiline(String raw) {
        StringBuilder builder = new StringBuilder();
        String[] lines = raw.split("\n");
        for (String line : lines) {
            builder.append("# ").append(line).append("\n");
        }
        return builder.toString();
    }

    @SuppressLint("SimpleDateFormat")
    private static String formatDate(Date date) {
        DateFormat dateFormat = new SimpleDateFormat(TRIP_DATE_PATTERN);
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

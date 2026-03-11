package org.hwyl.sexytopo.control.io.thirdparty.therion;

import android.content.Context;

import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.activity.SexyTopoActivity;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurveyFormat;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionUtil;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;

import java.util.List;

public class ThExporter {

    public static String getContent(Context context, Survey survey, List<String> th2Files) {
        StringBuilder builder = new StringBuilder();

        // Encoding
        builder.append(TherionExporter.getEncodingText()).append("\n");

        // Survey block
        builder.append("survey ").append(survey.getName()).append("\n");

        // Creation comment
        String versionInfo = SexyTopoConstants.APP_NAME + " " + SexyTopoActivity.getVersionName(context);
        builder.append(SurvexTherionUtil.getCreationComment(
                SurveyFormat.THERION.getCommentChar(), versionInfo)).append("\n\n");

        // Input files
        builder.append(SurvexTherionUtil.getInputText(th2Files)).append("\n\n");

        // Metadata
        String teamLines = "";
        String exploTeamLines = "";
        Trip trip = survey.getTrip();
        if (trip != null) {
            teamLines = formatTeamLines(trip);
            exploTeamLines = formatExploTeamLines(trip);
        }
        builder.append(SurvexTherionUtil.getMetadata(
                survey, SurveyFormat.THERION,
                teamLines, exploTeamLines)).append("\n");

        // Centreline block
        builder.append("centreline\n");
        builder.append(SurvexTherionUtil.getStationCommentsData(survey, SurveyFormat.THERION));
        builder.append(SurvexTherionUtil.getCentrelineData(survey, SurveyFormat.THERION));
        builder.append(SurvexTherionUtil.getExtendedElevationExtensions(survey, SurveyFormat.THERION));
        builder.append("endcentreline\n");

        // End survey
        builder.append("endsurvey\n");

        return builder.toString();
    }

    public static String updateOriginalContent(
            Survey survey, String originalFileContent, List<String> th2Files) {

        // Replace centreline block
        String centrelineText =
            "centreline\n" +
            SurvexTherionUtil.getStationCommentsData(survey, SurveyFormat.THERION) +
            SurvexTherionUtil.getCentrelineData(survey, SurveyFormat.THERION) +
            SurvexTherionUtil.getExtendedElevationExtensions(survey, SurveyFormat.THERION) +
            "endcentreline\n";
        String newContent = replaceCentreline(originalFileContent, centrelineText);

        // Replace input statements
        String inputText = SurvexTherionUtil.getInputText(th2Files);
        newContent = replaceInputsText(newContent, inputText);

        return newContent;
    }

    static String formatTeamLines(Trip trip) {
        StringBuilder builder = new StringBuilder();
        for (Trip.TeamEntry entry : trip.getTeam()) {
            if (!entry.hasRoles()) {
                continue;
            }
            if (hasOnlyExplorerRole(entry)) {
                continue;
            }
            builder.append("team \"").append(entry.name).append("\"");
            for (Trip.Role role : entry.roles) {
                if (role != Trip.Role.EXPLORATION) {
                    builder.append(" ").append(getRoleDescription(role));
                }
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    static String formatExploTeamLines(Trip trip) {
        StringBuilder builder = new StringBuilder();
        for (Trip.TeamEntry entry : trip.getTeam()) {
            if (entry.hasRoles() && hasExplorerRole(entry)) {
                builder.append("explo-team \"").append(entry.name).append("\"\n");
            }
        }
        return builder.toString();
    }

    private static String getRoleDescription(Trip.Role role) {
        switch (role) {
            case BOOK: return "notes";
            case INSTRUMENTS: return "instruments";
            case DOG: default: return "assistant";
        }
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
        return entry.roles.size() == 1
                && entry.roles.contains(Trip.Role.EXPLORATION);
    }

    static String replaceCentreline(String original, String replacementText) {
        return original.replaceFirst(
                "(?s)(\\s*?(centreline|centerline)(.*)(endcentreline|endcenterline)\\s*)",
                replacementText);
    }

    static String replaceInputsText(String original, String replacementText) {
        return original.replaceFirst("(?m)(^input .*\\n)+", replacementText);
    }

}

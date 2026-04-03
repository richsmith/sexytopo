package org.hwyl.sexytopo.control.io.thirdparty.survex;

import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurveyFormat;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionUtil;
import org.hwyl.sexytopo.control.io.translation.SingleFileExporter;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;

public class SurvexExporter extends SingleFileExporter {

    public String getContent(Survey survey) {
        StringBuilder builder = new StringBuilder();

        String teamLines = "";
        Trip trip = survey.getTrip();
        if (trip != null) {
            teamLines = formatTeamLines(trip);
        }

        // Begin survey block
        builder.append("*begin ").append(survey.getName()).append("\n");

        // Creation comment (no version info available without Context)
        builder.append(SurvexTherionUtil.getCreationComment(
                SurveyFormat.SURVEX.getCommentChar(), "SexyTopo")).append("\n\n");

        // Metadata
        builder.append(SurvexTherionUtil.getMetadata(
                survey, SurveyFormat.SURVEX, teamLines, "")).append("\n");

        // Station comments data block
        builder.append(SurvexTherionUtil.getStationCommentsData(survey, SurveyFormat.SURVEX));

        // Centreline data
        builder.append(SurvexTherionUtil.getCentrelineData(survey, SurveyFormat.SURVEX));

        // Extended elevation
        builder.append("\n");
        builder.append(SurvexTherionUtil.getExtendedElevationExtensions(survey, SurveyFormat.SURVEX));

        // End survey block
        builder.append("*end ").append(survey.getName()).append("\n");

        return builder.toString();
    }

    @Override
    public String getFileExtension() {
        return "svx";
    }

    @Override
    public String getExportTypeName(Context context) {
        return context.getString(R.string.third_party_survex);
    }

    public String getMimeType() {
        return "text/svx";
    }

    @Override
    public String getExportDirectoryName() {
        return "survex";
    }

    static String formatTeamLines(Trip trip) {
        StringBuilder builder = new StringBuilder();
        for (Trip.TeamEntry entry : trip.getTeam()) {
            if (!entry.hasRoles()) {
                continue;
            }
            builder.append("*team \"").append(entry.name).append("\"");
            for (Trip.Role role : entry.roles) {
                builder.append(" ").append(getRoleDescription(role));
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    private static String getRoleDescription(Trip.Role role) {
        switch (role) {
            case BOOK: return "notes";
            case INSTRUMENTS: return "instruments";
            case EXPLORATION: return "explorer";
            case DOG: default: return "assistant";
        }
    }
}

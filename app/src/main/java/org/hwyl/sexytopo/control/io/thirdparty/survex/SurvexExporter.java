package org.hwyl.sexytopo.control.io.thirdparty.survex;

import android.content.Context;
import java.io.IOException;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.SurveyFile;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionUtil;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurveyFormat;
import org.hwyl.sexytopo.control.io.translation.SingleFileExporter;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;

public class SurvexExporter extends SingleFileExporter {

    static final String ESPEC_EXTENSION = "espec";
    static final String ESPEC_MIME_TYPE = "application/octet-stream";

    @Override
    public void run(Context context, Survey survey) throws IOException {
        // Save the main .svx file
        String svxContent = getContent(survey);
        SurveyFile.SurveyFileType svxType =
                new SurveyFile.SurveyFileType(getFileExtension(), getMimeType());
        getOutputFile(svxType).save(context, svxContent);

        // Save the extended elevation specification to a separate .espec file
        String especContent = getEspecContent(survey);
        SurveyFile.SurveyFileType especType =
                new SurveyFile.SurveyFileType(ESPEC_EXTENSION, ESPEC_MIME_TYPE);
        getOutputFile(especType).save(context, especContent);
    }

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
        builder.append(
                        SurvexTherionUtil.getCreationComment(
                                SurveyFormat.SURVEX.getCommentChar(), "SexyTopo"))
                .append("\n\n");

        // Metadata
        builder.append(SurvexTherionUtil.getMetadata(survey, SurveyFormat.SURVEX, teamLines, ""))
                .append("\n");

        // Station comments data block
        builder.append(SurvexTherionUtil.getStationCommentsData(survey, SurveyFormat.SURVEX));

        // Centreline data
        builder.append(SurvexTherionUtil.getCentrelineData(survey, SurveyFormat.SURVEX));

        // End survey block
        builder.append("*end ").append(survey.getName()).append("\n");

        return builder.toString();
    }

    public String getEspecContent(Survey survey) {
        return SurvexTherionUtil.getEspecExtendedElevationExtensions(survey);
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
            case BOOK:
                return "notes";
            case INSTRUMENTS:
                return "instruments";
            case EXPLORATION:
                return "explorer";
            case DOG:
            default:
                return "assistant";
        }
    }
}

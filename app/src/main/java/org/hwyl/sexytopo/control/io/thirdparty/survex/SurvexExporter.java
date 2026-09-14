package org.hwyl.sexytopo.control.io.thirdparty.survex;

import android.content.Context;
import java.io.IOException;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.SurveyFile;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionUtil;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurveyFormat;
import org.hwyl.sexytopo.control.io.translation.SingleFileExporter;
import org.hwyl.sexytopo.model.graph.ExtendedElevationDirection;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
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

        // Only write the .espec file when it contains more than just the *start line —
        // an all-right survey carries no information beyond the default, so omit the file.
        String especContent = getEspecContent(survey);
        if (isEspecContentMeaningful(especContent)) {
            SurveyFile.SurveyFileType especType =
                    new SurveyFile.SurveyFileType(ESPEC_EXTENSION, ESPEC_MIME_TYPE);
            getOutputFile(especType).save(context, especContent);
        }
    }

    /**
     * Returns true if the espec content contains anything beyond the {@code *start} line. Blank
     * lines are ignored. Comment lines (starting with {@code ;}) are treated as substantive — a
     * commented-out {@code ; *evertical} line carries meaningful information and should be
     * preserved. If the only non-blank line is the start line, the content is not meaningful and
     * the file should not be written.
     */
    static boolean isEspecContentMeaningful(String content) {
        int substantiveLineCount = 0;
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            substantiveLineCount++;
            if (substantiveLineCount > 1) {
                return true;
            }
        }
        return false;
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
                .append("\n");

        // Copyright / licence (if the trip has either set)
        builder.append(SurvexTherionUtil.getCopyrightLine(survey, SurveyFormat.SURVEX));
        builder.append("\n");

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
        StringBuilder builder = new StringBuilder();
        // NOTE: the traversal logic here mirrors
        // SurvexTherionUtil.generateExtendCommandsFromStation
        // — if that method changes (e.g. new non-propagating directions) this must be kept in sync.
        generateEspecExtendCommandsFromStation(builder, survey.getOrigin(), null, null);
        return builder.toString();
    }

    private static void generateEspecExtendCommandsFromStation(
            StringBuilder builder,
            Station station,
            Station fromStation,
            ExtendedElevationDirection lastDirection) {

        ExtendedElevationDirection currentDirection = station.getExtendedElevationDirection();

        // A direction that doesn't propagate (e.g. VERTICAL) applies to this leg alone and does
        // not change what the rest of the survey inherits. The two-station form is not supported
        // by the Survex extend tool in .espec files, so it is emitted as a comment to preserve
        // the information without breaking the file.
        ExtendedElevationDirection inheritedDirection;
        if (!currentDirection.propagates()) {
            builder.append(getEspecExtendCommentedCommand(fromStation, station, "evertical"));
            inheritedDirection = lastDirection;
        } else {
            if (lastDirection == null) {
                builder.append(getEspecExtendCommand(station, "start"));
            } else if (currentDirection != lastDirection) {
                String keyword =
                        currentDirection == ExtendedElevationDirection.LEFT ? "eleft" : "eright";
                builder.append(getEspecExtendCommand(station, keyword));
            }
            inheritedDirection = currentDirection;
        }

        for (Leg leg : station.getConnectedOnwardLegs()) {
            generateEspecExtendCommandsFromStation(
                    builder, leg.getDestination(), station, inheritedDirection);
        }
    }

    private static String getEspecExtendCommand(Station station, String keyword) {
        return "*" + keyword + " " + station.getName() + "\n";
    }

    private static String getEspecExtendCommentedCommand(Station from, Station to, String keyword) {
        return "; *" + keyword + " " + from.getName() + " " + to.getName() + "\n";
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

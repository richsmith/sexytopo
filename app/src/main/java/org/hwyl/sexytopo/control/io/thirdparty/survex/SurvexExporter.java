package org.hwyl.sexytopo.control.io.thirdparty.survex;

import android.content.Context;
import java.io.IOException;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.SurveyFile;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionUtil;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurveyFormat;
import org.hwyl.sexytopo.control.io.translation.Exporter;
import org.hwyl.sexytopo.model.geometry.ExtendedElevationDirection;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;

public class SurvexExporter extends Exporter {

    @Override
    public void run(Context context, Survey survey) throws IOException {
        SurveyFile.SurveyFileType svxType =
                new SurveyFile.SurveyFileType(getFileExtension(), getMimeType());
        getOutputFile(svxType).save(context, getContent(survey));

        // Everything is drawn right unless told otherwise, so only write an .espec if needed
        if (hasNonDefaultDirections(survey)) {
            SurveyFile.SurveyFileType especType =
                    new SurveyFile.SurveyFileType(
                            SurvexConstants.ESPEC_EXTENSION, SurvexConstants.ESPEC_MIME_TYPE);
            getOutputFile(especType).save(context, getEspecContent(survey));
        }
    }

    static boolean hasNonDefaultDirections(Survey survey) {
        return survey.getAllStations().stream()
                .anyMatch(
                        station ->
                                station.getExtendedElevationDirection()
                                        != ExtendedElevationDirection.DEFAULT);
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
        // *start only says where to begin; the origin's own direction is compared with the
        // default like any other station's, so a survey that starts left gets an *eleft too
        Station origin = survey.getOrigin();
        builder.append(getEspecExtendCommand(origin, SurvexConstants.ESPEC_START));
        generateEspecExtendCommandsFromStation(
                builder, origin, null, ExtendedElevationDirection.DEFAULT);
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
            builder.append(
                    getEspecExtendCommentedCommand(
                            fromStation, station, SurvexConstants.ESPEC_VERTICAL));
            inheritedDirection = lastDirection;
        } else {
            if (currentDirection != lastDirection) {
                String command =
                        currentDirection == ExtendedElevationDirection.LEFT
                                ? SurvexConstants.ESPEC_LEFT
                                : SurvexConstants.ESPEC_RIGHT;
                builder.append(getEspecExtendCommand(station, command));
            }
            inheritedDirection = currentDirection;
        }

        for (Leg leg : station.getConnectedOnwardLegs()) {
            generateEspecExtendCommandsFromStation(
                    builder, leg.getDestination(), station, inheritedDirection);
        }
    }

    private static String getEspecExtendCommand(Station station, String command) {
        return command + " " + station.getName() + "\n";
    }

    private static String getEspecExtendCommentedCommand(Station from, Station to, String command) {
        return SurvexConstants.ESPEC_COMMENT_PREFIX
                + command
                + " "
                + from.getName()
                + " "
                + to.getName()
                + "\n";
    }

    public String getFileExtension() {
        return SurvexConstants.SVX_EXTENSION;
    }

    @Override
    public String getExportTypeName(Context context) {
        return context.getString(R.string.third_party_survex);
    }

    public String getMimeType() {
        return SurvexConstants.SVX_MIME_TYPE;
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

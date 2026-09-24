package org.hwyl.sexytopo.control.io.thirdparty.survex;

import android.content.Context;
import androidx.documentfile.provider.DocumentFile;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.io.IoUtils;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.graph.ExtendedElevationDirection;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

/** Parses a Survex extended elevation specification (.espec) file and applies it to a survey. */
public class EspecParser {

    private EspecParser() {}

    /**
     * Reads the given .espec file and applies the extend directions to the survey. Stations not
     * mentioned in the file remain at their default direction (RIGHT).
     */
    public static void applyEspec(Context context, DocumentFile especFile, Survey survey)
            throws Exception {
        String text = IoUtils.slurpFile(context, especFile);
        applyEspecText(text, survey);
    }

    /**
     * Parses the given .espec text and applies the extend directions to the survey. Exposed as a
     * separate method to allow testing without Android file I/O.
     */
    static void applyEspecText(String text, Survey survey) {
        try {
            for (String line : text.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                // Only un-comment *evertical lines — these were deliberately commented out
                // because Survex's extend tool doesn't support them yet, but we preserve the
                // information on import. All other commented lines remain skipped.
                if (trimmed.startsWith(";")) {
                    if (trimmed.startsWith(
                            SurvexConstants.ESPEC_COMMENT_PREFIX
                                    + SurvexConstants.ESPEC_VERTICAL)) {
                        trimmed =
                                trimmed.substring(SurvexConstants.ESPEC_COMMENT_PREFIX.length())
                                        .trim();
                    } else {
                        continue;
                    }
                }

                String[] tokens = trimmed.split("\\s+");
                String command = tokens[0].toLowerCase();

                if (command.equals(SurvexConstants.ESPEC_START)) {
                    continue;
                }

                if (command.equals(SurvexConstants.ESPEC_VERTICAL)) {
                    // Two-station form: *evertical <from> <to>
                    // VERTICAL applies only to the destination station — does not propagate.
                    if (tokens.length != 3) {
                        Log.e("espec: skipping malformed evertical line: " + trimmed);
                        continue;
                    }
                    String toStationName = tokens[2];
                    Station toStation = survey.getStationByName(toStationName);
                    if (toStation == null) {
                        Log.e("espec: station not found: " + toStationName);
                        continue;
                    }
                    SurveyUpdater.setExtendedElevationDirection(
                            survey, toStation, ExtendedElevationDirection.VERTICAL);
                    continue;
                }

                // Single-station form: *eleft/*eright <station> — propagates down subtree.
                if (tokens.length != 2) {
                    Log.e("espec: skipping malformed line: " + trimmed);
                    continue;
                }
                String stationName = tokens[1];

                ExtendedElevationDirection direction;
                if (command.equals(SurvexConstants.ESPEC_LEFT)) {
                    direction = ExtendedElevationDirection.LEFT;
                } else if (command.equals(SurvexConstants.ESPEC_RIGHT)) {
                    direction = ExtendedElevationDirection.RIGHT;
                } else {
                    Log.e("espec: unknown command: " + command);
                    continue;
                }

                Station station = survey.getStationByName(stationName);
                if (station == null) {
                    Log.e("espec: station not found: " + stationName);
                    continue;
                }

                SurveyUpdater.setExtendedElevationDirectionOfSubtree(station, direction);
            }
        } catch (Exception exception) {
            Log.e("corrupted espec file: " + exception);
        }
    }
}

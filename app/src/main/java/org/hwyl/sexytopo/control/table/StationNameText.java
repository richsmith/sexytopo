package org.hwyl.sexytopo.control.table;

import android.content.Context;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.util.StationNameRule;
import org.hwyl.sexytopo.control.util.StationNameRule.Action;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

/** Helper and error text for the station name boxes, shared by the station dialogs. */
final class StationNameText {

    private StationNameText() {}

    /** Why a name is refused, or null if it is valid. */
    static CharSequence error(Context context, StationNameRule.Result result) {
        if (result.isValid()) {
            return null;
        }
        switch (result.error) {
            case BLANK:
                return context.getString(R.string.validation_error_cannot_be_blank);
            case RESERVED:
                return context.getString(R.string.validation_error_station_name_reserved);
            case ALREADY_EXISTS:
                return context.getString(
                        R.string.validation_error_station_already_exists, result.name);
            case BREAKS_CONTINUITY:
                return context.getString(
                        R.string.validation_error_station_breaks_continuity, result.name);
            default:
                return context.getString(R.string.validation_error_station_not_found, result.name);
        }
    }

    /** What a name in the existing station box will do. */
    static CharSequence existingHelper(
            Context context,
            Survey survey,
            Station station,
            StationNameRule.Result result,
            boolean isSplay) {
        if (result.action == Action.MOVE) {
            return context.getString(R.string.manual_station_moves_to, result.name);
        }
        if (result.action == Action.RENAME) {
            String text = context.getString(R.string.manual_station_renames_to, result.name);
            return withLinkWarning(context, survey, station, result, text);
        }
        return context.getString(
                isSplay
                        ? R.string.manual_station_splay_helper
                        : R.string.manual_station_existing_helper);
    }

    /**
     * What a name in the new station box will do. When adding, station is null and the name creates
     * it; otherwise it renames the station everywhere.
     */
    static CharSequence newHelper(
            Context context, Survey survey, Station station, StationNameRule.Result result) {
        if (station == null) {
            return context.getString(R.string.manual_station_new_helper);
        }
        if (result.action == Action.RENAME) {
            String text = context.getString(R.string.manual_station_renames_to, result.name);
            return withLinkWarning(context, survey, station, result, text);
        }
        return context.getString(R.string.manual_station_rename_helper);
    }

    /** Adds a warning line when renaming a station that links to another survey. */
    private static CharSequence withLinkWarning(
            Context context,
            Survey survey,
            Station station,
            StationNameRule.Result result,
            String text) {
        if (result.action != Action.RENAME || !survey.hasLinkedSurveys(station)) {
            return text;
        }
        return text
                + "\n"
                + context.getString(R.string.manual_station_linked_warning, station.getName());
    }
}

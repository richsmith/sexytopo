package org.hwyl.sexytopo.control.table;

import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.activity.SexyTopoActivity;
import org.hwyl.sexytopo.control.activity.SurveyEditorActivity;
import org.hwyl.sexytopo.control.components.DialogUtils;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.StationNamer;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.table.LRUD;

/** Utility class for creating and managing leg/station/splay dialogs. */
public class LegDialogs {

    private LegDialogs() {}

    public static void addStation(final SexyTopoActivity activity, final Survey survey) {
        showAddLegDialog(
                activity,
                survey,
                R.layout.leg_edit_dialog_unified,
                R.string.manual_add_station_title,
                false, // not a splay
                false // no LRUDs
                );
    }

    public static void addSplay(final SexyTopoActivity activity, final Survey survey) {
        showAddLegDialog(
                activity,
                survey,
                R.layout.leg_edit_dialog_unified,
                R.string.manual_add_splay_title,
                true, // is a splay
                false // no LRUDs
                );
    }

    /** Unified helper method for showing add leg/station/splay dialogs */
    private static void showAddLegDialog(
            final SexyTopoActivity activity,
            final Survey survey,
            int layoutResId,
            int titleResId,
            boolean isSplay,
            boolean includeLruds) {

        Station activeStation = survey.getActiveStation();
        String defaultToName =
                isSplay ? null : StationNamer.generateNextStationName(survey, activeStation);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        final View dialogView = inflater.inflate(layoutResId, null);

        boolean usingDegMinsSecs = GeneralPreferences.isDegMinsSecsModeOn();
        if (usingDegMinsSecs) {
            dialogView.findViewById(R.id.azimuth_standard).setVisibility(View.GONE);
            dialogView.findViewById(R.id.azimuth_dms_container).setVisibility(View.VISIBLE);
        }

        if (GeneralPreferences.isIncDegMinsSecsModeOn()) {
            dialogView.findViewById(R.id.inclinationLayout).setVisibility(View.GONE);
            dialogView.findViewById(R.id.inclination_dms_container).setVisibility(View.VISIBLE);
        }

        // Hide TO field for splays
        if (isSplay) {
            dialogView.findViewById(R.id.toStationLayout).setVisibility(View.GONE);
            dialogView.findViewById(R.id.toCommentLayout).setVisibility(View.GONE);
        }

        // Create validation form
        final EditLegForm form =
                new EditLegForm(
                        activity, survey, activeStation, defaultToName, isSplay, dialogView);

        builder.setView(dialogView)
                .setTitle(titleResId)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null);

        final AlertDialog dialog = builder.create();
        DialogUtils.showKeyboardOnDisplay(dialog);
        dialog.show();

        DialogUtils.enableValidationOnButton(
                dialog,
                form,
                () -> {
                    // Get the leg with measurements and shot direction from form
                    Leg leg = form.getUpdatedLeg();
                    leg.setComment(form.getUpdatedLegComment());
                    Station fromStation =
                            SurveyUpdater.applyStationName(
                                    survey, activeStation, null, form.getExistingStationResult());
                    fromStation.setComment(form.getUpdatedFromComment());

                    if (isSplay) {
                        // Add splay to from station
                        SurveyUpdater.addLegFromStation(survey, fromStation, leg);
                    } else {
                        // Create new destination station and set it on the leg
                        Station newToStation =
                                SurveyUpdater.applyStationName(
                                        survey, null, null, form.getNewStationResult());

                        String toComment = form.getUpdatedToComment();
                        if (toComment != null) {
                            newToStation.setComment(toComment);
                        }

                        // Reconstruct leg with destination station (preserving backwards flag from
                        // form)
                        String legComment = leg.getComment();
                        leg =
                                new Leg(
                                        leg.getDistance(),
                                        leg.getAzimuth(),
                                        leg.getInclination(),
                                        newToStation,
                                        new Leg[] {},
                                        leg.wasShotBackwards());
                        leg.setComment(legComment);

                        // Add leg to from station using SurveyUpdater
                        // This also sets the active station to the new destination
                        SurveyUpdater.addLegFromStation(survey, fromStation, leg);

                        // Add LRUDs if requested
                        if (includeLruds) {
                            // Restore the active station back to the FROM station
                            survey.setActiveStation(fromStation);

                            LRUD.Mode lrudMode = GeneralPreferences.getLrudMode();
                            createLrudIfPresent(
                                    survey,
                                    fromStation,
                                    dialog,
                                    R.id.editDistanceLeft,
                                    LRUD.LEFT,
                                    lrudMode);
                            createLrudIfPresent(
                                    survey,
                                    fromStation,
                                    dialog,
                                    R.id.editDistanceRight,
                                    LRUD.RIGHT,
                                    lrudMode);
                            createLrudIfPresent(
                                    survey,
                                    fromStation,
                                    dialog,
                                    R.id.editDistanceUp,
                                    LRUD.UP,
                                    lrudMode);
                            createLrudIfPresent(
                                    survey,
                                    fromStation,
                                    dialog,
                                    R.id.editDistanceDown,
                                    LRUD.DOWN,
                                    lrudMode);

                            // Move active station back to the TO station again
                            survey.setActiveStation(newToStation);
                        }
                    }

                    SurveyManager manager = activity.getSurveyManager();
                    manager.broadcastSurveyUpdated();
                    if (!isSplay) {
                        manager.broadcastNewStationCreated();
                    }
                    if (activity instanceof SurveyEditorActivity) {
                        ((SurveyEditorActivity) activity).syncWithSurvey();
                    }
                });
    }

    public static void editLeg(
            final SexyTopoActivity activity,
            final Survey survey,
            final Station fromStation,
            final Leg toEdit) {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.leg_edit_dialog_unified, null);

        boolean usingDegMinsSecs = GeneralPreferences.isDegMinsSecsModeOn();

        if (usingDegMinsSecs) {
            dialogView.findViewById(R.id.azimuth_standard).setVisibility(View.GONE);
            dialogView.findViewById(R.id.azimuth_dms_container).setVisibility(View.VISIBLE);
        }

        if (GeneralPreferences.isIncDegMinsSecsModeOn()) {
            dialogView.findViewById(R.id.inclinationLayout).setVisibility(View.GONE);
            dialogView.findViewById(R.id.inclination_dms_container).setVisibility(View.VISIBLE);
        }

        // Hide to station field for splays
        if (!toEdit.hasDestination()) {
            dialogView.findViewById(R.id.toStationLayout).setVisibility(View.GONE);
            dialogView.findViewById(R.id.toCommentLayout).setVisibility(View.GONE);
        }

        // Create and configure the form for validation
        final EditLegForm form = new EditLegForm(activity, survey, fromStation, toEdit, dialogView);

        builder.setView(dialogView)
                .setTitle(
                        toEdit.hasDestination()
                                ? R.string.manual_edit_leg_title
                                : R.string.manual_edit_splay_title)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null);

        final AlertDialog dialog = builder.create();
        DialogUtils.showKeyboardOnDisplay(dialog);
        dialog.show();

        // Populate fields with current values
        form.showReadings(toEdit.toRecordedReading());

        DialogUtils.enableValidationOnButton(
                dialog,
                form,
                () -> {

                    // Get the updated leg with measurements and shot direction from form
                    Leg edited = form.getUpdatedLeg();
                    edited.setComment(form.getUpdatedLegComment());

                    // Edit the readings first, then the stations of the edited leg
                    SurveyUpdater.editLeg(survey, toEdit, edited);

                    Station newFromStation =
                            SurveyUpdater.applyStationName(
                                    survey, fromStation, edited, form.getExistingStationResult());
                    newFromStation.setComment(form.getUpdatedFromComment());

                    if (edited.hasDestination()) {
                        Station toStation =
                                SurveyUpdater.applyStationName(
                                        survey,
                                        edited.getDestination(),
                                        edited,
                                        form.getNewStationResult());
                        toStation.setComment(form.getUpdatedToComment());
                    }

                    activity.getSurveyManager().broadcastSurveyUpdated();

                    // Sync view with survey data
                    if (activity instanceof SurveyEditorActivity) {
                        ((SurveyEditorActivity) activity).syncWithSurvey();
                    }
                });
    }

    public static void addStationWithLruds(final SexyTopoActivity activity, final Survey survey) {
        showAddLegDialog(
                activity,
                survey,
                R.layout.leg_edit_dialog_unified_with_lruds,
                R.string.manual_add_station_title,
                false, // not a splay
                true // include LRUDs
                );
    }

    private static void createLrudIfPresent(
            Survey survey,
            Station station,
            AlertDialog dialog,
            int fieldId,
            LRUD direction,
            LRUD.Mode lrudMode) {
        Float value = getFieldValue(dialog, fieldId);
        if (value != null) {
            Leg leg = direction.createSplay(survey, station, lrudMode, value);
            SurveyUpdater.update(survey, leg);
        }
    }

    private static Float getFieldValue(AlertDialog dialog, int id) {
        try {
            TextView field = dialog.findViewById(id);
            return Float.parseFloat(field.getText().toString());
        } catch (Exception e) {
            return null;
        }
    }

    public static void renameStation(
            final SurveyEditorActivity activity, final Survey survey, final Station toRename) {
        final RenameStationForm form = new RenameStationForm(activity, survey, toRename);

        Runnable renameAction =
                () -> {
                    try {
                        SurveyUpdater.applyStationName(survey, toRename, null, form.getResult());
                        activity.getSurveyManager().broadcastSurveyUpdated();
                        activity.syncWithSurvey();
                    } catch (Exception e) {
                        activity.showExceptionAndLog(R.string.manual_rename_error, e);
                    }
                };

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.manual_rename_station_title)
                        .setView(form.stationNameLayout)
                        .setPositiveButton(R.string.rename, null)
                        .setNegativeButton(R.string.cancel, null)
                        .create();

        // The keyboard's done key acts as the Rename button
        form.stationName.setOnEditorActionListener(
                (view, actionId, event) ->
                        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick());

        if (dialog.getWindow() != null) {
            dialog.getWindow()
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        dialog.show();
        DialogUtils.enableValidationOnButton(dialog, form, renameAction);
    }
}

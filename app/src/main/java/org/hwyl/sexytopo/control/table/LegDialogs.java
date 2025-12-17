package org.hwyl.sexytopo.control.table;

import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.activity.SexyTopoActivity;
import org.hwyl.sexytopo.control.activity.TableActivity;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.StationNamer;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.table.LRUD;

/**
 * Utility class for creating and managing leg/station/splay dialogs.
 */
public class LegDialogs {

    private LegDialogs() {}


    public static void addStation(final TableActivity tableActivity, final Survey survey) {
        showAddLegDialog(
            tableActivity,
            survey,
            R.layout.leg_edit_dialog_unified,
            R.string.manual_add_station_title,
            false, // not a splay
            false  // no LRUDs
        );
    }



    public static void addSplay(final TableActivity tableActivity, final Survey survey) {
        showAddLegDialog(
            tableActivity,
            survey,
            R.layout.leg_edit_dialog_unified,
            R.string.manual_add_splay_title,
            true, // is a splay
            false // no LRUDs
        );
    }


    /**
     * Unified helper method for showing add leg/station/splay dialogs
     */
    private static void showAddLegDialog(
            final TableActivity tableActivity,
            final Survey survey,
            int layoutResId,
            int titleResId,
            boolean isSplay,
            boolean includeLruds) {

        Station activeStation = survey.getActiveStation();
        String defaultToName = isSplay ? null : StationNamer.generateNextStationName(survey, activeStation);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(tableActivity);
        LayoutInflater inflater = tableActivity.getLayoutInflater();
        final View dialogView = inflater.inflate(layoutResId, null);

        boolean usingDegMinsSecs = GeneralPreferences.isDegMinsSecsModeOn();
        if (usingDegMinsSecs) {
            dialogView.findViewById(R.id.azimuth_standard).setVisibility(View.GONE);
            dialogView.findViewById(R.id.azimuth_deg_mins_secs).setVisibility(View.VISIBLE);
        }

        // Get the views from the dialog
        TextInputLayout fromStationLayout = dialogView.findViewById(R.id.fromStationLayout);
        TextInputEditText fromStationField = dialogView.findViewById(R.id.editFromStation);
        TextInputLayout toStationLayout = dialogView.findViewById(R.id.toStationLayout);
        TextInputEditText toStationField = dialogView.findViewById(R.id.editToStation);
        TextInputEditText distanceField = dialogView.findViewById(R.id.editDistance);
        TextInputEditText azimuthField = dialogView.findViewById(R.id.editAzimuth);
        TextInputEditText inclinationField = dialogView.findViewById(R.id.editInclination);
        TextInputEditText azimuthDegreesField = dialogView.findViewById(R.id.editAzimuthDegrees);
        TextInputEditText azimuthMinutesField = dialogView.findViewById(R.id.editAzimuthMinutes);
        TextInputEditText azimuthSecondsField = dialogView.findViewById(R.id.editAzimuthSeconds);

        // Hide TO field for splays
        if (isSplay) {
            toStationLayout.setVisibility(View.GONE);
        }

        // Create validation form
        final EditLegForm form = new EditLegForm(
            survey, activeStation, defaultToName, isSplay,
            fromStationLayout, fromStationField,
            toStationLayout, toStationField,
            distanceField, azimuthField, inclinationField,
            azimuthDegreesField, azimuthMinutesField, azimuthSecondsField);

        builder
            .setView(dialogView)
            .setTitle(titleResId)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null);

        final AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();

        // Set up validation callback
        form.setOnDidValidateCallback((valid) -> {
            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setEnabled(valid);
            }
        });

        // Run initial validation
        form.validate();

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, tableActivity.getString(R.string.save),
            (dialogInterface, buttonId) -> {
                // Validate form first
                form.validate();
                if (!form.isValid()) {
                    return;
                }

                // Parse measurements using form methods
                float distance = form.getDistance();
                float azimuth = form.getAzimuth();
                float inclination = form.getInclination();

                // Get from station using form method
                Station fromStation = form.getFromStation();

                if (isSplay) {
                    // Create splay (no destination)
                    Leg splay = new Leg(distance, azimuth, inclination);
                    fromStation.getOnwardLegs().add(splay);
                } else {
                    // Get to station name and create new station
                    String toStationName = form.getToStationName();
                    Station newStation = new Station(toStationName);
                    Leg leg = new Leg(distance, azimuth, inclination, newStation, new Leg[]{});

                    // Add leg to from station
                    fromStation.getOnwardLegs().add(leg);

                    // Add LRUDs if requested
                    if (includeLruds) {
                        createLrudIfPresent(survey, fromStation, dialog, R.id.editDistanceLeft, LRUD.LEFT);
                        createLrudIfPresent(survey, fromStation, dialog, R.id.editDistanceRight, LRUD.RIGHT);
                        createLrudIfPresent(survey, fromStation, dialog, R.id.editDistanceUp, LRUD.UP);
                        createLrudIfPresent(survey, fromStation, dialog, R.id.editDistanceDown, LRUD.DOWN);
                    }

                    // Set active station to the new station
                    survey.setActiveStation(newStation);
                }

                dialogInterface.dismiss();

                SurveyManager manager = tableActivity.getSurveyManager();
                manager.broadcastSurveyUpdated();
                if (!isSplay) {
                    manager.broadcastNewStationCreated();
                }
                tableActivity.syncTableWithSurvey();
            });
    }


    public static void editLeg(final SexyTopoActivity activity,
                               final Survey survey,
                               final Station fromStation,
                               final Leg toEdit) {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.leg_edit_dialog_unified, null);

        boolean usingDegMinsSecs = GeneralPreferences.isDegMinsSecsModeOn();

        if (usingDegMinsSecs) {
            dialogView.findViewById(R.id.azimuth_standard).setVisibility(View.GONE);
            dialogView.findViewById(R.id.azimuth_deg_mins_secs).setVisibility(View.VISIBLE);
        }

        // Get the views from the dialog
        TextInputLayout fromStationLayout = dialogView.findViewById(R.id.fromStationLayout);
        TextInputEditText fromStationField = dialogView.findViewById(R.id.editFromStation);
        TextInputLayout toStationLayout = dialogView.findViewById(R.id.toStationLayout);
        TextInputEditText toStationField = dialogView.findViewById(R.id.editToStation);
        TextInputEditText distanceField = dialogView.findViewById(R.id.editDistance);
        TextInputEditText azimuthField = dialogView.findViewById(R.id.editAzimuth);
        TextInputEditText inclinationField = dialogView.findViewById(R.id.editInclination);
        TextInputEditText azimuthDegreesField = dialogView.findViewById(R.id.editAzimuthDegrees);
        TextInputEditText azimuthMinutesField = dialogView.findViewById(R.id.editAzimuthMinutes);
        TextInputEditText azimuthSecondsField = dialogView.findViewById(R.id.editAzimuthSeconds);

        // Create and configure the form for validation
        final EditLegForm form = new EditLegForm(
            survey, fromStation, toEdit,
            fromStationLayout, fromStationField,
            toStationLayout, toStationField,
            distanceField, azimuthField, inclinationField,
            azimuthDegreesField, azimuthMinutesField, azimuthSecondsField);

        // Hide to station field for splays
        if (!toEdit.hasDestination()) {
            toStationLayout.setVisibility(View.GONE);
        }

        builder
            .setView(dialogView)
            .setTitle(toEdit.hasDestination() ? R.string.manual_edit_leg_title : R.string.manual_edit_splay_title)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null);

        final AlertDialog dialog = builder.create();

        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();

        // Populate fields with current values
        Leg editData = toEdit.wasShotBackwards()? toEdit.reverse() : toEdit;
        ((TextView) (dialog.findViewById(R.id.editDistance)))
                .setText(Float.toString(editData.getDistance()));

        if (usingDegMinsSecs) {
            float azimuth = editData.getAzimuth();
            int degrees = (int) azimuth;
            float remainder = azimuth - degrees;
            int minutes = (int) (remainder * 60);
            float seconds = ((remainder * 60) - minutes) * 60;

            ((TextView) (dialog.findViewById(R.id.editAzimuthDegrees)))
                    .setText(Integer.toString(degrees));
            ((TextView) (dialog.findViewById(R.id.editAzimuthMinutes)))
                    .setText(Integer.toString(minutes));
            ((TextView) (dialog.findViewById(R.id.editAzimuthSeconds)))
                    .setText(Float.toString(seconds));
        } else {
            ((TextView) (dialog.findViewById(R.id.editAzimuth)))
                    .setText(Float.toString(editData.getAzimuth()));
        }

        ((TextView) (dialog.findViewById(R.id.editInclination)))
                .setText(Float.toString(editData.getInclination()));

        // Set up validation callback
        form.setOnDidValidateCallback((valid) -> {
            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setEnabled(valid);
            }
        });

        // Run initial validation to enable/disable save button
        form.validate();

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, activity.getString(R.string.save),
            (dialogInterface, buttonId) -> {

                // Validate form first
                form.validate();
                if (!form.isValid()) {
                    return;
                }

                dialogInterface.dismiss();

                // Parse measurements using form methods
                float distance = form.getDistance();
                float azimuth = form.getAzimuth();
                float inclination = form.getInclination();

                // Create the edited leg with new measurements
                Leg edited = new Leg(distance, azimuth, inclination);

                // Preserve destination if it's a full leg
                if (toEdit.hasDestination()) {
                    edited = new Leg(
                        edited.getDistance(),
                        edited.getAzimuth(),
                        edited.getInclination(),
                        toEdit.getDestination(),
                        new Leg[]{});
                }

                // Handle backwards shots
                if (toEdit.wasShotBackwards()) {
                    edited = edited.reverse();
                }

                // Get the new station names from the form
                String newFromStationName = form.getFromStationName();
                String oldFromStationName = fromStation.getName();

                // Apply changes in the correct order:
                // 1. First, edit the leg measurements
                SurveyUpdater.editLeg(survey, toEdit, edited);

                // 2. Move leg if from station changed
                if (!newFromStationName.equals(oldFromStationName)) {
                    Station newFromStation = survey.getStationByName(newFromStationName);
                    SurveyUpdater.moveLeg(survey, edited, newFromStation);
                }

                // 3. Rename destination station if to station name changed (for full legs)
                if (toEdit.hasDestination()) {
                    String newToStationName = form.getToStationName();
                    String oldToStationName = toEdit.getDestination().getName();
                    if (!newToStationName.equals(oldToStationName)) {
                        SurveyUpdater.renameStation(survey, edited.getDestination(), newToStationName);
                    }
                }

                activity.getSurveyManager().broadcastSurveyUpdated();

                // Sync table view if this is a TableActivity
                if (activity instanceof TableActivity) {
                    ((TableActivity) activity).syncTableWithSurvey();
                }
            });
    }


    public static void addStationWithLruds(final TableActivity tableActivity, final Survey survey) {
        showAddLegDialog(
            tableActivity,
            survey,
            R.layout.leg_edit_dialog_unified_with_lruds,
            R.string.manual_add_station_title,
            false, // not a splay
            true   // include LRUDs
        );
    }


    private static void createLrudIfPresent(Survey survey, Station station,
                                            AlertDialog dialog, int fieldId,
                                            LRUD direction) {
        Float value = getFieldValue(dialog, fieldId);
        if (value != null) {
            Leg leg = direction.createSplay(survey, station, value);
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


    public static void renameStation(final TableActivity activity,
                                     final Survey survey, final Station toRename) {
        final RenameStationForm form = new RenameStationForm(activity, survey, toRename);

        Runnable renameAction = () -> {
            String newName = form.stationName.getText().toString();
            try {
                SurveyUpdater.renameStation(survey, toRename, newName);
                activity.syncTableWithSurvey();
            } catch (Exception e) {
                activity.showExceptionAndLog(R.string.manual_rename_error, e);
            }
        };

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.manual_rename_station_title)
            .setView(form.stationNameLayout)
            .setPositiveButton(R.string.rename, (ignore, buttonId) -> renameAction.run())
            .setNegativeButton(R.string.cancel, null)
            .create();

        form.stationName.setOnEditorActionListener((view, actionId, event) -> {
            form.validate();

            if (form.isValid()) {
                renameAction.run();
                dialog.dismiss();
                return true;
            } else {
                return false;
            }
        });

        form.setOnDidValidateCallback((valid) -> {
            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setEnabled(valid);
        });

        dialog.getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();
    }

}

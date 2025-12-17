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
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.table.LRUD;

/**
 * Handles manual data entry in the TableActivity. Revolves around creating and reacting to dialogs.
 */
public class ManualEntry {

    private ManualEntry() {}


    public static void addStation(final TableActivity tableActivity, final Survey survey) {
        AlertDialog dialog = createDialog(R.layout.leg_edit_dialog,
            tableActivity,
            (leg, ignore) -> {
                SurveyUpdater.updateWithNewStation(survey, leg);
                SurveyManager manager = tableActivity.getSurveyManager();
                manager.broadcastSurveyUpdated();
                manager.broadcastNewStationCreated();
                tableActivity.syncTableWithSurvey();
            });
        dialog.setTitle(tableActivity.getString(R.string.manual_add_station_title));
    }



    public static void addSplay(final TableActivity tableActivity, final Survey survey) {
        AlertDialog dialog = createDialog(R.layout.leg_edit_dialog,
            tableActivity,
            (leg, ignore) -> {
                SurveyUpdater.update(survey, leg);
                tableActivity.getSurveyManager().broadcastSurveyUpdated();
                tableActivity.syncTableWithSurvey();
            });
        dialog.setTitle(tableActivity.getString(R.string.manual_add_splay_title));
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

        // Create and configure the form for validation
        final EditLegForm form = new EditLegForm(
            survey, fromStation, toEdit,
            fromStationLayout, fromStationField,
            toStationLayout, toStationField,
            distanceField, azimuthField, inclinationField);

        // Hide to station field for splays
        if (!toEdit.hasDestination()) {
            toStationLayout.setVisibility(View.GONE);
        }

        builder
            .setView(dialogView)
            .setTitle(R.string.manual_edit_leg_title)
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

                // Parse leg measurements
                Float distance = getFieldValue(dialog, R.id.editDistance);
                Float inclination = getFieldValue(dialog, R.id.editInclination);

                Float azimuth;
                if (usingDegMinsSecs) {
                    Float degrees = getFieldValue(dialog, R.id.editAzimuthDegrees);
                    Float minutes = getFieldValue(dialog, R.id.editAzimuthMinutes);
                    Float seconds = getFieldValue(dialog, R.id.editAzimuthSeconds);
                    if (degrees == null || minutes == null || seconds == null) {
                        azimuth = null;
                    } else {
                        azimuth =
                            degrees +
                                (minutes * (1.0f / 60.0f)) +
                                (seconds * (1.0f / 60.0f) * (1.0f / 60.0f));
                    }
                } else {
                    azimuth = getFieldValue(dialog, R.id.editAzimuth);
                }

                // Validate measurements
                if (distance == null || !Leg.isDistanceLegal(distance)) {
                    TextView editDistance = dialogView.findViewById(R.id.editDistance);
                    String errorMessage =
                            activity.getString(R.string.manual_edit_distance_error);
                    editDistance.setError(errorMessage);
                    activity.showSimpleToast(errorMessage);
                } else if (azimuth == null || !Leg.isAzimuthLegal(azimuth)) {
                    String errorMessage =
                            activity.getString(R.string.manual_edit_azimuth_error);
                    TextView editAzimuth = dialogView.findViewById(R.id.editAzimuth);
                    activity.showSimpleToast(errorMessage);
                    editAzimuth.setError(errorMessage);
                } else if (inclination == null || !Leg.isInclinationLegal(inclination)) {
                    String errorMessage =
                            activity.getString(R.string.manual_edit_inclination_error);
                    TextView editInclination = dialogView.findViewById(R.id.editInclination);
                    editInclination.setError(errorMessage);
                    activity.showSimpleToast(errorMessage);
                } else {
                    dialogInterface.dismiss();

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
                }
            });
    }


    public static void addStationWithLruds(final TableActivity tableActivity, final Survey survey) {

        AlertDialog dialog = createDialog(R.layout.leg_edit_dialog_with_lruds,
            tableActivity,
            (leg, theDialog) -> {
                Station station = survey.getActiveStation();
                SurveyUpdater.updateWithNewStation(survey, leg);
                Station newActiveStation = survey.getActiveStation();
                survey.setActiveStation(station);
                createLrudIfPresent(survey, station, theDialog, R.id.editDistanceLeft, LRUD.LEFT);
                createLrudIfPresent(survey, station, theDialog, R.id.editDistanceRight, LRUD.RIGHT);
                createLrudIfPresent(survey, station, theDialog, R.id.editDistanceUp, LRUD.UP);
                createLrudIfPresent(survey, station, theDialog, R.id.editDistanceDown, LRUD.DOWN);
                survey.setActiveStation(newActiveStation);
                tableActivity.getSurveyManager().broadcastSurveyUpdated();
                tableActivity.syncTableWithSurvey();
            });
        dialog.setTitle(tableActivity.getString(R.string.manual_add_station_title));
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


    private static AlertDialog createDialog(
            int layoutId,
            final TableActivity tableActivity,
            final EditCallback editCallback) {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(tableActivity);

        LayoutInflater inflater = tableActivity.getLayoutInflater();
        final View dialogView = inflater.inflate(layoutId, null);

        boolean usingDegMinsSecs = GeneralPreferences.isDegMinsSecsModeOn();

        if (usingDegMinsSecs) {
            dialogView.findViewById(R.id.azimuth_standard).setVisibility(View.GONE);
            dialogView.findViewById(R.id.azimuth_deg_mins_secs).setVisibility(View.VISIBLE);
        }
        builder
            .setView(dialogView)
            .setTitle(R.string.manual_add_station_title)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null);

        final AlertDialog dialog = builder.create();

        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, tableActivity.getString(R.string.save),
            (dialogInterface, buttonId) -> {

                Float distance = getFieldValue(dialog, R.id.editDistance);
                Float inclination = getFieldValue(dialog, R.id.editInclination);

                Float azimuth;
                if (usingDegMinsSecs) {
                    Float degrees = getFieldValue(dialog, R.id.editAzimuthDegrees);
                    Float minutes = getFieldValue(dialog, R.id.editAzimuthMinutes);
                    Float seconds = getFieldValue(dialog, R.id.editAzimuthSeconds);
                    if (degrees == null || minutes == null || seconds == null) {
                        azimuth = null;
                    } else {
                        azimuth =
                            degrees +
                                (minutes * (1.0f / 60.0f)) +
                                (seconds * (1.0f / 60.0f) * (1.0f / 60.0f));
                    }
                } else {
                    azimuth = getFieldValue(dialog, R.id.editAzimuth);
                }

                if (distance == null || !Leg.isDistanceLegal(distance)) {
                    TextView editDistance = dialogView.findViewById(R.id.editDistance);
                    String errorMessage =
                            tableActivity.getString(R.string.manual_edit_distance_error);
                    editDistance.setError(errorMessage);
                    tableActivity.showSimpleToast(errorMessage);
                } else if (azimuth == null || !Leg.isAzimuthLegal(azimuth)) {
                    String errorMessage =
                            tableActivity.getString(R.string.manual_edit_azimuth_error);
                    TextView editAzimuth = dialogView.findViewById(R.id.editAzimuth);
                    tableActivity.showSimpleToast(errorMessage);
                    editAzimuth.setError(errorMessage);
                } else if (inclination == null || !Leg.isInclinationLegal(inclination)) {
                    String errorMessage =
                            tableActivity.getString(R.string.manual_edit_inclination_error);
                    TextView editInclination = dialogView.findViewById(R.id.editInclination);
                    editInclination.setError(errorMessage);
                    tableActivity.showSimpleToast(errorMessage);
                } else {
                    dialogInterface.dismiss();
                    Leg leg = new Leg(distance, azimuth, inclination);
                    editCallback.submit(leg, dialog);
                }
            });

        return dialog;

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


    public interface EditCallback {
        void submit(Leg leg, AlertDialog dialog);
    }

}

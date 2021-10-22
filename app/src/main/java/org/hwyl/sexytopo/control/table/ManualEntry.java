package org.hwyl.sexytopo.control.table;

import android.app.Dialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.activity.TableActivity;
import org.hwyl.sexytopo.control.util.PreferenceHelper;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.databinding.DialogEditTextBinding;
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
        final AlertDialog dialog = createDialog(R.layout.leg_edit_dialog,
            tableActivity,
            (leg, ignore) -> {
                SurveyUpdater.updateWithNewStation(survey, leg);
                SurveyManager manager = tableActivity.getSurveyManager();
                manager.broadcastSurveyUpdated();
                manager.broadcastNewStationCreated();
                tableActivity.syncTableWithSurvey();
            });
        dialog.setTitle(R.string.manual_add_station_title);
    }



    public static void addSplay(final TableActivity tableActivity, final Survey survey) {
        final AlertDialog dialog = createDialog(R.layout.leg_edit_dialog,
            tableActivity,
            (leg, ignore) -> {
                SurveyUpdater.update(survey, leg);
                tableActivity.getSurveyManager().broadcastSurveyUpdated();
                tableActivity.syncTableWithSurvey();
            });
        dialog.setTitle(R.string.manual_add_splay_title);
    }


    public static void editLeg(final TableActivity tableActivity,
                               final Survey survey,
                               final Leg toEdit) {

        final AlertDialog dialog = createDialog(R.layout.leg_edit_dialog,
            tableActivity,
            (edited, ignore) -> {
                    if (toEdit.hasDestination()) {
                        edited = new Leg(
                            edited.getDistance(),
                            edited.getAzimuth(),
                            edited.getInclination(),
                            toEdit.getDestination(),
                            new Leg[]{});
                    }
                    if (toEdit.wasShotBackwards()) {
                        edited = edited.reverse();
                    }
                    SurveyUpdater.editLeg(survey, toEdit, edited);
                    tableActivity.getSurveyManager().broadcastSurveyUpdated();
                    tableActivity.syncTableWithSurvey();
            });

        dialog.setTitle(R.string.manual_edit_leg_title);

        Leg editData = toEdit.wasShotBackwards()? toEdit.reverse() : toEdit;
        ((TextView) (dialog.findViewById(R.id.editDistance)))
                .setText(Float.toString(editData.getDistance()));
        ((TextView) (dialog.findViewById(R.id.editAzimuth)))
                .setText(Float.toString(editData.getAzimuth()));
        ((TextView) (dialog.findViewById(R.id.editInclination)))
                .setText(Float.toString(editData.getInclination()));
    }


    public static void addStationWithLruds(final TableActivity tableActivity, final Survey survey) {

        final AlertDialog dialog = createDialog(R.layout.leg_edit_dialog_with_lruds,
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
        dialog.setTitle(R.string.manual_add_station_title);
    }


    private static void createLrudIfPresent(Survey survey, Station station,
                                            Dialog dialog, int fieldId,
                                            LRUD direction) {
        Float value = getFieldValue(dialog, fieldId);
        if (value != null) {
            Leg leg = direction.createSplay(survey, station, value);
            SurveyUpdater.update(survey, leg);
        }

    }


    private static Float getFieldValue(Dialog dialog, int id) {
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

        LayoutInflater inflater = tableActivity.getLayoutInflater();
        final View dialogView = inflater.inflate(layoutId, null);

        if (PreferenceHelper.degreesMinutesSeconds()) {
            dialogView.findViewById(R.id.azimuth_standard).setVisibility(View.GONE);
            dialogView.findViewById(R.id.azimuth_deg_mins_secs).setVisibility(View.VISIBLE);
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(tableActivity)
            .setView(dialogView)
            .setTitle(R.string.manual_add_station_title)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(android.R.string.cancel, null);

        final AlertDialog dialog = builder.create();

        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Save",
                (dialogInterface, buttonId) -> {

                    Float distance = getFieldValue(dialog, R.id.editDistance);
                    Float inclination = getFieldValue(dialog, R.id.editInclination);

                    Float azimuth;
                    if (PreferenceHelper.degreesMinutesSeconds()) {
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
                        editDistance.setError("Bad distance");
                        tableActivity.showSimpleToast("Bad distance");
                    } else if (azimuth == null || !Leg.isAzimuthLegal(azimuth)) {
                        TextView editAzimuth = dialogView.findViewById(R.id.editAzimuth);
                        tableActivity.showSimpleToast("Bad azimuth");
                        editAzimuth.setError("Bad azimuth");
                    } else if (inclination == null || !Leg.isInclinationLegal(inclination)) {
                        TextView editInclination = dialogView.findViewById(R.id.editInclination);
                        editInclination.setError("Bad inclination");
                        tableActivity.showSimpleToast("Bad inclination " + inclination);
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

        final DialogEditTextBinding binding
                = DialogEditTextBinding.inflate(LayoutInflater.from(activity));
        binding.dialogEditText.setText(toRename.getName());

        binding.dialogEditText.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // nothing
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // nothing
            }

            public void afterTextChanged(Editable s) {

                String currentName = toRename.getName();
                String currentText = binding.dialogEditText.getText().toString();

                // only check for non-null or max length
                if (currentText.isEmpty()) {
                    binding.dialogEditText.setError("Cannot be blank");
                } else if (currentText.equals("-")) {
                    binding.dialogEditText.setError("Station cannot be named \"-\"");
                } else if (!currentText.equals(currentName) && (survey.getStationByName(currentText) != null)) {
                    binding.dialogEditText.setError("Station name must be unique");
                } else {
                    binding.dialogEditText.setError(null);
                }
            }
        });

        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Edit name")
                .setView(binding.getRoot())
                .setPositiveButton("Rename", (ignore, buttonId) -> {
                    String newName = binding.dialogEditText.getText().toString();
                    try {
                        SurveyUpdater.renameStation(survey, toRename, newName);
                        activity.syncTableWithSurvey();
                    } catch (Exception e) {
                        activity.showSimpleToast("Rename failed");
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();
    }


    public interface EditCallback {
        void submit(Leg leg, Dialog dialog);
    }

}

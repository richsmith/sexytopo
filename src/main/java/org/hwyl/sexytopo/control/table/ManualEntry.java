package org.hwyl.sexytopo.control.table;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.activity.TableActivity;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Survey;

/**
 * Created by rls on 04/06/15.
 */
public class ManualEntry {

    private ManualEntry() {}


    public static void addStation(final TableActivity tableActivity, final Survey survey) {
        AlertDialog dialog = createDialog(tableActivity,
                new EditCallback() {
                    @Override
                    public void submit(Leg leg) {
                        SurveyUpdater.updateWithNewStation(survey, leg);
                        SurveyManager.getInstance(tableActivity).broadcastSurveyUpdated();
                        tableActivity.syncTableWithSurvey();
                    }
                });
        dialog.setTitle(R.string.manual_add_station_title);
    }

    public static void addSplay(final TableActivity tableActivity, final Survey survey) {
        AlertDialog dialog = createDialog(tableActivity,
                new EditCallback() {
                    @Override
                    public void submit(Leg leg) {
                        SurveyUpdater.update(survey, leg);
                        SurveyManager.getInstance(tableActivity).broadcastSurveyUpdated();
                        tableActivity.syncTableWithSurvey();
                    }
                });
        dialog.setTitle(R.string.manual_add_splay_title);
    }


    public static void editLeg(final TableActivity tableActivity,
                               final Survey survey,
                               final Leg toEdit) {

        AlertDialog dialog = createDialog(tableActivity,
                new EditCallback() {
                    public void submit(Leg edited) {
                        if (toEdit.hasDestination()) {
                            edited = new Leg(
                                    edited.getDistance(),
                                    edited.getBearing(),
                                    edited.getInclination(),
                                    toEdit.getDestination());
                        }
                        SurveyUpdater.editLeg(survey, toEdit, edited);
                        SurveyManager.getInstance(tableActivity).broadcastSurveyUpdated();
                        tableActivity.syncTableWithSurvey();
                    }
                });
        dialog.setTitle("Edit Leg");

        ((TextView) (dialog.findViewById(R.id.editDistance)))
                .setText("" + toEdit.getDistance());
        ((TextView) (dialog.findViewById(R.id.editDeclination)))
                .setText("" + toEdit.getBearing());
        ((TextView) (dialog.findViewById(R.id.editInclination)))
                .setText("" + toEdit.getInclination());
    }


    private static AlertDialog createDialog(final TableActivity tableActivity,
                            final EditCallback editCallback) {


        AlertDialog.Builder builder = new AlertDialog.Builder(tableActivity);

        LayoutInflater inflater = tableActivity.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.leg_edit_dialog, null);
        builder
            .setView(dialogView)
            .setTitle(R.string.manual_add_station_title)
            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // do nothing
                }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // do nothing
                }
            });


        final AlertDialog dialog = builder.create();

        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Save", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {


                TextView editDistance = ((TextView) dialogView.findViewById(R.id.editDistance));
                Double distance = getData(editDistance);

                TextView editDeclination = ((TextView)(dialogView.findViewById(R.id.editDeclination)));
                Double declination = getData(editDeclination);

                TextView editInclination = ((TextView)(dialogView.findViewById(R.id.editInclination)));
                Double inclination = getData(editInclination);

                if (distance == null || !Leg.isDistanceLegal(distance)) {
                    editDistance.setError("Bad distance");
                    tableActivity.showSimpleToast("Bad distance");
                } else if (declination == null || !Leg.isDeclinationLegal(declination)) {
                    tableActivity.showSimpleToast("Bad declination");
                    editDeclination.setError("Bad declination");
                } else if (inclination == null || !Leg.isInclinationLegal(inclination)) {
                    tableActivity.showSimpleToast("Bad inclination");
                } else {
                    dialog.dismiss();
                    Leg leg = new Leg(distance, declination, inclination);
                    editCallback.submit(leg);
                }
            }

            private Double getData(TextView textView) {
                try {
                    return Double.parseDouble(textView.getText().toString());
                } catch (Exception e) {
                    return null;
                }
            }
        });

        return dialog;

    }

    public interface EditCallback {
        public void submit(Leg leg);
    }

}

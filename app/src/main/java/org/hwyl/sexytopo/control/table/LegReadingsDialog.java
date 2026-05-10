package org.hwyl.sexytopo.control.table;

import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TableLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.util.ReadingUpdater;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.table.TableCol;

/** Dialog for viewing and editing the individual raw readings that make up a promoted leg. */
public class LegReadingsDialog {

    private final AppCompatActivity activity;
    private final Survey survey;
    private final Station fromStation;
    private Leg promotedLeg;
    private final Runnable onChanged;

    private TableLayout table;
    private Dialog dialog;

    private LegReadingsDialog(
            AppCompatActivity activity,
            Survey survey,
            Station fromStation,
            Leg promotedLeg,
            Runnable onChanged) {
        this.activity = activity;
        this.survey = survey;
        this.fromStation = fromStation;
        this.promotedLeg = promotedLeg;
        this.onChanged = onChanged;
    }

    /**
     * Shows the readings dialog for the given promoted leg.
     *
     * @param activity the calling activity
     * @param survey the current survey
     * @param fromStation the station the leg departs from
     * @param promotedLeg the promoted leg whose readings are shown
     * @param onChanged called after any mutation so the activity can refresh its view
     */
    public static void show(
            AppCompatActivity activity,
            Survey survey,
            Station fromStation,
            Leg promotedLeg,
            Runnable onChanged) {
        new LegReadingsDialog(activity, survey, fromStation, promotedLeg, onChanged).show();
    }

    private void show() {
        LayoutInflater inflater = LayoutInflater.from(activity);
        View dialogView = inflater.inflate(R.layout.dialog_leg_readings, null);
        table = dialogView.findViewById(R.id.readingsTable);

        refreshRows();

        AlertDialog alertDialog =
                new MaterialAlertDialogBuilder(activity)
                        .setTitle(buildDialogTitle())
                        .setView(dialogView)
                        .setPositiveButton(R.string.dialog_readings_close, null)
                        .create();

        this.dialog = alertDialog;
        alertDialog.show();
    }

    private void refreshRows() {
        table.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(activity);
        Leg[] readings = promotedLeg.getPromotedFrom();

        for (int i = 0; i < readings.length; i++) {
            final int index = i;
            Leg reading = readings[i];
            Leg display = reading.wasShotBackwards() ? reading.reverse() : reading;

            View rowView = inflater.inflate(R.layout.dialog_leg_readings_row, table, false);

            ((TextView) rowView.findViewById(R.id.readingsRowDistance))
                    .setText(TableCol.DISTANCE.format(display.getDistance()));
            ((TextView) rowView.findViewById(R.id.readingsRowAzimuth))
                    .setText(TableCol.AZIMUTH.format(display.getAzimuth()));
            ((TextView) rowView.findViewById(R.id.readingsRowInclination))
                    .setText(TableCol.INCLINATION.format(display.getInclination()));

            int bgColor =
                    ContextCompat.getColor(
                            activity,
                            i % 2 == 0 ? R.color.tableBackground : R.color.tableBackgroundAlt);
            rowView.setBackgroundColor(bgColor);

            rowView.setOnLongClickListener(
                    v -> {
                        showReadingContextMenu(v, index);
                        return true;
                    });

            table.addView(rowView);
        }
    }

    private void showReadingContextMenu(View anchor, int readingIndex) {
        PopupMenu popup = new PopupMenu(activity, anchor);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.context_reading, popup.getMenu());

        String fromName =
                promotedLeg.wasShotBackwards()
                        ? promotedLeg.getDestination().getName()
                        : fromStation.getName();
        String toName =
                promotedLeg.wasShotBackwards()
                        ? fromStation.getName()
                        : promotedLeg.getDestination().getName();
        String shotTitle =
                activity.getString(
                        R.string.dialog_reading_action_title, readingIndex + 1, fromName, toName);

        popup.setOnMenuItemClickListener(
                item -> {
                    int id = item.getItemId();
                    if (id == R.id.action_downgrade_reading) {
                        onDowngradeReading(readingIndex);
                        return true;
                    } else if (id == R.id.action_edit_reading) {
                        onEditReading(readingIndex);
                        return true;
                    } else if (id == R.id.action_delete_reading) {
                        onDeleteReading(readingIndex, shotTitle);
                        return true;
                    }
                    return false;
                });

        popup.show();
    }

    private void onDeleteReading(int readingIndex, String shotTitle) {
        String noun = activity.getString(R.string.reading_shot);
        String message =
                activity.getString(R.string.context_this_will_delete)
                        + "\n"
                        + TextTools.pluralise(1, noun);
        new MaterialAlertDialogBuilder(activity)
                .setTitle(shotTitle)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.delete, (d, which) -> applyDeleteReading(readingIndex))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void applyDeleteReading(int readingIndex) {
        Leg newLeg = ReadingUpdater.deleteReading(promotedLeg, readingIndex);
        ReadingUpdater.applyUpdatedLeg(survey, fromStation, promotedLeg, newLeg);
        promotedLeg = newLeg;
        onChanged.run();

        if (!newLeg.wasPromoted()) {
            dialog.dismiss();
        } else {
            refreshRows();
        }
    }

    private void onDowngradeReading(int readingIndex) {
        Leg[] result = ReadingUpdater.downgradeReading(promotedLeg, readingIndex);
        Leg newLeg = result[0];
        Leg splay = result[1];
        ReadingUpdater.applyUpdatedLeg(survey, fromStation, promotedLeg, newLeg, splay);
        promotedLeg = newLeg;
        onChanged.run();

        if (!newLeg.wasPromoted()) {
            dialog.dismiss();
        } else {
            refreshRows();
        }
    }

    private void onEditReading(int readingIndex) {
        Leg reading = promotedLeg.getPromotedFrom()[readingIndex];
        Leg displayReading = reading.wasShotBackwards() ? reading.reverse() : reading;

        String shotTitle =
                activity.getString(
                        R.string.dialog_reading_action_title,
                        readingIndex + 1,
                        fromStation.getName(),
                        promotedLeg.getDestination().getName());

        LayoutInflater inflater = LayoutInflater.from(activity);
        View editView = inflater.inflate(R.layout.leg_edit_dialog_unified, null);

        // Hide station fields — readings are raw shots with no station context of their own
        editView.findViewById(R.id.toStationLayout).setVisibility(View.GONE);
        editView.findViewById(R.id.toCommentLayout).setVisibility(View.GONE);
        editView.findViewById(R.id.fromStationLayout).setVisibility(View.GONE);

        ((TextView) editView.findViewById(R.id.editDistance))
                .setText(Float.toString(displayReading.getDistance()));
        ((TextView) editView.findViewById(R.id.editAzimuth))
                .setText(Float.toString(displayReading.getAzimuth()));
        ((TextView) editView.findViewById(R.id.editInclination))
                .setText(Float.toString(displayReading.getInclination()));

        new MaterialAlertDialogBuilder(activity)
                .setTitle(shotTitle)
                .setView(editView)
                .setPositiveButton(
                        R.string.save,
                        (d, which) -> applyEditedReading(editView, readingIndex, reading))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void applyEditedReading(View editView, int readingIndex, Leg original) {
        Leg edited = readEditedLeg(editView, original);
        if (edited == null) {
            return;
        }
        Leg newLeg = ReadingUpdater.editReading(promotedLeg, readingIndex, edited);
        ReadingUpdater.applyUpdatedLeg(survey, fromStation, promotedLeg, newLeg);
        promotedLeg = newLeg;
        onChanged.run();
        refreshRows();
    }

    private Leg readEditedLeg(View editView, Leg original) {
        try {
            float distance =
                    Float.parseFloat(
                            ((TextView) editView.findViewById(R.id.editDistance))
                                    .getText()
                                    .toString());
            float azimuth =
                    Float.parseFloat(
                            ((TextView) editView.findViewById(R.id.editAzimuth))
                                    .getText()
                                    .toString());
            float inclination =
                    Float.parseFloat(
                            ((TextView) editView.findViewById(R.id.editInclination))
                                    .getText()
                                    .toString());

            if (!Leg.isDistanceLegal(distance)
                    || !Leg.isAzimuthLegal(azimuth)
                    || !Leg.isInclinationLegal(inclination)) {
                return null;
            }

            return new Leg(distance, azimuth, inclination, original.wasShotBackwards());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String buildDialogTitle() {
        String fromName =
                promotedLeg.wasShotBackwards()
                        ? promotedLeg.getDestination().getName()
                        : fromStation.getName();
        String toName =
                promotedLeg.wasShotBackwards()
                        ? fromStation.getName()
                        : promotedLeg.getDestination().getName();
        return activity.getString(R.string.menu_leg_title_dynamic, fromName, toName);
    }
}

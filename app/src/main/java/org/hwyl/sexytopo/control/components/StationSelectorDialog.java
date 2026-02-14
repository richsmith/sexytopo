package org.hwyl.sexytopo.control.components;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

/**
 * Utility for creating station selection dialogs with autocomplete functionality.
 */
public class StationSelectorDialog {

    public interface StationSelectedListener {
        void onStationSelected(Station station);
    }

    /**
     * Show a dialog to select a station from a survey with autocomplete.
     *
     * @param context The context for the dialog
     * @param survey The survey containing stations
     * @param titleResId Resource ID for dialog title
     * @param hintResId Resource ID for input hint
     * @param positiveButtonResId Resource ID for positive button text
     * @param listener Callback when a station is selected
     */
    public static void show(
            Context context,
            Survey survey,
            int titleResId,
            int hintResId,
            int positiveButtonResId,
            StationSelectedListener listener) {

        StationAutoCompleteField field = new StationAutoCompleteField(
            context,
            survey,
            context.getString(hintResId)
        );

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setView(field.getView())
            .setTitle(titleResId)
            .setPositiveButton(positiveButtonResId, (dialog, which) -> {
                Station station = field.getSelectedStation();
                if (station != null) {
                    listener.onStationSelected(station);
                }
            })
            .setNegativeButton(R.string.cancel, null);

        AlertDialog dialog = builder.create();
        DialogUtils.showKeyboardOnDisplay(dialog);
        dialog.show();

        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        field.enableValidation(positiveButton);
        field.requestFocus();
    }
}

package org.hwyl.sexytopo.control.components;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import org.hwyl.sexytopo.control.table.Form;

/**
 * Utility class providing common dialog styling and setup for SexyTopo.
 * Handles Material Design styling, standard padding, and keyboard management.
 */
public class DialogUtils {

    private static final int PADDING_DP_HORIZONTAL = 24;
    private static final int PADDING_DP_VERTICAL = 20;

    /**
     * Creates a standard TextInputLayout with consistent Material Design styling and padding.
     *
     * @param context The context
     * @param hintRes Hint string resource
     * @return Configured TextInputLayout
     */
    public static TextInputLayout createStandardTextInputLayout(Context context, @StringRes int hintRes) {
        TextInputLayout inputLayout = new TextInputLayout(context);
        inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        inputLayout.setHint(context.getString(hintRes));

        float density = context.getResources().getDisplayMetrics().density;
        int paddingH = (int) (PADDING_DP_HORIZONTAL * density);
        int paddingV = (int) (PADDING_DP_VERTICAL * density);
        inputLayout.setPadding(paddingH, paddingV, paddingH, 0);

        return inputLayout;
    }

    /**
     * Configures a dialog to show the keyboard when displayed.
     *
     * @param dialog The dialog to configure
     */
    public static void showKeyboardOnDisplay(Dialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    /**
     * Enables form validation on a dialog's positive button.
     * The button is enabled/disabled based on form validity and the dialog
     * won't dismiss until the form is valid.
     *
     * @param dialog The AlertDialog
     * @param form The form to validate
     * @param onValid Action to perform when form is valid (before dismissing)
     */
    public static void enableValidationOnButton(AlertDialog dialog, Form form, Runnable onValid) {
        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);

        if (positiveButton == null || form == null) {
            return;
        }

        form.setOnDidValidateCallback(valid -> positiveButton.setEnabled(valid));
        form.validate();

        positiveButton.setOnClickListener(v -> {
            form.validate();
            if (form.isValid()) {
                if (onValid != null) {
                    onValid.run();
                }
                dialog.dismiss();
            }
        });
    }
}

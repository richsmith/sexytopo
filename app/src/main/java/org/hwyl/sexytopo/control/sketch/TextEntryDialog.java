package org.hwyl.sexytopo.control.sketch;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.view.inputmethod.InputMethodManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.function.Consumer;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.components.DialogUtils;

/** Asks for some text to put on the sketch, with the keyboard up ready to type. */
final class TextEntryDialog {

    private TextEntryDialog() {}

    /**
     * Show the dialog, starting with the given text (empty for new text). onEntered gets what was
     * typed if the user confirms with something there; nothing happens on cancelling or on
     * confirming with the field empty.
     */
    static void show(Context context, String initialText, Consumer<String> onEntered) {
        TextInputLayout inputLayout =
                DialogUtils.createStandardTextInputLayout(context, R.string.sketch_text_hint);
        TextInputEditText input = DialogUtils.getEditText(inputLayout);
        input.setText(initialText);
        input.setSelection(initialText.length());

        Dialog dialog =
                new MaterialAlertDialogBuilder(context)
                        .setView(inputLayout)
                        .setPositiveButton(
                                R.string.ok,
                                (dialogInterface, which) -> {
                                    Editable editable = input.getText();
                                    if (editable != null && editable.length() > 0) {
                                        onEntered.accept(editable.toString());
                                    }
                                })
                        .setNegativeButton(R.string.cancel, null)
                        .create();

        dialog.setOnShowListener(
                dialogInterface -> {
                    input.requestFocus();
                    InputMethodManager imm =
                            (InputMethodManager)
                                    context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
                });

        DialogUtils.showKeyboardOnDisplay(dialog);
        dialog.show();
    }
}

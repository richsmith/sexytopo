package org.hwyl.sexytopo.control.sketch;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.components.DialogUtils;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.model.geometry.Coord2D;

/** Asks for some text and places it where the user tapped. */
class TextTool implements ToolHandler {

    private final Viewport viewport;
    private final SketchState state;
    private final ToolHost host;

    TextTool(Viewport viewport, SketchState state, ToolHost host) {
        this.viewport = viewport;
        this.state = state;
        this.host = host;
    }

    @Override
    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    public boolean onTouch(MotionEvent event) {

        final Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        final Coord2D touchPointOnSurvey = viewport.toSurvey(touchPointOnView);
        Context context = host.getContext();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                TextInputLayout inputLayout =
                        DialogUtils.createStandardTextInputLayout(
                                context, R.string.sketch_text_hint);

                TextInputEditText input = DialogUtils.getEditText(inputLayout);

                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                builder.setView(inputLayout)
                        .setPositiveButton(
                                R.string.ok,
                                (dialog, which) -> {
                                    Editable editable = input.getText();
                                    if (editable == null || editable.length() == 0) {
                                        return;
                                    }
                                    String text = editable.toString();
                                    int startingSizeSp = GeneralPreferences.getTextStartingSizeSp();
                                    float startingSizePixels =
                                            SketchPaints.spToPixels(context, startingSizeSp);
                                    float size = startingSizePixels / viewport.getScale();
                                    state.getSketch()
                                            .addTextDetail(
                                                    touchPointOnSurvey,
                                                    text,
                                                    state.getColour(),
                                                    size);
                                    host.invalidate();
                                })
                        .setNegativeButton(R.string.cancel, null);

                Dialog dialog = builder.create();

                // Automatically select text field
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
                return true;

            default:
                return false;
        }
    }
}

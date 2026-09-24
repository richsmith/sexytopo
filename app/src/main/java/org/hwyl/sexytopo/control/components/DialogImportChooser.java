package org.hwyl.sexytopo.control.components;

import android.content.Context;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.List;
import org.hwyl.sexytopo.control.io.translation.ImportChooser;

/** Asks import questions with a simple list dialog. */
public class DialogImportChooser implements ImportChooser {

    private final Context context;

    public DialogImportChooser(Context context) {
        this.context = context;
    }

    @Override
    public void choose(int titleRes, List<String> options, OnChosen onChosen) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(titleRes)
                .setItems(
                        options.toArray(new String[0]), (dialog, which) -> onChosen.onChosen(which))
                .show();
    }
}

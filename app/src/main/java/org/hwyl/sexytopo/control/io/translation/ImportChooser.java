package org.hwyl.sexytopo.control.io.translation;

import java.util.List;

/**
 * Lets import code ask the user to pick one of several options (e.g. which of several .svx files in
 * a folder to import) without knowing anything about the UI. If the user cancels, onChosen is never
 * called and the import is quietly abandoned.
 */
public interface ImportChooser {

    void choose(int titleRes, List<String> options, OnChosen onChosen);

    interface OnChosen {
        void onChosen(int index);
    }
}

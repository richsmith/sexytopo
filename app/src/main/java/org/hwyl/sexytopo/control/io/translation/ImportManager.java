package org.hwyl.sexytopo.control.io.translation;

import android.content.Context;
import androidx.documentfile.provider.DocumentFile;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.thirdparty.pockettopo.PocketTopoImporter;
import org.hwyl.sexytopo.control.io.thirdparty.pockettopo.PocketTopoTxtImporter;
import org.hwyl.sexytopo.control.io.thirdparty.survex.SurvexImporter;
import org.hwyl.sexytopo.control.io.thirdparty.therion.TherionImporter;
import org.hwyl.sexytopo.control.io.thirdparty.xvi.XviImporter;

public class ImportManager {

    private static final List<? extends Importer> IMPORTERS =
            Arrays.asList(
                    new TherionImporter(),
                    new XviImporter(),
                    new SurvexImporter(),
                    new PocketTopoImporter(),
                    new PocketTopoTxtImporter());

    /**
     * Imports a file or folder with whichever importer recognises it. If more than one does (e.g. a
     * folder holding both Therion and Survex files), the user is asked which format to use.
     */
    public static void importSurvey(
            Context context, DocumentFile file, ImportChooser chooser, ImportCallback callback) {
        importSurvey(context, file, chooser, callback, IMPORTERS);
    }

    static void importSurvey(
            Context context,
            DocumentFile file,
            ImportChooser chooser,
            ImportCallback callback,
            List<? extends Importer> importers) {

        List<Importer> candidates =
                importers.stream()
                        .filter(importer -> importer.canHandleFile(file))
                        .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            callback.onImportFailed(new IllegalArgumentException("could not recognise that data"));
        } else if (candidates.size() == 1) {
            candidates.get(0).importSurvey(context, file, chooser, callback);
        } else {
            List<String> names =
                    candidates.stream().map(Importer::getName).collect(Collectors.toList());
            chooser.choose(
                    R.string.import_choose_format,
                    names,
                    index -> candidates.get(index).importSurvey(context, file, chooser, callback));
        }
    }
}

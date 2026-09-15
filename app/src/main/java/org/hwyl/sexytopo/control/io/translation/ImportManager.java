package org.hwyl.sexytopo.control.io.translation;

import android.content.Context;
import androidx.documentfile.provider.DocumentFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hwyl.sexytopo.control.io.thirdparty.pockettopo.PocketTopoImporter;
import org.hwyl.sexytopo.control.io.thirdparty.pockettopo.PocketTopoTxtImporter;
import org.hwyl.sexytopo.control.io.thirdparty.survex.SurvexImporter;
import org.hwyl.sexytopo.control.io.thirdparty.therion.TherionImporter;
import org.hwyl.sexytopo.control.io.thirdparty.xvi.XviImporter;
import org.hwyl.sexytopo.model.survey.Survey;

@SuppressWarnings("UnnecessaryLocalVariable")
public class ImportManager {

    private static final List<? extends Importer> IMPORTERS =
            Arrays.asList(
                    new TherionImporter(),
                    new XviImporter(),
                    new SurvexImporter(),
                    new PocketTopoImporter(),
                    new PocketTopoTxtImporter());

    /**
     * Returns all folder importers that can handle the given directory. Used by the UI to determine
     * whether to show a format-selection dialog before a file-selection dialog.
     */
    public static List<FolderImporter> getFolderImporters(DocumentFile directory) {
        List<FolderImporter> matching = new ArrayList<>();
        for (Importer importer : IMPORTERS) {
            if (importer instanceof FolderImporter && importer.canHandleFile(directory)) {
                matching.add((FolderImporter) importer);
            }
        }
        return matching;
    }

    public static Survey toSurvey(Context context, DocumentFile file) throws Exception {
        Importer importer = chooseImporter(file);
        Survey survey = importer.toSurvey(context, file);
        return survey;
    }

    private static Importer chooseImporter(DocumentFile file) throws IllegalArgumentException {
        for (Importer importer : IMPORTERS) {
            if (importer.canHandleFile(file)) {
                return importer;
            }
        }

        throw new IllegalArgumentException("could not recognise that data");
    }
}

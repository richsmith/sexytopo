package org.hwyl.sexytopo.control.io.translation;

import android.content.Context;

import androidx.documentfile.provider.DocumentFile;

import org.hwyl.sexytopo.control.io.thirdparty.pockettopo.PocketTopoImporter;
import org.hwyl.sexytopo.control.io.thirdparty.pockettopo.PocketTopoTxtImporter;
import org.hwyl.sexytopo.control.io.thirdparty.survex.SurvexImporter;
import org.hwyl.sexytopo.control.io.thirdparty.therion.TherionImporter;
import org.hwyl.sexytopo.control.io.thirdparty.xvi.XviImporter;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.Arrays;
import java.util.List;


@SuppressWarnings("UnnecessaryLocalVariable")
public class ImportManager {


    private static final List<? extends Importer> IMPORTERS = Arrays.asList(
        new TherionImporter(),
        new XviImporter(),
        new SurvexImporter(),
        new PocketTopoImporter(),
        new PocketTopoTxtImporter()
    );

    public static Survey toSurvey(Context context, DocumentFile file) throws Exception {
        Importer importer = chooseImporter(file);
        Survey survey = importer.toSurvey(context, file);
        return survey;
    }

    private static Importer chooseImporter(DocumentFile file) throws IllegalArgumentException {
        for (Importer importer: IMPORTERS) {
            if (importer.canHandleFile(file)) {
                return importer;
            }
        }

        throw new IllegalArgumentException("could not recognise that data");
    }

}

package org.hwyl.sexytopo.control.io.translation;

import org.hwyl.sexytopo.control.io.thirdparty.PocketTopoTxtImporter;
import org.hwyl.sexytopo.control.io.thirdparty.therion.TherionImporter;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.File;
import java.util.Arrays;
import java.util.List;


public class ImportManager {


    private static List<? extends Importer> importers = Arrays.asList(
        new PocketTopoTxtImporter(),
        new TherionImporter()
    );


    public static Survey toSurvey(File file) throws Exception {
        Importer importer = chooseImporter(file);
        Survey survey = importer.toSurvey(file);
        return survey;
    }

    private static Importer chooseImporter(File file) throws IllegalArgumentException {
        for (Importer importer: importers) {
            if (importer.canHandleFile(file)) {
                return importer;
            }
        }

        throw new IllegalArgumentException("Cannot import a file of that type");
    }

}

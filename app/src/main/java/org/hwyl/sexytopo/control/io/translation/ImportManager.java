package org.hwyl.sexytopo.control.io.translation;

import org.apache.commons.io.FileUtils;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.io.Util;
import org.hwyl.sexytopo.control.io.thirdparty.pockettopo.PocketTopoTxtImporter;
import org.hwyl.sexytopo.control.io.thirdparty.survex.SurvexImporter;
import org.hwyl.sexytopo.control.io.thirdparty.therion.TherionImporter;
import org.hwyl.sexytopo.control.io.thirdparty.xvi.XviImporter;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class ImportManager {


    private static final List<? extends Importer> IMPORTERS = Arrays.asList(
        new TherionImporter(),
        new XviImporter(),
        new SurvexImporter(),
        new PocketTopoTxtImporter()
    );


    public static Survey toSurvey(File file) throws Exception {
        Importer importer = chooseImporter(file);
        Survey survey = importer.toSurvey(file);
        return survey;
    }

    public static void saveACopyOfSourceInput(Survey survey, File source) throws IOException {
        String surveyDirectory = Util.getDirectoryPathForSurvey(survey.getName());
        String path = surveyDirectory + File.separator + SexyTopo.IMPORT_SOURCE_DIR;
        Util.ensureDirectoriesInPathExist(path);

        File destinationDirectory = new File(path);
        FileUtils.cleanDirectory(destinationDirectory);

        File destination = new File(path + File.separator + source.getName());

        if (source.isDirectory()) {
            FileUtils.copyDirectory(source, destination);
        } else {
            FileUtils.copyFile(source, destination);
        }
    }



    private static Importer chooseImporter(File file) throws IllegalArgumentException {
        for (Importer importer: IMPORTERS) {
            if (importer.canHandleFile(file)) {
                return importer;
            }
        }

        throw new IllegalArgumentException("Cannot import a file of that type");
    }

}

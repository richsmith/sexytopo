package org.hwyl.sexytopo.control.io;

import org.hwyl.sexytopo.SexyTopo;

import java.io.File;
import java.util.HashSet;
import java.util.Set;


/**
 * Created by rls on 08/10/14.
 */
public class Util {


    public static void ensureDirectoriesInPathExist(String path) {
        new File(path).mkdirs();
    }


    private static void ensureDirectoryExists(String path) {
        File surveyDirectory = new File(path);
        if (!surveyDirectory.exists()) {
            surveyDirectory.mkdirs();
        }
    }


    public static File[] getSurveyDirectories() {
        ensureDirectoryExists(SexyTopo.EXTERNAL_SURVEY_DIR);
        File surveyDirectory = new File(SexyTopo.EXTERNAL_SURVEY_DIR);
        File surveyDirectories[] = surveyDirectory.listFiles();
        return surveyDirectories;
    }

    public static File[] getImportFiles() {
        ensureDirectoryExists(SexyTopo.EXTERNAL_IMPORT_DIR);
        File importDirectory = new File(SexyTopo.EXTERNAL_IMPORT_DIR);
        File importFiles[] = importDirectory.listFiles();
        return importFiles;
    }


    public static String getNextDefaultSurveyName(String defaultName) {

        Set<String> existingSurveyNames = getExistingSurveyNames();

        if (existingSurveyNames.isEmpty()) {
            return defaultName;
        }

        for (int i = 0; i <= existingSurveyNames.size(); i++) {
            String name = defaultName + (i == 0 ? "" : ("-" + (i + 1)));
            if (!existingSurveyNames.contains(name)) {
                return name;
            }
        }

        // shouldn't get here
        throw new IllegalStateException(
                "Programming error trying to find next default name for " + defaultName);

    }


    private static Set<String> getExistingSurveyNames() {
        File[] surveyDirectories = getSurveyDirectories();
        Set<String> existingSurveyNames = new HashSet<String>();
        for (File surveyDirectory : surveyDirectories) {
            existingSurveyNames.add(surveyDirectory.getName());
        }
        return existingSurveyNames;
    }


    public static String getDirectoryForSurveyFile(String name) {
        return SexyTopo.EXTERNAL_SURVEY_DIR + File.separator + name;
    }

    public static String getPathForSurveyFile(String name, String extension) {
        String directory = getDirectoryForSurveyFile(name);
        return directory + File.separator + name + "." + extension;
    }


    public static void deleteSurvey(String name) throws Exception {
        String surveyDirectory = getDirectoryForSurveyFile(name);
        new File(surveyDirectory).delete();
    }


    public static boolean doesFileExist(String path) {
        File filename = new File(path);
        return filename.exists();
    }

}

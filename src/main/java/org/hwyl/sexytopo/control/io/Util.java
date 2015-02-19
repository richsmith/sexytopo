package org.hwyl.sexytopo.control.io;

import org.hwyl.sexytopo.SexyTopo;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by rls on 08/10/14.
 */
public class Util {

    public static void ensureSurveyDirectoryExists() {
        File surveyDirectory = new File(SexyTopo.SURVEY_PATH);
        if (! surveyDirectory.exists()) {
            surveyDirectory.mkdirs();
        }
    }

    public static void ensureDirectoriesInPathExist(String path) {
        String[] dirs = path.split(File.separator);
    }

    public static File[] getSurveyDirectories() {
        ensureSurveyDirectoryExists();
        File surveyDirectory = new File(SexyTopo.SURVEY_PATH);
        File surveyDirectories[] = surveyDirectory.listFiles();
        return surveyDirectories;
    }
    
    public static String getNextDefaultSurveyName(String defaultName) {

        Set<String> existingSurveyNames = getExistingSurveyNames();

        if (existingSurveyNames.isEmpty()) {
            return defaultName;
        }

        for (int i = 0; i < existingSurveyNames.size(); i++) {
            String name = defaultName + (i == 0? "" : ("-" + (i + 1)));
            if (! existingSurveyNames.contains(name)) {
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

    public static void ensureDirectoryExists(String path) {
        new File(path).mkdirs();
    }

    public static String getDirectoryForSurveyFile(String name) {
        return SexyTopo.SURVEY_PATH + File.separatorChar + name;
    }

    public static void deleteSurvey(String name) {
        String path = getDirectoryForSurveyFile(name);
        File surveyDirectory = new File(path);
        surveyDirectory.delete();
    }

    public static String getPathForSurveyFile(String name, String extension) {
        return getDirectoryForSurveyFile(name) + File.separatorChar + name + "." + extension;
    }

    public static String getPathForDataFile(String surveyName, String name, String extension) {
        return getDirectoryForSurveyFile(surveyName) + File.separatorChar + name + "." + extension;
    }


    public static boolean doesFileExist(String path) {
        File filename = new File(path);
        return filename.exists();
    }

    public boolean doesSurveyNeedSaving() {
        // FIXME
        return true;
    }
}

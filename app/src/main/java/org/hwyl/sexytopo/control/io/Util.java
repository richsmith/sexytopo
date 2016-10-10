package org.hwyl.sexytopo.control.io;

import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.util.TextTools;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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

    public static void ensureDataDirectoriesExist() {
        ensureDirectoryExists(SexyTopo.EXTERNAL_SURVEY_DIR);
        ensureDirectoryExists(SexyTopo.EXTERNAL_IMPORT_DIR);
        ensureDirectoryExists(SexyTopo.EXTERNAL_EXPORT_DIR);
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

    public static String getNextDefaultSurveyName(Context context) {
        String defaultNameBase = context.getString(R.string.default_survey_name);
        return getNextDefaultSurveyName(defaultNameBase);
    }


    public static String getNextDefaultSurveyName(String defaultName) {

        Set<String> existingSurveyNames = getExistingSurveyNames();

        if (!existingSurveyNames.contains(defaultName)) {
            return defaultName;
        }

        for (String name : existingSurveyNames) {
            Pattern pattern = Pattern.compile("(.+-)(\\d+)\\z");
            Matcher matcher = pattern.matcher(name);
            boolean foundMatch = matcher.find();
            if (!foundMatch) {
                continue;
            } else {
                String withoutSuffix = matcher.group(1);
                int numberSuffix = Integer.parseInt(matcher.group(2));
                return withoutSuffix + (++numberSuffix);
            }
        }

        return defaultName + "-2";
    }


    public static boolean isSurveyNameUnique(String name) {
        return !getExistingSurveyNames().contains(name);
    }


    private static Set<String> getExistingSurveyNames() {
        File[] surveyDirectories = getSurveyDirectories();
        Set<String> existingSurveyNames = new HashSet<String>();
        for (File surveyDirectory : surveyDirectories) {
            existingSurveyNames.add(surveyDirectory.getName());
        }
        return existingSurveyNames;
    }


    public static String getDirectoryPathForSurvey(String name) {
        return SexyTopo.EXTERNAL_SURVEY_DIR + File.separator + name;
    }

    public static String getPathForSurveyFile(String name, String extension) {
        String directory = getDirectoryPathForSurvey(name);
        return directory + File.separator + name + "." + extension;
    }

    public static String getExportDirectoryPath(String exportFormat, String name) {
        return TextTools.joinAll(File.separator, SexyTopo.EXTERNAL_EXPORT_DIR, exportFormat, name);
    }


    public static void deleteSurvey(String name) throws Exception {
        File surveyDirectory = new File(getDirectoryPathForSurvey(name));
        deleteFileAndAnyContents(surveyDirectory);
    }


    private static void deleteFileAndAnyContents(File file) {
        if (file.isDirectory())
            for (File child : file.listFiles()) {
                deleteFileAndAnyContents(child);
            }

        file.delete();
    }


    public static boolean doesFileExist(String path) {
        File filename = new File(path);
        return filename.exists();
    }

}

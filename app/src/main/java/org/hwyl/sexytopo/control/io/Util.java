package org.hwyl.sexytopo.control.io;

import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.survey.Survey;

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
        ensureDirectoryExists(SexyTopo.getExternalSurveyDirectory());
        ensureDirectoryExists(SexyTopo.getExternalImportDirectory());
        ensureDirectoryExists(SexyTopo.getExternalExportDirectory());
    }


    public static File[] getSurveyDirectories() {
        ensureDirectoryExists(SexyTopo.getExternalSurveyDirectory());
        File surveyDirectory = new File(SexyTopo.getExternalSurveyDirectory());
        File surveyDirectories[] = surveyDirectory.listFiles();
        return surveyDirectories;
    }

    public static File[] getImportFiles() {
        ensureDirectoryExists(SexyTopo.getExternalImportDirectory());
        File importDirectory = new File(SexyTopo.getExternalImportDirectory());
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


    public static String getNextAvailableName(String basename) {
        String name = basename;
        do {
            name = getNextName(name);
        } while (doesSurveyExist(name));
        return  name;
    }

    public static String getNextName(String basename) {
        if (basename.length() == 0) {
            return "name";
        }
        char last = basename.charAt(basename.length() - 1);
        if (Character.isDigit(last)) {
            return TextTools.advanceLastNumber(basename);
        } else {
            return basename + "-2";
        }
    }


    public static boolean doesSurveyExist(String name) {
        return getExistingSurveyNames().contains(name);
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
        return SexyTopo.getExternalSurveyDirectory() + File.separator + name;
    }

    public static String getPathForSurveyFile(String name, String extension) {
        String directory = getDirectoryPathForSurvey(name);
        return directory + File.separator + name + "." + extension;
    }

    public static String getExportDirectoryPath(String exportFormat, String name) {
        return TextTools.joinAll(File.separator, SexyTopo.getExternalExportDirectory(), exportFormat, name);
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

    public static String getImportSourceDirectoryPath(Survey survey) {
        String surveyDirectory = getDirectoryPathForSurvey(survey.getName());
        String path = surveyDirectory + File.separator + SexyTopo.IMPORT_SOURCE_DIR;
        return path;
    }

    public static boolean wasSurveyImported(Survey survey) {
        String path = getImportSourceDirectoryPath(survey);
        File file = new File(path);
        return (file.exists() && file.listFiles().length > 0);
    }

    public static File getFileSurveyWasImportedFrom(Survey survey) {
        String path = getImportSourceDirectoryPath(survey);
        return new File(path).listFiles()[0];
    }

}

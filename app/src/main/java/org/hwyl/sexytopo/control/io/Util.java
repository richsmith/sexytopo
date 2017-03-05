package org.hwyl.sexytopo.control.io;

import android.content.Context;
import android.os.Environment;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hwyl.sexytopo.SexyTopo.APP_DIR;
import static org.hwyl.sexytopo.SexyTopo.EXPORT_DIR;
import static org.hwyl.sexytopo.SexyTopo.IMPORT_DIR;
import static org.hwyl.sexytopo.SexyTopo.SURVEY_DIR;


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

    public static void ensureDataDirectoriesExist(Context context) {
        ensureDirectoryExists(getSurveyDirectory(context));
        ensureDirectoryExists(getImportDirectory(context));
        ensureDirectoryExists(getExportDirectory(context));
    }


    public static File[] getSurveyDirectories(Context context) {
        ensureDirectoryExists(getSurveyDirectory(context));
        File surveyDirectory = new File(getSurveyDirectory(context));
        File surveyDirectories[] = surveyDirectory.listFiles();
        return surveyDirectories;
    }

    public static File[] getImportFiles(Context context) {
        ensureDirectoryExists(getImportDirectory(context));
        File importDirectory = new File(getImportDirectory(context));
        File importFiles[] = importDirectory.listFiles();
        return importFiles;
    }

    public static String getNextDefaultSurveyName(Context context) {
        String defaultNameBase = context.getString(R.string.default_survey_name);
        return getNextDefaultSurveyName(context, defaultNameBase);
    }


    public static String getNextDefaultSurveyName(Context context, String defaultName) {

        Set<String> existingSurveyNames = getExistingSurveyNames(context);

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


    public static String getNextAvailableName(Context context, String basename) {
        String name = basename;
        do {
            name = getNextName(name);
        } while (doesSurveyExist(context, name));
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


    public static boolean doesSurveyExist(Context context, String name) {
        return getExistingSurveyNames(context).contains(name);
    }


    public static boolean isSurveyNameUnique(Context context, String name) {
        return !getExistingSurveyNames(context).contains(name);
    }


    private static Set<String> getExistingSurveyNames(Context context) {
        File[] surveyDirectories = getSurveyDirectories(context);
        Set<String> existingSurveyNames = new HashSet<>();
        for (File surveyDirectory : surveyDirectories) {
            existingSurveyNames.add(surveyDirectory.getName());
        }
        return existingSurveyNames;
    }


    public static String getDirectoryPathForSurvey(Context context, String name) {
        return getSurveyDirectory(context) + File.separator + name;
    }

    public static String getPathForSurveyFile(Context context, String name, String extension) {
        String directory = getDirectoryPathForSurvey(context, name);
        return directory + File.separator + name + "." + extension;
    }

    public static String getExportDirectoryPath(Context context, String exportFormat, String name) {
        return TextTools.joinAll(File.separator, getExportDirectory(context), exportFormat, name);
    }


    public static void deleteSurvey(Context context, String name) throws Exception {
        File surveyDirectory = new File(getDirectoryPathForSurvey(context, name));
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

    public static String getImportSourceDirectoryPath(Context context, Survey survey) {
        String surveyDirectory = getDirectoryPathForSurvey(context, survey.getName());
        String path = surveyDirectory + File.separator + SexyTopo.IMPORT_SOURCE_DIR;
        return path;
    }

    public static boolean wasSurveyImported(Context context, Survey survey) {
        String path = getImportSourceDirectoryPath(context, survey);
        File file = new File(path);
        return (file.exists() && file.listFiles().length > 0);
    }

    public static File getFileSurveyWasImportedFrom(Context context, Survey survey) {
        String path = getImportSourceDirectoryPath(context, survey);
        return new File(path).listFiles()[0];
    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state));
    }

    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
    }

    public static String getExternalRoot(Context context) {
        String ext = context.getExternalFilesDir(null).getAbsolutePath();
        return ext;
        //return context.getExternalFilesDir(null);
    }

    public static String getSurveyDirectory(Context context) {
        return TextTools.joinAll(File.separator, getExternalRoot(context), APP_DIR, SURVEY_DIR);
    }

    public static String getImportDirectory(Context context) {
        return TextTools.joinAll(File.separator, getExternalRoot(context), APP_DIR, IMPORT_DIR);
    }

    public static String getExportDirectory(Context context) {
        return TextTools.joinAll(File.separator, getExternalRoot(context), APP_DIR, EXPORT_DIR);
    }

    public static String getAutosaveName(String filename) {
        return filename + "." + SexyTopo.AUTOSAVE_EXTENSION;
    }


}

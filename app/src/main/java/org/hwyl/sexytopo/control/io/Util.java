package org.hwyl.sexytopo.control.io;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.content.ContextCompat;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.survey.Survey;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hwyl.sexytopo.SexyTopo.CALIBRATION_DIR;
import static org.hwyl.sexytopo.SexyTopo.EXPORT_DIR;
import static org.hwyl.sexytopo.SexyTopo.IMPORT_DIR;
import static org.hwyl.sexytopo.SexyTopo.LOG_DIR;
import static org.hwyl.sexytopo.SexyTopo.SURVEY_DIR;


public class Util {


    public static void ensureDirectoriesInPathExist(String path) {
        new File(path).mkdirs();
    }


    private static void ensureDirectoryExists(File directory) {
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    public static void ensureDataDirectoriesExist(Context context) {
        ensureDirectoryExists(getSurveyDirectory(context));
        ensureDirectoryExists(getImportDir(context));
        ensureDirectoryExists(getExportDirectory(context));
        ensureDirectoryExists(getCalibrationDirectory(context));

    }


    public static File[] getSurveyDirectories(Context context) {
        ensureDirectoryExists(getSurveyDirectory(context));
        File surveyDirectory = getSurveyDirectory(context);
        File surveyDirectories[] = surveyDirectory.listFiles();
        return surveyDirectories;
    }


    public static File[] getCalibrationFiles(Context context) {
        ensureDataDirectoriesExist(context);
        File calibrationDirectory = getCalibrationDirectory(context);
        File[] calibrationFiles = calibrationDirectory.listFiles();
        return calibrationFiles;
    }

    public static File[] getImportFiles(Context context) {
        ensureDirectoryExists(getImportDir(context));
        File importDirectory = getImportDir(context);
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

    public static boolean doWeHavePermissionToWriteToExternalStorage(Context context) {
        return (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean isExternalStorageMounted() {
        String state = Environment.getExternalStorageState();
        boolean isMounted = (Environment.MEDIA_MOUNTED.equals(state));
        return isMounted;
    }

    public static boolean isExternalStorageWriteable(Context context) {
        boolean havePermission = doWeHavePermissionToWriteToExternalStorage(context);
        boolean isMounted = isExternalStorageMounted();
        return havePermission && isMounted;
    }

    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
    }

    public static File getDocumentRoot(Context context) {

        if (isExternalStorageWriteable(context)) {
            try {
                return getExternalDirectory();
            } catch (Throwable exception) {
                return getInternalDirectory(context);
            }
        } else {
            return getInternalDirectory(context);
        }
    }

    public static File getInternalDirectory(Context context) {
        return context.getFilesDir();
    }

    public static File getExternalDirectory() {

        File directory = null;

        try {
            directory = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS), "SexyTopo");

        } catch (Throwable exception) { //old version of Android?
            directory = new File(Environment.getExternalStorageDirectory() + "/Documents/SexyTopo");

        } finally {
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IllegalStateException(
                        "Could not create survey master directory " + directory);
            }

            return directory;
        }
    }


    public static File getSurveyDirectory(Context context) {
        return new File(getDocumentRoot(context), SURVEY_DIR);
    }


    public static File getImportDir(Context context) {
        return new File(getDocumentRoot(context), IMPORT_DIR);
    }

    public static File getExportDirectory(Context context) {
        return new File(getDocumentRoot(context), EXPORT_DIR);
    }

    public static File getLogDirectory(Context context) {
        return new File(getDocumentRoot(context), LOG_DIR);
    }

    public static File getCalibrationDirectory(Context context) {
        return new File(getDocumentRoot(context), CALIBRATION_DIR);
    }

    public static String getAutosaveName(String filename) {
        return filename + "." + SexyTopo.AUTOSAVE_EXTENSION;
    }


    public static Map<String, JSONArray> toMap(JSONObject object) throws JSONException {
        Map<String, JSONArray> map = new HashMap<>();
        Iterator iterator = object.keys();
        while (iterator.hasNext()) {
            String key = (String)iterator.next();
            JSONArray value = object.getJSONArray(key);
            map.put(key, value);
        }
        return map;
    }


    public static List<JSONObject> toList(JSONArray array) throws JSONException {
        List<JSONObject> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.getJSONObject(i));
        }
        return list;
    }


    public static String getPath(File directory, String filename) {
        return directory.getPath() + File.separator + filename;
    }
}

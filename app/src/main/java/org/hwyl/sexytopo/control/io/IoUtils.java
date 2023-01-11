package org.hwyl.sexytopo.control.io;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;

import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.io.basic.Loader;
import org.hwyl.sexytopo.control.io.basic.Saver;
import org.hwyl.sexytopo.model.survey.Survey;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@SuppressWarnings("UnnecessaryLocalVariable")
public class IoUtils {


    public static void ensureDirectoriesInPathExist(String path) {
        new File(path).mkdirs();
    }


    private static void ensureDirectoryExists(File directory) {
        if (!directory.exists()) {
            boolean success = directory.mkdirs();
        }
    }

    public static boolean doesSurveyExist(Context context, Uri uri) {
        return DocumentFile.fromTreeUri(context, uri).isDirectory();
    }

    public static boolean isDirectoryEmpty(DocumentFile directory) {
        return directory.listFiles().length == 0;
    }


    public static boolean isSurveyDirectory(DocumentFile directory) {
        DocumentFile[] files = directory.listFiles();
        for (DocumentFile file : files) {
            String filename = file.getName();
            if (filename.endsWith(SexyTopo.DATA_EXTENSION)) {
                return true;
            }
        }
        return false;
    }


    public static DocumentFile getOrCreateFile(
            DocumentFile directory, String mimeType, String filename) {
        DocumentFile file = directory.findFile(filename);
        if (file == null) {
            file = directory.createFile(mimeType, filename);
        }
        return file;
    }

    public static DocumentFile getOrCreateDirectory(DocumentFile directory, String filename) {
        DocumentFile file = directory.findFile(filename);
        if (file == null) {
            file = directory.createDirectory(filename);
        }
        return file;
    }

    public static String withExtension(String filename, String extension) {
        return filename + "." + extension;
    }

    public static DocumentFile getOrCreateSurveyChildFile(
            Context context, Survey survey, String extension) {
        DocumentFile surveyDirectory = getSurveyDirectory(context, survey);
        String filename = withExtension(survey.getName(), extension);
        DocumentFile file = getOrCreateFile(
                surveyDirectory, "application/json", filename);
        return file;
    }


    public static DocumentFile getOrCreateSurveyChildDirectory(
            Context context, Survey survey, String filename) {
        DocumentFile surveyDirectory = getSurveyDirectory(context, survey);
        DocumentFile file = getOrCreateDirectory(surveyDirectory, filename);
        return file;
    }


    public static DocumentFile getDataFile(Context context, Survey survey) {
        DocumentFile file = getOrCreateSurveyChildFile(
                context, survey, SexyTopo.DATA_EXTENSION);
        return file;
    }

    public static DocumentFile getAutosaveDataFile(Context context, Survey survey) {
        String extension = withExtension(SexyTopo.DATA_EXTENSION, SexyTopo.AUTOSAVE_EXTENSION);
        DocumentFile file = getOrCreateSurveyChildFile(context, survey, extension);
        return file;
    }

    public static DocumentFile getMetadataFile(Context context, Survey survey) {
        DocumentFile file = getOrCreateSurveyChildFile(
                context, survey, SexyTopo.METADATA_EXTENSION);
        return file;
    }

    public static DocumentFile getPlanSketchFile(Context context, Survey survey) {
        DocumentFile file = getOrCreateSurveyChildFile(
                context, survey, SexyTopo.PLAN_SKETCH_EXTENSION);
        return file;
    }

    public static DocumentFile getExtendedElevationSketchFile(Context context, Survey survey) {
        DocumentFile file = getOrCreateSurveyChildFile(
                context, survey, SexyTopo.EXT_ELEVATION_SKETCH_EXTENSION);
        return file;
    }


    public static DocumentFile getImportSourceDirectory(Context context, Survey survey) {
        DocumentFile file = getOrCreateSurveyChildDirectory(
                context, survey, SexyTopo.IMPORT_SOURCE_DIR);
        return file;
    }


    public static DocumentFile getExportDirectory(Context context, Survey survey) {
        DocumentFile file = getOrCreateSurveyChildDirectory(
                context, survey, SexyTopo.EXPORT_DIR);
        return file;
    }

    public static DocumentFile getSurveyDirectory(Context context, Survey survey) {
        Uri uri = survey.getUri();
        return getSurveyDirectory(context, uri);
    }

    public static DocumentFile getSurveyDirectory(Context context, Uri uri) {
        DocumentFile directory = DocumentFile.fromTreeUri(context, uri);
        return directory;
    }

    public static void deleteSurvey(Context context, Survey survey) {
        DocumentFile surveyDirectory = DocumentFile.fromTreeUri(context, survey.getUri());
        boolean deleted = surveyDirectory.delete();
        if (deleted) {
            Log.i("Deleted survey " + survey.getName());
        } else {
            Log.e("Failed to delete survey " + survey.getName());
        }

    }

    public static boolean doesFileExist(String path) {
        File filename = new File(path);
        return filename.exists();
    }

    public static boolean wasSurveyImported(Context context, Survey survey) {
        DocumentFile importSourceDirectory = getImportSourceDirectory(context, survey);
        return importSourceDirectory.exists()
                && importSourceDirectory.isDirectory();
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


    public static File getInternalDirectory(Context context) {
        return context.getFilesDir();
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

    public static List<String> toListOfStrings(JSONArray array) throws JSONException {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.getString(i));
        }
        return list;
    }


    public static String getPath(File directory, String filename) {
        return directory.getPath() + File.separator + filename;
    }

    public static void copyFile(
            Context context, DocumentFile source, DocumentFile destination) throws IOException {

        if (source.isDirectory()) {
            DocumentFile childDirectory = destination.createDirectory(
                    Objects.requireNonNull(source.getName()));
            for (DocumentFile file : source.listFiles()) {
                copyFile(context, file, childDirectory);
            }

        } else {
            String contents = Loader.slurpFile(context, source);
            DocumentFile file = destination.createFile(source.getType(), source.getName());
            Saver.saveFile(context, file, contents);
        }
    }


    public static Uri getParentUri(Survey survey) {
        Uri uri = null;
        if (survey != null && survey.hasHome()) {
            DocumentFile directory = survey.getDirectory();
            DocumentFile parent = directory.getParentFile();
            if (parent != null) {
                uri = parent.getUri();
            }
        }
        return uri;
    }

    public static Uri getDefaultSurveyUri(Context context) {
        Uri uri = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String startDir = SexyTopo.DEFAULT_ROOT_DIR;
            StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            Intent intent = sm.getPrimaryStorageVolume().createOpenDocumentTreeIntent();
            Uri initial = intent.getParcelableExtra("android.provider.extra.INITIAL_URI");
            String scheme = initial.toString();
            scheme = scheme.replace("/root/", "/document/");
            scheme += "%3A" + startDir;
            uri = Uri.parse(scheme);
        }

        return uri;
    }

}

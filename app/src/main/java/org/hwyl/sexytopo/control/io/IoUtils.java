package org.hwyl.sexytopo.control.io;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.storage.StorageManager;

import androidx.documentfile.provider.DocumentFile;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.model.survey.Survey;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


@SuppressWarnings("UnnecessaryLocalVariable")
public class IoUtils {

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

    public static void deleteSurvey(Context context, Survey survey) {
        DocumentFile surveyDirectory = DocumentFile.fromTreeUri(context, survey.getUri());
        boolean deleted = surveyDirectory.delete();
        if (deleted) {
            Log.i("Deleted survey " + survey.getName());
        } else {
            Log.e("Failed to delete survey " + survey.getName());
        }

    }

    public static boolean wasSurveyImported(Context context, Survey survey) {
        SurveyDirectory importSourceDirectory = SurveyDirectory.IMPORT_SOURCE.get(survey);
        return importSourceDirectory.exists(context);
    }

    public static Map<String, JSONArray> toMap(JSONObject object) throws JSONException {
        Map<String, JSONArray> map = new HashMap<>();
        Iterator<String> iterator = object.keys();
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

        /* FIXME
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
        }*/
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

    public static String slurpFile(Context context, DocumentFile file) throws IOException{
        ContentResolver contentResolver = context.getContentResolver();

        try (InputStream inputStream = contentResolver.openInputStream(file.getUri())) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int length; (length = inputStream.read(buffer)) != -1; ) {
                output.write(buffer, 0, length);
            }

            String content = output.toString("UTF-8");
            return content;
        }
    }

    public static void saveToFile(Context context, DocumentFile documentFile, String contents)
            throws IOException{
        Uri uri = documentFile.getUri();
        ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "w");
        FileOutputStream fileOutputStream =
                new FileOutputStream(pfd.getFileDescriptor());
        fileOutputStream.write(contents.getBytes());
        fileOutputStream.close();
        pfd.close();
    }

}

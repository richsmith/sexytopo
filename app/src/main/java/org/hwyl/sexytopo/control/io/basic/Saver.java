package org.hwyl.sexytopo.control.io.basic;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.activity.SexyTopoActivity;
import org.hwyl.sexytopo.control.io.IoUtils;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import androidx.documentfile.provider.DocumentFile;


public class Saver {

    public static synchronized void save(Context context, Survey survey) throws Exception {
        saveMetadata(context, survey);
        saveSurveyData(context, survey);
        savePlanSketch(context, survey);
        saveElevationSketch(context, survey);
        survey.setSaved(true);

        Log.d("Saved survey " + survey.getName());
    }

    public static synchronized void autosave(Context context, Survey survey) throws Exception {
        throw new Exception("Not implemented yet!!!");
    }

    private static void saveSurveyData(Context context, Survey survey)
            throws IOException, JSONException {
        String filename = SexyTopo.DATA_EXTENSION;
        String versionName = SexyTopoActivity.getVersionName(context);
        int versionCode = SexyTopoActivity.getVersionCode(context);
        String surveyText = SurveyJsonTranslater.toText(survey, versionName, versionCode);
        saveToFileInSurvey(context, survey, filename, surveyText);
    }

    private static void savePlanSketch(Context context, Survey survey)
            throws IOException, JSONException {
        String filename = SexyTopo.PLAN_SKETCH_EXTENSION;
        Sketch sketch = survey.getPlanSketch();
        String planText = SketchJsonTranslater.translate(sketch);
        saveToFileInSurvey(context, survey, filename, planText);
    }


    private static void saveElevationSketch(Context context, Survey survey)
            throws IOException, JSONException {
        String filename = SexyTopo.EXT_ELEVATION_SKETCH_EXTENSION;
        Sketch sketch = survey.getElevationSketch();
        String eeText = SketchJsonTranslater.translate(sketch);
        saveToFileInSurvey(context, survey, filename, eeText);
    }

    private static void saveMetadata(Context context, Survey survey)
            throws Exception {
        String metadataText = MetadataTranslater.translate(survey);
        saveToFileInSurvey(context, survey, SexyTopo.METADATA_EXTENSION, metadataText);
    }

    public static void saveToFileInSurvey(
            Context context, Survey survey, String extension, String contents)
    throws IOException {
        Uri surveyUri = survey.getUri();
        DocumentFile surveyDirectory = DocumentFile.fromTreeUri(context, surveyUri);
        String filename = IoUtils.withExtension(survey.getName(), extension);
        DocumentFile file = surveyDirectory.findFile(filename);
        if (file == null) {
            file = surveyDirectory.createFile("text/plain", filename);
        }
        if (file == null) {
            throw new IOException("Could not create file " + filename);
        }
        saveFile(context, file, contents);
    }

    public static void saveFile(Context context, DocumentFile file, String contents)
            throws IOException {

        /*
        ///File file = new File(path);
        String location = file.getParentFile().getPath();
        IoUtils.ensureDirectoriesInPathExist(location);

        file.createNewFile();
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
        outputStreamWriter.append(contents);
        outputStreamWriter.close();
        fileOutputStream.flush();
        fileOutputStream.close();*/

        Uri uri = file.getUri();
        ParcelFileDescriptor pfd = context.getContentResolver().
                openFileDescriptor(uri, "w");
        FileOutputStream fileOutputStream =
                new FileOutputStream(pfd.getFileDescriptor());
        fileOutputStream.write(contents.getBytes());
        fileOutputStream.close();
        pfd.close();
    }

    public static void saveFile(File file, String contents) throws IOException {
        file.createNewFile();
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
        outputStreamWriter.append(contents);
        outputStreamWriter.close();
        fileOutputStream.flush();
        fileOutputStream.close();
    }
}

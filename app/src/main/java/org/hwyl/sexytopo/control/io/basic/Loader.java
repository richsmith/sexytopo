package org.hwyl.sexytopo.control.io.basic;

import android.content.ContentResolver;
import android.content.Context;

import androidx.documentfile.provider.DocumentFile;

import org.hwyl.sexytopo.control.io.IoUtils;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;


@SuppressWarnings("UnnecessaryLocalVariable")
public class Loader {


    public static Survey loadSurvey(Context context, DocumentFile directory) throws Exception {
        return loadSurvey(context, directory, false);
    }


    public static Survey restoreAutosave(Context context, DocumentFile directory) throws Exception {
        return loadSurvey(context, directory, true);
    }


    public static Survey loadSurvey(Context context, DocumentFile directory, boolean restoreAutosave)
            throws Exception {
        Set<String> surveyNamesNotToLoad = new HashSet<>();
        return loadSurvey(context, directory, surveyNamesNotToLoad, restoreAutosave);
    }


    public static Survey loadSurvey(Context context, DocumentFile directory,
                                    Set<String> surveyNamesNotToLoad, boolean restoreAutosave)
            throws Exception {

        if (!IoUtils.isSurveyDirectory(directory)) {
            throw new Exception("Not a valid survey directory");
        }

        Survey survey = new Survey();
        survey.setDirectory(directory);
        loadSurveyData(context, survey, restoreAutosave);
        loadSketches(context, survey, restoreAutosave);
        surveyNamesNotToLoad.add(survey.getName());
        loadMetadata(context, survey, surveyNamesNotToLoad, restoreAutosave);
        survey.setSaved(true);
        return survey;
    }

    private static void loadMetadata(Context context, Survey survey,
                                     Set<String> surveyNamesNotToLoad, boolean restoreAutosave)
            throws Exception {

        DocumentFile file = IoUtils.getMetadataFile(context, survey);
        if (file.exists()) {
            String metadataText = slurpFile(context, file);
            MetadataTranslater.translateAndUpdate(
                    context, survey, metadataText, surveyNamesNotToLoad);
        }
    }


    private static void loadSurveyData(Context context, Survey survey,
                                       boolean restoreAutosave)
            throws Exception {

        DocumentFile file;
        if (restoreAutosave) {
            file = IoUtils.getAutosaveDataFile(context, survey);
        } else {
            file = IoUtils.getDataFile(context, survey);
        }

        if (file.exists()) {
            String text = slurpFile(context, file);
            SurveyJsonTranslater.populateSurvey(survey, text);
        }
    }


    private static void loadSketches(Context context, Survey survey, boolean restoreAutosave)
            throws IOException, JSONException {

        DocumentFile planFile = IoUtils.getPlanSketchFile(context, survey);
        if (planFile.exists()) {
            String planText = slurpFile(context, planFile);
            Sketch plan = SketchJsonTranslater.translate(survey, planText);
            survey.setPlanSketch(plan);
        }

        DocumentFile elevationFile = IoUtils.getExtendedElevationSketchFile(context, survey);
        if (elevationFile.exists()) {
            String elevationText = slurpFile(context, elevationFile);
            Sketch elevation = SketchJsonTranslater.translate(survey, elevationText);
            survey.setElevationSketch(elevation);
        }
    }

    public static String slurpFile(Context context, DocumentFile file)
            throws IOException {

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


}

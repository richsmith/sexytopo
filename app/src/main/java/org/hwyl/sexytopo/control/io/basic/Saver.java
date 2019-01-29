package org.hwyl.sexytopo.control.io.basic;

import android.content.Context;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.io.Util;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;


/**
 * The entry point for saving survey and sketch data.
 */
public class Saver {


    public static synchronized void save(Context context, Survey survey) throws Exception {

        if (survey.getName().equals("")) {
            throw new IllegalArgumentException("Not saved; survey name cannot be empty");
        } else if (survey.getName().contains("/")) {
            throw new IllegalArgumentException("Not saved; survey name cannot contain a slash");
        }

        saveMetadata(context, survey, SexyTopo.METADATA_EXTENSION);
        saveSurveyData(context, survey, SexyTopo.DATA_EXTENSION);
        saveSketch(context, survey, survey.getPlanSketch(), SexyTopo.PLAN_SKETCH_EXTENSION);
        saveSketch(context, survey, survey.getElevationSketch(),
                SexyTopo.EXT_ELEVATION_SKETCH_EXTENSION);
        survey.setSaved(true);

        Log.d("Saved survey " + survey.getName());
    }

    public static synchronized void autosave(Context context, Survey survey) throws Exception {
        saveMetadata(context, survey,
                SexyTopo.METADATA_EXTENSION + "." + SexyTopo.AUTOSAVE_EXTENSION);
        saveSurveyData(context, survey, SexyTopo.DATA_EXTENSION + "." + SexyTopo.AUTOSAVE_EXTENSION);
        saveSketch(context, survey, survey.getPlanSketch(),
                SexyTopo.PLAN_SKETCH_EXTENSION + "." + SexyTopo.AUTOSAVE_EXTENSION);
        saveSketch(context, survey, survey.getElevationSketch(),
                SexyTopo.EXT_ELEVATION_SKETCH_EXTENSION + "." + SexyTopo.AUTOSAVE_EXTENSION);
    }


    private static void saveSurveyData(Context context, Survey survey, String extension)
            throws IOException,JSONException {
        String path = Util.getPathForSurveyFile(context, survey.getName(), extension);
        String surveyText = SurveyJsonTranslater.toText(survey);
        saveFile(path, surveyText);
    }


    private static void saveSketch(Context context, Survey survey, Sketch sketch, String extension)
            throws IOException, JSONException {
        String path = Util.getPathForSurveyFile(context, survey.getName(), extension);
        String planText = SketchJsonTranslater.translate(sketch);
        saveFile(path, planText);
    }


    private static void saveMetadata(Context context, Survey survey, String extension) throws Exception {
        String path = Util.getPathForSurveyFile(context, survey.getName(), extension);
        String metadataText = MetadataTranslater.translate(survey);
        saveFile(path, metadataText);
    }


    public static void saveFile(String path, String contents) throws IOException {

        File file = new File(path);
        String location = file.getParentFile().getPath();
        Util.ensureDirectoriesInPathExist(location);

        file.createNewFile();
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
        outputStreamWriter.append(contents);
        outputStreamWriter.close();
        fileOutputStream.flush();
        fileOutputStream.close();


    }


}

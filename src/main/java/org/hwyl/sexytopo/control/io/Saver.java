package org.hwyl.sexytopo.control.io;

import android.content.Context;

import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by rls on 03/08/14.
 */
public class Saver {


    public static void save(Context context, Survey survey) throws IOException, JSONException {

        saveSurveyData(context, survey, "svx");
        saveSketch(context, survey, survey.getPlanSketch(), "plan", "json");
        saveSketch(context, survey, survey.getElevationSketch(), "elevation", "json");

    }


    public static void autosave(Context context, Survey survey) throws IOException, JSONException {
        saveSurveyData(context, survey, "svx.autosave");
        saveSketch(context, survey, survey.getPlanSketch(), "plan", "json.autosave");
        saveSketch(context, survey, survey.getElevationSketch(), "elevation", "json.autosave");
    }


    private static void saveSurveyData(Context context, Survey survey, String extension)
            throws IOException {
        String filename = Util.getPathForSurveyFile(survey.getName(), extension);
        String surveyText = SurvexExporter.export(survey);
        saveFile(context, filename, surveyText);
    }


    private static void saveSketch(Context context, Survey survey, Sketch sketch,
                                   String type, String extension)
            throws IOException, JSONException {
        String planFilename = Util.getPathForDataFile(survey.getName(), type, extension);
        String planText = SketchJsonTranslater.translate(sketch);
        saveFile(context, planFilename, planText);
    }


    public static void saveFile(Context context, String filename, String contents) throws IOException {

        Util.ensureSurveyDirectoryExists();
        Util.ensureDirectoriesInPathExist(filename);


        File surveyFile = new File(filename);


        FileOutputStream stream = new FileOutputStream(surveyFile);

        try {
            stream.write(contents.getBytes());
        } finally {
            stream.close();
        }
    }

}

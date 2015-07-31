package org.hwyl.sexytopo.control.io;

import android.content.Context;

import org.apache.commons.io.FileUtils;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;
import org.json.JSONException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * Created by rls on 03/08/14.
 */
public class Saver {


    public static void save(Context context, Survey survey) throws IOException, JSONException {

        saveSurveyData(context, survey, "svx");
        saveSketch(context, survey, survey.getPlanSketch(), SexyTopo.PLAN_SKETCH_EXTENSION);
        saveSketch(context, survey, survey.getElevationSketch(), SexyTopo.EXT_ELEVATION_SKETCH_EXTENSION);

    }


    public static void autosave(Context context, Survey survey) throws IOException, JSONException {
        saveSurveyData(context, survey, "svx.autosave");
        saveSketch(context, survey, survey.getPlanSketch(), SexyTopo.PLAN_SKETCH_EXTENSION + ".autosave");
        saveSketch(context, survey, survey.getElevationSketch(), SexyTopo.EXT_ELEVATION_SKETCH_EXTENSION + ".autosave");
    }


    private static void saveSurveyData(Context context, Survey survey, String extension)
            throws IOException {
        String path = Util.getPathForSurveyFile(survey.getName(), extension);
        String surveyText = SurvexExporter.export(survey);
        saveFile(context, path, surveyText);
    }


    private static void saveSketch(Context context, Survey survey, Sketch sketch, String extension)
            throws IOException, JSONException {
        String path = Util.getPathForSurveyFile(survey.getName(), extension);
        String planText = SketchJsonTranslater.translate(sketch);
        saveFile(context, path, planText);
    }



    public static void saveFile(Context context, String path, String contents) throws IOException {

        File filePath = new File(path);
        String location = filePath.getParentFile().getPath();
        Util.ensureDirectoriesInPathExist(location);


        FileUtils.writeStringToFile(filePath, contents);

    }

}

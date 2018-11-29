package org.hwyl.sexytopo.control.io.basic;

import android.content.Context;

import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.io.Util;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;


public class Loader {


    public static Survey loadSurvey(Context context, String name) throws Exception {
        return loadSurvey(context, name, false);
    }


    public static Survey restoreAutosave(Context context, String name) throws Exception {
        return loadSurvey(context, name, true);
    }


    public static Survey loadSurvey(Context context, String name, boolean restoreAutosave)
            throws Exception {
        Set<String> surveyNamesNotToLoad = new HashSet<>();
        return loadSurvey(context, name, surveyNamesNotToLoad, restoreAutosave);
    }


    public static Survey loadSurvey(Context context, String name,
                                    Set<String> surveyNamesNotToLoad, boolean restoreAutosave)
            throws Exception {
        Survey survey = new Survey(name);
        loadSurveyData(context, name, survey, restoreAutosave);
        loadSketches(context, survey, restoreAutosave);
        surveyNamesNotToLoad.add(name);
        loadMetadata(context, survey, surveyNamesNotToLoad, restoreAutosave);
        survey.setSaved(true);
        return survey;
    }

    private static void loadMetadata(Context context, Survey survey,
                                     Set<String> surveyNamesNotToLoad, boolean restoreAutosave)
            throws Exception {
        String metadataPath = Util.getPathForSurveyFile(context, survey.getName(),
                getExtension(SexyTopo.METADATA_EXTENSION, restoreAutosave));
        if (Util.doesFileExist(metadataPath)) {
            String metadataText = slurpFile(metadataPath);
            MetadataTranslater.translateAndUpdate(
                    context, survey, metadataText, surveyNamesNotToLoad);
        }
    }


    private static void loadSurveyData(Context context, String name, Survey survey,
                                       boolean restoreAutosave)
            throws Exception {

        String path = Util.getPathForSurveyFile(context, name,
                getExtension(SexyTopo.DATA_EXTENSION, restoreAutosave));

        if (new File(path).exists()) {
            String text = slurpFile(path);
            SurveyJsonTranslater.populateSurvey(survey, text);
        } else {
            // if there's no data file, we'll assume the survey was created with an older version
            // of SexyTopo
            String oldStylePath = Util.getPathForSurveyFile(context, name,
                    getExtension("svx", restoreAutosave));
            String text = slurpFile(oldStylePath);
            OldStyleLoader.parse(text, survey);
        }

    }


    private static void loadSketches(Context context, Survey survey, boolean restoreAutosave)
            throws JSONException {

        String planPath = Util.getPathForSurveyFile(context, survey.getName(),
                getExtension(SexyTopo.PLAN_SKETCH_EXTENSION, restoreAutosave));
        if (Util.doesFileExist(planPath)) {
            String planText = slurpFile(planPath);
            Sketch plan = SketchJsonTranslater.translate(survey, planText);
            survey.setPlanSketch(plan);
        }

        String elevationPath = Util.getPathForSurveyFile(context, survey.getName(),
                getExtension(SexyTopo.EXT_ELEVATION_SKETCH_EXTENSION, restoreAutosave));
        if (Util.doesFileExist(elevationPath)) {
            String elevationText = slurpFile(elevationPath);
            Sketch elevation = SketchJsonTranslater.translate(survey, elevationText);
            survey.setElevationSketch(elevation);
        }
    }


    public static String slurpFile(File file) {
        return slurpFile(file.getAbsolutePath());
    }

    public static String slurpFile(String filename) {

        StringBuilder text = new StringBuilder();

        try {
            File file = new File(filename);
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }

        } catch (Exception e) {
            Log.e("Error trying to read " + filename + ": " + e.getMessage());
        }
        return text.toString();
    }



    private static String getExtension(String baseExtention, boolean isAutosave) {
        if (isAutosave) {
            return baseExtention + "." + SexyTopo.AUTOSAVE_EXTENSION;
        } else {
            return baseExtention;
        }
    }


}

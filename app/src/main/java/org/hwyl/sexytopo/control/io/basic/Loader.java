package org.hwyl.sexytopo.control.io.basic;

import android.content.Context;

import androidx.documentfile.provider.DocumentFile;

import org.hwyl.sexytopo.control.io.IoUtils;
import org.hwyl.sexytopo.control.io.SurveyFile;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;
import org.json.JSONException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


public class Loader {


    public static Survey loadSurvey(Context context, DocumentFile directory) throws Exception {
        return loadSurvey(context, directory, false);
    }


    public static Survey loadAutosave(Context context, DocumentFile directory) throws Exception {
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

        SurveyFile surveyFile = SurveyFile.METADATA.get(survey);
        surveyFile = considerSwappingForAutosave(context, surveyFile, restoreAutosave);
        if (surveyFile.exists(context)) {
            String metadataText = surveyFile.slurp(context);
            MetadataTranslater.translateAndUpdate(
                    context, survey, metadataText, surveyNamesNotToLoad);
        }
    }


    private static void loadSurveyData(
            Context context, Survey survey, boolean restoreAutosave)
            throws Exception {
        SurveyFile surveyFile = SurveyFile.DATA.get(survey);
        surveyFile = considerSwappingForAutosave(context, surveyFile, restoreAutosave);
        if (surveyFile.exists(context)) {
            String text = surveyFile.slurp(context);
            SurveyJsonTranslater.populateSurvey(survey, text);
        }
    }


    private static void loadSketches(Context context, Survey survey, boolean restoreAutosave)
            throws IOException, JSONException {

        SurveyFile planFile = SurveyFile.SKETCH_PLAN.get(survey);
        planFile = considerSwappingForAutosave(context, planFile, restoreAutosave);
        if (planFile.exists(context)) {
            String planText = planFile.slurp(context);
            Sketch plan = SketchJsonTranslater.translate(survey, planText);
            survey.setPlanSketch(plan);
        }

        SurveyFile elevationFile = SurveyFile.SKETCH_EXT_ELEVATION.get(survey);
        if (elevationFile.exists(context)) {
            String elevationText = elevationFile.slurp(context);
            Sketch elevation = SketchJsonTranslater.translate(survey, elevationText);
            survey.setElevationSketch(elevation);
        }
    }

    private static SurveyFile considerSwappingForAutosave(
            Context context, SurveyFile surveyFile, boolean restoreAutosave) {
        if (restoreAutosave && surveyFile.exists(context)) {
                return SurveyFile.DATA.AUTOSAVE.get(surveyFile.getSurvey());
        } else {
            return surveyFile;
        }

    }


}

package org.hwyl.sexytopo.control.io.basic;

import android.content.Context;

import org.hwyl.sexytopo.control.activity.SexyTopoActivity;
import org.hwyl.sexytopo.control.io.SurveyFile;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;
import org.json.JSONException;

import java.io.IOException;


public class Saver {

    public static synchronized void save(Context context, Survey survey) throws Exception {
        saveMetadata(context, survey, SurveyFile.METADATA.get(survey));
        saveSurveyData(context, survey, SurveyFile.DATA.get(survey));
        savePlanSketch(context, survey, SurveyFile.SKETCH_PLAN.get(survey));
        saveElevationSketch(context, survey, SurveyFile.SKETCH_EXT_ELEVATION.get(survey));
        survey.setSaved(true);
    }

    public static synchronized void autosave(Context context, Survey survey) throws Exception {
        saveMetadata(context, survey, SurveyFile.METADATA.AUTOSAVE.get(survey));
        saveSurveyData(context, survey, SurveyFile.DATA.AUTOSAVE.get(survey));
        savePlanSketch(context, survey, SurveyFile.SKETCH_PLAN.AUTOSAVE.get(survey));
        saveElevationSketch(context, survey, SurveyFile.SKETCH_EXT_ELEVATION.AUTOSAVE.get(survey));
        survey.setAutosaved(true);
    }

    private static void saveSurveyData(
            Context context, Survey survey, SurveyFile surveyFile)
            throws IOException, JSONException {
        String versionName = SexyTopoActivity.getVersionName(context);
        int versionCode = SexyTopoActivity.getVersionCode(context);
        String surveyText = SurveyJsonTranslater.toText(survey, versionName, versionCode);
        surveyFile.save(context, surveyText);
    }

    private static void savePlanSketch(
            Context context, Survey survey, SurveyFile surveyFile)
            throws IOException, JSONException {
        Sketch sketch = survey.getPlanSketch();
        String planText = SketchJsonTranslater.translate(sketch);
        surveyFile.save(context, planText);
    }

    private static void saveElevationSketch(
            Context context, Survey survey, SurveyFile surveyFile)
            throws IOException, JSONException {
        Sketch sketch = survey.getElevationSketch();
        String eeText = SketchJsonTranslater.translate(sketch);
        surveyFile.save(context, eeText);
    }

    private static void saveMetadata(
            Context context, Survey survey, SurveyFile surveyFile)
            throws Exception {
        String metadataText = MetadataTranslater.translate(survey);
        surveyFile.save(context, metadataText);
    }
}

package org.hwyl.sexytopo.control.io.basic;

import android.content.Context;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.io.IoUtils;
import org.hwyl.sexytopo.control.io.SurveyFile;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;
import org.json.JSONException;

public class Loader {

    public static Survey loadSurvey(Context context, DocumentFile directory) throws Exception {
        return loadSurvey(context, directory, false);
    }

    public static Survey loadAutosave(Context context, DocumentFile directory) throws Exception {
        return loadSurvey(context, directory, true);
    }

    public static Survey loadSurvey(
            Context context, DocumentFile directory, boolean restoreAutosave) throws Exception {
        Set<Uri> surveyUrisNotToLoad = new HashSet<>();
        return loadSurvey(context, directory, surveyUrisNotToLoad, restoreAutosave);
    }

    public static Survey loadSurvey(
            Context context,
            DocumentFile directory,
            Set<Uri> surveyUrisNotToLoad,
            boolean restoreAutosave)
            throws Exception {

        if (!IoUtils.isSurveyDirectory(directory)) {
            throw new Exception(context.getString(R.string.file_load_error_not_survey));
        }

        Survey survey = new Survey();
        survey.setDirectory(directory);
        loadSurveyData(context, survey, restoreAutosave);
        loadSketches(context, survey, restoreAutosave);
        surveyUrisNotToLoad.add(survey.getUri());
        loadMetadata(context, survey, surveyUrisNotToLoad, restoreAutosave);
        survey.setSaved(true);
        return survey;
    }

    private static void loadMetadata(
            Context context, Survey survey, Set<Uri> surveyUrisNotToLoad, boolean restoreAutosave)
            throws Exception {

        SurveyFile surveyFile = SurveyFile.METADATA.get(survey);
        surveyFile = considerSwappingForAutosave(context, surveyFile, restoreAutosave);
        if (surveyFile.exists(context)) {
            Log.i(context.getString(R.string.file_loading_file, surveyFile.getFilename()));
            String metadataText = surveyFile.slurp(context);
            MetadataTranslater.translateAndUpdate(
                    context, survey, metadataText, surveyUrisNotToLoad);
        }
    }

    private static void loadSurveyData(Context context, Survey survey, boolean restoreAutosave)
            throws Exception {
        SurveyFile surveyFile = SurveyFile.DATA.get(survey);
        surveyFile = considerSwappingForAutosave(context, surveyFile, restoreAutosave);
        if (surveyFile.exists(context)) {
            Log.i(R.string.file_loading_file, surveyFile.getFilename());
            String text = surveyFile.slurp(context);
            SurveyJsonTranslater.populateSurvey(survey, text);
        }
    }

    private static void loadSketches(Context context, Survey survey, boolean restoreAutosave)
            throws IOException, JSONException {

        SurveyFile planFile = SurveyFile.SKETCH_PLAN.get(survey);
        planFile = considerSwappingForAutosave(context, planFile, restoreAutosave);
        boolean planLoaded = planFile.exists(context);
        if (planLoaded) {
            Log.i(R.string.file_loading_file, planFile.getFilename());
            String planText = planFile.slurp(context);
            Sketch plan = SketchJsonTranslater.translate(survey, planText);
            survey.setPlanSketch(plan);
        }

        SurveyFile elevationFile = SurveyFile.SKETCH_EXT_ELEVATION.get(survey);
        if (elevationFile.exists(context)) {
            Log.i(R.string.file_loading_file, planFile.getFilename());
            String elevationText = elevationFile.slurp(context);
            Sketch elevation = SketchJsonTranslater.translate(survey, elevationText);
            survey.setElevationSketch(elevation);
        }

        if (planLoaded) {
            // The cross-section scale is survey-wide and the plan sketch holds the canonical
            // value, so make sure the elevation sketch (which may predate that) agrees with it.
            survey.syncCrossSectionScale();
        }
    }

    private static SurveyFile considerSwappingForAutosave(
            Context context, SurveyFile surveyFile, boolean restoreAutosave) {

        if (restoreAutosave) {
            SurveyFile autosaveVersion = surveyFile.getAutosaveVersion();
            if (autosaveVersion.exists(context)) {
                return autosaveVersion;
            }
        }

        return surveyFile;
    }
}

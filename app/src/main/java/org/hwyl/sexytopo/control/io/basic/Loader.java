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
        surveyUrisNotToLoad.add(survey.getUri());

        SurveyTextSource surveyFiles =
                type -> {
                    SurveyFile surveyFile = type.get(survey);
                    surveyFile = considerSwappingForAutosave(context, surveyFile, restoreAutosave);
                    if (!surveyFile.exists(context)) {
                        return null;
                    }
                    Log.i(R.string.file_loading_file, surveyFile.getFilename());
                    String text = surveyFile.slurp(context);
                    return text;
                };
        populate(context, survey, surveyFiles, surveyUrisNotToLoad);

        survey.setSaved(true);
        return survey;
    }

    /** Supplies the contents of each of a survey's files, or null if there isn't one. */
    public interface SurveyTextSource {
        String read(SurveyFile.SurveyFileType type) throws IOException;
    }

    public static void populate(
            Context context, Survey survey, SurveyTextSource source, Set<Uri> surveyUrisNotToLoad)
            throws Exception {
        for (SurveyFile.SurveyFileType type : SurveyFile.ALL_DATA_TYPES) {
            String text = source.read(type);
            if (text != null) {
                fromText(context, survey, type, text, surveyUrisNotToLoad);
            }
        }
    }

    private static void fromText(
            Context context,
            Survey survey,
            SurveyFile.SurveyFileType type,
            String text,
            Set<Uri> surveyUrisNotToLoad)
            throws Exception {
        if (type == SurveyFile.DATA) {
            SurveyJsonTranslater.populateSurvey(survey, text);
        } else if (type == SurveyFile.METADATA) {
            MetadataTranslater.translateAndUpdate(context, survey, text, surveyUrisNotToLoad);
        } else if (type == SurveyFile.SKETCH_PLAN) {
            Sketch plan = SketchJsonTranslater.translate(survey, text);
            survey.setPlanSketch(plan);
        } else if (type == SurveyFile.SKETCH_EXT_ELEVATION) {
            Sketch elevation = SketchJsonTranslater.translate(survey, text);
            survey.setElevationSketch(elevation);
        } else {
            throw new IllegalArgumentException("Not a survey data file: " + type.getExtension());
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

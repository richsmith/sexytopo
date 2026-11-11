package org.hwyl.sexytopo.control.io.basic;

import android.content.Context;
import org.hwyl.sexytopo.control.activity.SexyTopoActivity;
import org.hwyl.sexytopo.control.io.SurveyFile;
import org.hwyl.sexytopo.model.survey.Survey;

public class Saver {

    public static synchronized void save(Context context, Survey survey) throws Exception {
        for (SurveyFile.SurveyFileType type : SurveyFile.ALL_DATA_TYPES) {
            String text = toText(context, survey, type);
            SurveyFile surveyFile = type.get(survey);
            surveyFile.save(context, text);
        }
        survey.setSaved(true);
    }

    public static synchronized void autosave(Context context, Survey survey) throws Exception {
        for (SurveyFile.SurveyFileType type : SurveyFile.ALL_DATA_TYPES) {
            String text = toText(context, survey, type);
            SurveyFile autosaveFile = type.AUTOSAVE.get(survey);
            autosaveFile.save(context, text);
        }
        survey.setAutosaved(true);
    }

    public static String toText(Context context, Survey survey, SurveyFile.SurveyFileType type)
            throws Exception {
        String versionName = SexyTopoActivity.getVersionName(context);
        int versionCode = SexyTopoActivity.getVersionCode(context);

        if (type == SurveyFile.DATA) {
            return SurveyJsonTranslater.toText(survey, versionName, versionCode);
        } else if (type == SurveyFile.METADATA) {
            return MetadataTranslater.translate(survey, versionName, versionCode);
        } else if (type == SurveyFile.SKETCH_PLAN) {
            return SketchJsonTranslater.translate(
                    survey.getPlanSketch(), survey, versionName, versionCode);
        } else if (type == SurveyFile.SKETCH_EXT_ELEVATION) {
            return SketchJsonTranslater.translate(
                    survey.getElevationSketch(), survey, versionName, versionCode);
        } else {
            throw new IllegalArgumentException("Not a survey data file: " + type.getExtension());
        }
    }
}

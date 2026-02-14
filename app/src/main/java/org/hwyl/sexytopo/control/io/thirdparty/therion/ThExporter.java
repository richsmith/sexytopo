package org.hwyl.sexytopo.control.io.thirdparty.therion;

import android.content.Context;

import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.activity.SexyTopoActivity;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurveyFormat;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionUtil;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.List;

public class ThExporter {

    public static String getContent(Context context, Survey survey, List<String> th2Files) {
        StringBuilder builder = new StringBuilder();

        // Encoding
        builder.append(TherionExporter.getEncodingText()).append("\n");

        // Survey block
        builder.append("survey ").append(survey.getName()).append("\n");

        // Creation comment
        String versionInfo = SexyTopoConstants.APP_NAME + " " + SexyTopoActivity.getVersionName(context);
        builder.append(SurvexTherionUtil.getCreationComment(TherionExporter.COMMENT_CHAR, versionInfo)).append("\n\n");

        // Input files
        builder.append(SurvexTherionUtil.getInputText(th2Files)).append("\n\n");

        // Metadata (instrument is now in Trip, not passed separately)
        builder.append(SurvexTherionUtil.getMetadata(survey, TherionExporter.COMMENT_CHAR, SurveyFormat.THERION)).append("\n");

        // Centreline block
        builder.append("centreline\n");
        builder.append(SurvexTherionUtil.getStationCommentsData(survey, SurveyFormat.THERION));
        builder.append(SurvexTherionUtil.getCentrelineData(survey, TherionExporter.COMMENT_CHAR, SurveyFormat.THERION));
        builder.append(SurvexTherionUtil.getExtendedElevationExtensions(survey, SurveyFormat.THERION));
        builder.append("endcentreline\n");

        // End survey
        builder.append("endsurvey\n");

        return builder.toString();
    }

    public static String updateOriginalContent(
            Survey survey, String originalFileContent, List<String> th2Files) {

        // Replace centreline block
        String centrelineText =
            "centreline\n" +
            SurvexTherionUtil.getStationCommentsData(survey, SurveyFormat.THERION) +
            SurvexTherionUtil.getCentrelineData(survey, TherionExporter.COMMENT_CHAR, SurveyFormat.THERION) +
            SurvexTherionUtil.getExtendedElevationExtensions(survey, SurveyFormat.THERION) +
            "endcentreline\n";
        String newContent = replaceCentreline(originalFileContent, centrelineText);

        // Replace input statements
        String inputText = SurvexTherionUtil.getInputText(th2Files);
        newContent = replaceInputsText(newContent, inputText);

        return newContent;
    }

    static String replaceCentreline(String original, String replacementText) {
        return original.replaceFirst(
                "(?s)(\\s*?(centreline|centerline)(.*)(endcentreline|endcenterline)\\s*)",
                replacementText);
    }

    static String replaceInputsText(String original, String replacementText) {
        return original.replaceFirst("(?m)(^input .*\\n)+", replacementText);
    }

}

package org.hwyl.sexytopo.control.io.thirdparty.survex;

import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurveyFormat;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionUtil;
import org.hwyl.sexytopo.control.io.translation.SingleFileExporter;
import org.hwyl.sexytopo.model.survey.Survey;

public class SurvexExporter extends SingleFileExporter {

    public static final char COMMENT_CHAR = ';';

    // StringBuilder is more efficient than string concatenation for multiple appends
    @SuppressWarnings("StringBufferReplaceableByString")
    public String getContent(Survey survey) {
        StringBuilder builder = new StringBuilder();

        // Begin survey block
        builder.append("*begin ").append(survey.getName()).append("\n");

        // Creation comment (no version info available without Context)
        builder.append(SurvexTherionUtil.getCreationComment(COMMENT_CHAR, "SexyTopo")).append("\n\n");

        // Metadata (instrument is now in Trip, not passed separately)
        builder.append(SurvexTherionUtil.getMetadata(survey, COMMENT_CHAR, SurveyFormat.SURVEX)).append("\n");

        // Station comments data block
        builder.append(SurvexTherionUtil.getStationCommentsData(survey, SurveyFormat.SURVEX));

        // Centreline data
        builder.append(SurvexTherionUtil.getCentrelineData(survey, COMMENT_CHAR, SurveyFormat.SURVEX));

        // Extended elevation
        builder.append("\n");
        builder.append(SurvexTherionUtil.getExtendedElevationExtensions(survey, SurveyFormat.SURVEX));

        // End survey block
        builder.append("*end ").append(survey.getName()).append("\n");

        return builder.toString();
    }

    @Override
    public String getFileExtension() {
        return "svx";
    }

    @Override
    public String getExportTypeName(Context context) {
        return context.getString(R.string.third_party_survex);
    }

    public String getMimeType() {
        return "text/svx";
    }

    @Override
    public String getExportDirectoryName() {
        return "survex";
    }
}

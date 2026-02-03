package org.hwyl.sexytopo.control.io.thirdparty.survex;

import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionUtil;
import org.hwyl.sexytopo.control.io.translation.SingleFileExporter;
import org.hwyl.sexytopo.model.survey.Survey;


public class SurvexExporter extends SingleFileExporter {

    public static final char COMMENT_CHAR = ';';
    public static final String SYNTAX_MARKER = "*";

    public String getContent(Survey survey) {
        StringBuilder builder = new StringBuilder();

        // Begin survey block
        builder.append("*begin ").append(survey.getName()).append("\n");
        
        // Creation comment (no version info available without Context)
        builder.append(SurvexTherionUtil.getCreationComment(COMMENT_CHAR, "SexyTopo")).append("\n\n");
        
        // Metadata (instrument is now in Trip, not passed separately)
        builder.append(SurvexTherionUtil.getMetadata(survey, SYNTAX_MARKER, COMMENT_CHAR)).append("\n");
        
        // Station comments data block
        builder.append(SurvexTherionUtil.getStationCommentsData(survey, SYNTAX_MARKER));
        
        // Centreline data
        builder.append(SurvexTherionUtil.getCentrelineData(survey, SYNTAX_MARKER, COMMENT_CHAR, true));
        
        // Extended elevation
        builder.append("\n");
        builder.append(SurvexTherionUtil.getExtendedElevationExtensions(survey, SYNTAX_MARKER));
        
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

package org.hwyl.sexytopo.control.io.thirdparty.survex;

import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionUtil;
import org.hwyl.sexytopo.control.io.translation.SingleFileExporter;
import org.hwyl.sexytopo.control.util.GraphToListTranslator;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.List;


public class SurvexExporter extends SingleFileExporter {

    public static final char COMMENT_CHAR = ';';

    protected static final GraphToListTranslator graphToListTranslator =
            new GraphToListTranslator();

    public String getContent(Survey survey) {

        StringBuilder builder = new StringBuilder();

        builder.append("*alias station - ..\n\n");

        List<GraphToListTranslator.SurveyListEntry> list =
                graphToListTranslator.toChronoListOfSurveyListEntries(survey);

        for (GraphToListTranslator.SurveyListEntry entry : list) {
            SurvexTherionUtil.formatEntry(builder, entry, COMMENT_CHAR);
            builder.append("\n");
        }

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


    @Override
    public String getExportDirectoryName() {
        return "survex";
    }
}

package org.hwyl.sexytopo.control.io.thirdparty.survex;

import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionUtil;
import org.hwyl.sexytopo.control.io.translation.SingleFileExporter;
import org.hwyl.sexytopo.control.util.GraphToListTranslator;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.List;


public class SurvexExporter extends SingleFileExporter {

    protected static GraphToListTranslator graphToListTranslator = new GraphToListTranslator();

    public String getContent(Survey survey) {

        StringBuilder builder = new StringBuilder();

        List<GraphToListTranslator.SurveyListEntry> list =
                graphToListTranslator.toListOfSurveyListEntries(survey);

        for (GraphToListTranslator.SurveyListEntry entry : list) {
            SurvexTherionUtil.formatEntry(builder, entry);
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

package org.hwyl.sexytopo.control.io.thirdparty.survex;

import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.translation.SingleFileExporter;
import org.hwyl.sexytopo.control.util.GraphToListTranslator;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.table.TableCol;

import java.util.List;
import java.util.Locale;
import java.util.Map;


public class SurvexExporter extends SingleFileExporter {

    private static GraphToListTranslator graphToListTranslator = new GraphToListTranslator();

    public String getContent(Survey survey) {

        List<GraphToListTranslator.SurveyListEntry> list = graphToListTranslator.toListOfSurveyListEntries(survey);
        List<Map<TableCol, Object>> data = graphToListTranslator.toListOfColMaps(survey);

        String text = "";
        for (Map map : data) {
            text += getEntry(TableCol.FROM, map);
            text += getEntry(TableCol.TO, map);
            text += getEntry(TableCol.DISTANCE, map);
            text += getEntry(TableCol.AZIMUTH, map);
            text += getEntry(TableCol.INCLINATION, map);
            text += "\n";
        }

        return text;
    }


    private static String getEntry(TableCol tableCol, Map map) {
        Object value = map.get(tableCol);
        String text = tableCol.format(value, Locale.UK);
        return text + "\t";
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

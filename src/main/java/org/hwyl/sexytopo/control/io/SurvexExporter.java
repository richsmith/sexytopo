package org.hwyl.sexytopo.control.io;

import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.table.TableCol;
import org.hwyl.sexytopo.util.GraphToListTranslator;

import java.util.List;
import java.util.Map;

/**
 * Created by rls on 03/08/14.
 */
public class SurvexExporter {

    private static GraphToListTranslator graphToListTranslator = new GraphToListTranslator();

    public static String export(Survey survey) {

        List<GraphToListTranslator.SurveyListEntry> list = graphToListTranslator.toListOfSurveyListEntries(survey);
        List<Map<TableCol, Object>> data = graphToListTranslator.toListOfColMaps(survey);

        String text = "";
        for (Map map : data) {
            text += getEntry(TableCol.FROM, map);
            text += getEntry(TableCol.TO, map);
            text += getEntry(TableCol.DISTANCE, map);
            text += getEntry(TableCol.BEARING, map);
            text += getEntry(TableCol.INCLINATION, map);
            text += "\n";
        }

        return text;
    }


    private static String getEntry(TableCol tableCol, Map map) {
        Object value = map.get(tableCol);
        String text = tableCol.format(value);
        return text + "\t";
    }

}

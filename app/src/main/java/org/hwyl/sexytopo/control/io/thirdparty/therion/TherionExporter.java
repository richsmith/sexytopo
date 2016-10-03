package org.hwyl.sexytopo.control.io.thirdparty.therion;

import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.thirdparty.survex.SurvexExporter;
import org.hwyl.sexytopo.control.io.translation.Exporter;
import org.hwyl.sexytopo.model.survey.Survey;


public class TherionExporter implements Exporter {

    private TherionExporter instance = new TherionExporter();

    public TherionExporter getInstance() {
        return instance;
    }


    public String export(Survey survey) {

        String centerlineText =
                "centreline\n\n" +
                        indent(getCentreline(survey)) + "\n\n" +
                        "endcentreline";

        String surveyText =
                "survey " + survey.getName() + "\n\n" +
                        indent(centerlineText) +
                        "\n\nendsurvey";

        return surveyText;
    }


    @Override
    public String getExportTypeName(Context context) {
        return context.getString(R.string.third_party_therion);
    }


    public static String indent(String text) {
        String indented = "";
        String[] lines = text.split("\n");
        for (String line : lines) {
            indented += "\t" + line + "\n";
        }
        return indented;
    }

    private static String getCentreline(Survey survey) {
        return "data normal from to length compass clino\n\n" +
                new SurvexExporter().export(survey);
    }
}

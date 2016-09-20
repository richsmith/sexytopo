package org.hwyl.sexytopo.control.io.thirdparty.therion;

import org.hwyl.sexytopo.control.io.thirdparty.survex.SurvexExporter;
import org.hwyl.sexytopo.model.survey.Survey;

/**
 * Created by rls on 29/06/15.
 */
public class OldTherionExporter {

    private OldTherionExporter instance = new OldTherionExporter();

    public OldTherionExporter getInstance() {
        return instance;
    }


    public static String export(Survey survey) {

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

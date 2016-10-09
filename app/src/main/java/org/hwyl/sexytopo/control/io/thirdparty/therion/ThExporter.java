package org.hwyl.sexytopo.control.io.thirdparty.therion;

import org.hwyl.sexytopo.control.io.thirdparty.survex.SurvexExporter;
import org.hwyl.sexytopo.model.survey.Survey;


public class ThExporter {

    public static String getContent(Survey survey) {

        String text =
            TherionExporter.getEncodingText() + "\n" +
            getSurveyText(survey) + "\n";

        return text;
    }

    private static String getSurveyText(Survey survey) {
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
            new SurvexExporter().getContent(survey);
    }


}

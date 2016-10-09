package org.hwyl.sexytopo.control.io.thirdparty.therion;

import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.Arrays;


public class ThconfigExporter {

    public static String getContent(Survey survey) {

        String name = survey.getName();

        String[] lines = new String[] {
            TherionExporter.getEncodingText(),
            "source " + name + ".th",
            "getContent map -proj plan -layout local -o " + name + "-plan.pdf",
            "getContent map -proj extended -layout local -o " + name + "-ee.pdf"
        };

        return TextTools.join(Arrays.asList(lines), "\n\n");
    }

}

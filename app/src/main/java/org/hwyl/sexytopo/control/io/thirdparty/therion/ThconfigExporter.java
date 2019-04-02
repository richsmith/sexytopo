package org.hwyl.sexytopo.control.io.thirdparty.therion;

import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.Arrays;


public class ThconfigExporter {

    public static final String DEFAULT_LAYOUT =
        "layout local\n" +
        "  debug off\n" +
        "  map-header 0 0 off\n" +
        "  # symbol-hide group cave-centreline\n" +
        "endlayout";

    public static String getContent(Survey survey) {

        String name = survey.getName();

        String[] lines = new String[] {
            TherionExporter.getEncodingText(),
            DEFAULT_LAYOUT,
            "source \"" + name + ".th\"",
            "export model -fmt survex -o \"" + name + "-th.3d\"",
            "# export map -proj plan -layout local -o \"" + name + "-plan.pdf\"",
            "# export map -proj extended -layout local -o \"" + name + "-ee.pdf\""
        };

        return TextTools.join("\n\n", Arrays.asList(lines));
    }

}

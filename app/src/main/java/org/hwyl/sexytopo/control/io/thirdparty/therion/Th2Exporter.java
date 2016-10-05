package org.hwyl.sexytopo.control.io.thirdparty.therion;

import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.LinkedList;
import java.util.List;


public class Th2Exporter {

    public String asText(Survey survey) {
        List<String> lines = new LinkedList<>();
        lines.add(getHeader());
        lines.add(getXviBlock(survey));
        return TextTools.join(lines, "\n\n");
    }

    public String getXviBlock(Survey survey) {
        List<String> lines = new LinkedList<>();
        lines.add(getXviLine("xth_me_area_adjust", 0, 0, 0, 0)); // map box bottom left x1,y1 to top right x2, y2?
        lines.add(getXviLine("zoom_to", 100)); // zoom adjustment (100%?)
        lines.add(getXviLine("xth_me_image_insert", "{}", "{}", survey.getName() + ".xvi", 0, "{}"));
        return TextTools.join(lines, "\n");
    }

    private static String getXviLine(String command, Object... values) {
        List<String> fields = new LinkedList<>();
        fields.add("##XTHERION##");
        fields.add(command);
        for (Object value : values) {
            fields.add(value.toString());
        }
        return TextTools.join(fields, " ");
    }

    public String getHeader() {
        return "encoding utf-8";
    }

}

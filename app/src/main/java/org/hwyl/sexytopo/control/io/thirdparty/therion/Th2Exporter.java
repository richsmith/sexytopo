package org.hwyl.sexytopo.control.io.thirdparty.therion;

import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.ArrayList;
import java.util.List;


public class Th2Exporter {

    public static String getContent(
            Survey survey, double scale, String filename, Space<Coord2D> projection) {
        List<String> lines = new ArrayList<>();
        lines.add(TherionExporter.getEncodingText());
        lines.add(getXviBlock(survey, scale, filename, projection));
        return TextTools.join("\n\n", lines);
    }

    public static String updateOriginalContent(
            Survey survey, double scale, String filename, Space<Coord2D> projection,
            String originalFileContent) {
        String newContent = stripXTherion(originalFileContent);
        newContent += "\n" + getXviBlock(survey, scale, filename, projection);
        return newContent;
    }


    public static String stripXTherion(String originalFileContent) {
        return originalFileContent.replaceAll("(\\s*##XTHERION##.*?)", "");
    }


    public static String getXviBlock(
            Survey survey, double scale, String filename, Space<Coord2D> projection) {

        List<String> lines = new ArrayList<>();

        SketchDimensions dimensions = SketchDimensions.getDimensions(projection);
        double width = dimensions.getWidth();
        double height = dimensions.getHeight();

        double widthBorder = 0.2 * width;
        double heightBorder = 0.2 * height;

        // see https://bitbucket.org/AndrewA/topparser/src/b85fe3ea07a51d8c4e30ced88a643f97fc2127d3/Writeth2.py?at=default&fileviewer=file-view-default
        lines.add(getXviLine("xth_me_area_adjust",
            TextTools.formatTo2dp((-1 * widthBorder) * scale),
            TextTools.formatTo2dp((-1 * heightBorder) * scale),
            TextTools.formatTo2dp((width + widthBorder) * scale),
            TextTools.formatTo2dp((height + heightBorder) * scale)));

        lines.add(getXviLine("zoom_to", 50)); // zoom adjustment (100%?)

        String firstStation = survey.getOrigin().getName();

        lines.add(getXviLine("xth_me_image_insert",
            "{" + ((-1 * dimensions.minX + widthBorder / 2) * scale) + " 1 1.0}",
            "{" + ((-1 * dimensions.minY + heightBorder / 2) * scale) + " " + firstStation + "}",
            "\"" + filename + "\"",
            0,
            "{}"));

        return TextTools.join("\n", lines);
    }


    private static String getXviLine(String command, Object... values) {
        List<String> fields = new ArrayList<>();
        fields.add("##XTHERION##");
        fields.add(command);
        for (Object value : values) {
            fields.add(value.toString());
        }
        return TextTools.join(" ", fields);
    }


}

package org.hwyl.sexytopo.control.io.thirdparty.therion;

import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.graph.BoundingBox;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.ArrayList;
import java.util.List;


/**
 * Exports the th2 file for Therion.
 * This file contains:
 * - XTHERION commands which position the XVI image
 * - scrap commands
 * - semantic data (e.g. points like stalactites)
 */
public class Th2Exporter {

    public static String getContent(
            Survey survey, double scale, String xviFilename, BoundingBox dimensions) {
        List<String> lines = new ArrayList<>();
        lines.add(TherionExporter.getEncodingText());
        lines.add(getXviBlock(survey, scale, xviFilename, dimensions));
        return TextTools.join("\n\n", lines);
    }

    public static String updateOriginalContent(
            Survey survey, double scale, String filename, BoundingBox dimensions,
            String originalFileContent) {
        String newContent = stripXTherion(originalFileContent);
        newContent += "\n" + getXviBlock(survey, scale, filename, dimensions);
        return newContent;
    }


    public static String stripXTherion(String originalFileContent) {
        return originalFileContent.replaceAll("(\\s*##XTHERION##.*?)", "");
    }


    public static String getXviBlock(
            Survey survey, double scale, String filename, BoundingBox dimensions) {

        List<String> lines = new ArrayList<>();

        double width = dimensions.getWidth();
        double height = dimensions.getHeight();

        double widthBorder = 0.2 * width;
        double heightBorder = 0.2 * height;

        // see https://bitbucket.org/AndrewA/topparser/src/b85fe3ea07a51d8c4e30ced88a643f97fc2127d3/Writeth2.py?at=default&fileviewer=file-view-default

        /*
*/


        /*
        From a file in Therion:
        fprintf(pltf,
                "##XTHERION## xth_me_image_insert {%.2f 1 1.0} {%.2f {}} %s 0 {}\n",
                nx, ny, new_file.filename().string().c_str());

        Presumably the parameters required here are:
        {nx 1 1.0}
        {ny 1}
        XVI filename
        0 {}

        So what is nx? min x?

         */

        String firstStation = survey.getOrigin().getName();

        lines.add(getXviLine("xth_me_image_insert",
                "{" + dimensions.getLeft() + " 1 1.0}",
                "{" + dimensions.getBottom() + " " + firstStation + "}",
                "\"" + filename + "\"",
                0,
                "{}"));


        /*
        From a file in Therion:
        fprintf(pltf,
                "##XTHERION## xth_me_area_adjust %.0f %.0f %0.f %0.f\n",
                xmin - 0.1 * (xmax - xmin),
                ymin - 0.1 * (ymax - ymin),
                xmax + 0.1 * (xmax - xmin),
                ymax + 0.1 * (ymax - ymin));

         Presumably the parameters required here are xmin, ymin, xmax ymax;
         0.1 is just for a border.

         */
        lines.add(getXviLine("xth_me_area_adjust",
                TextTools.formatTo2dp(dimensions.getLeft()),
                TextTools.formatTo2dp(dimensions.getTop()),
                TextTools.formatTo2dp(dimensions.getRight()),
                TextTools.formatTo2dp(dimensions.getBottom())));

        lines.add(getXviLine("xth_me_area_zoom_to", 50)); // zoom adjustment (100%?)


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

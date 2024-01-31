package org.hwyl.sexytopo.control.io.thirdparty.therion;

import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.common.Shape;
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
            Survey survey, double scale, String xviFilename, Shape innerFrame, Shape outerFrame) {
        List<String> lines = new ArrayList<>();
        lines.add(TherionExporter.getEncodingText());
        lines.add(getXviBlock(survey, scale, xviFilename, innerFrame, outerFrame));
        return TextTools.join("\n\n", lines);
    }

    public static String updateOriginalContent(
            Survey survey, double scale, String filename, Shape innerFrame, Shape outerFrame,
            String originalFileContent) {
        String newContent = stripXTherion(originalFileContent);
        newContent += "\n" + getXviBlock(survey, scale, filename, innerFrame, outerFrame);
        return newContent;
    }


    public static String stripXTherion(String originalFileContent) {
        return originalFileContent.replaceAll("(\\s*##XTHERION##.*?)", "");
    }


    public static String getXviBlock(
            Survey survey, double scale, String filename, Shape innerFrame, Shape outerFrame) {

        List<String> lines = new ArrayList<>();

        // see https://bitbucket.org/AndrewA/topparser/src/b85fe3ea07a51d8c4e30ced88a643f97fc2127d3/Writeth2.py?at=default&fileviewer=file-view-default

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


        // xth_me_area_adjust <Xmin> <Ymin> <Xmax> <Ymax>
        // Xmin, Ymin and Xmax, Ymax are cartesian coordinates of lower left and upper right
        // corners of drawing area
        lines.add(getXviLine("xth_me_area_adjust",
                TextTools.formatTo2dp(outerFrame.getLeft()),
                TextTools.formatTo2dp(outerFrame.getBottom()),
                TextTools.formatTo2dp(outerFrame.getRight()),
                TextTools.formatTo2dp(outerFrame.getTop())));


        //  xth_me_image_insert {<Xpos> <visibility> <gamma>} {<Ypos> <root>} {<filename>} 0 {}
        //  <Xpos> <Ypos> is the position of the 0,0 point of XVI coordinate system
        //  <visibility> - 0 image is hidden / 1 image is shown
        //  <gamma> - image gamma
        //  <root> - root station name. Can be omitted.
        //  <filename> - name of image file
        //  0 {} - image identifiers, can be 0 {} for all images
        String firstStation = survey.getOrigin().getName();
        lines.add(getXviLine("xth_me_image_insert",
                "{" + innerFrame.getLeft() + " 1 1.0}",
                "{" + innerFrame.getBottom() + " " + firstStation + "}",
                "\"" + filename + "\"",
                0,
                "{}"));


        // xth_me_area_zoom_to <zoom>
        // where <zoom> is the default zoom factor, when drawing is open in xtherion.
        // Should be 25,50,100,200,400.
        lines.add(getXviLine("xth_me_area_zoom_to", 50));


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

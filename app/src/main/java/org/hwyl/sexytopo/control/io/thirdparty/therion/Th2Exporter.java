package org.hwyl.sexytopo.control.io.thirdparty.therion;

import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.common.Shape;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.AutoScalableDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.Symbol;
import org.hwyl.sexytopo.model.sketch.SymbolDetail;
import org.hwyl.sexytopo.model.sketch.TextDetail;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * Exports the th2 file for Therion.
 * This file contains:
 * - XTHERION commands which position the XVI image
 * - scrap commands
 * - semantic data (e.g. points like stalactites)
 */
public class Th2Exporter {

    public static String getContent(
        Survey survey, Projection2D projection, Space<Coord2D> space, String xviFilename,
        Shape innerFrame, Shape outerFrame, float scale) {
        List<String> sections = new ArrayList<>();
        sections.add(TherionExporter.getEncodingText());
        sections.add(getXviBlock(survey, space, xviFilename, outerFrame));

        Sketch sketch = survey.getSketch(projection);
        String scrapName = getScrapName(survey, projection);
        sections.add(getScrap(scrapName, projection, sketch, space, innerFrame, scale));
        return TextTools.join("\n\n", sections);
    }


    public static String getXviBlock(
        Survey survey, Space<Coord2D> space, String filename, Shape outerFrame) {

        List<String> lines = new ArrayList<>();

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
        Station origin = survey.getOrigin();
        Coord2D originPos = space.getStationMap().get(survey.getOrigin());
        float xPos = originPos.x;
        float yPos = originPos.y;
        lines.add(getXviLine("xth_me_image_insert",
            "{" + xPos + " 1 1.0}",
            "{" + yPos + " " + origin.getName() + "}",
            "\"" + filename + "\"",
            0,
            "{}"));

        // xth_me_area_zoom_to <zoom>
        // where <zoom> is the default zoom factor, when drawing is open in xtherion.
        // Should be 25,50,100,200,400.
        lines.add(getXviLine("xth_me_area_zoom_to", 25));

        return TextTools.join("\n", lines);
    }

    public static String getScrapName(Survey survey, Projection2D projection) {
        String name = survey.getName().toLowerCase();
        String projectionSuffix = projection.getAbbreviation();
        String joiner = name.contains("_") ? "_" : "-";
        return TextTools.join(joiner, name, projectionSuffix);
    }

    public static String getScrap(String name, Projection2D projection, Sketch sketch,
                                  Space<Coord2D> space, Shape frame, float scale) {
        List<String> lines = new ArrayList<>();
        lines.add(getStartScrapCommands(name, projection, frame));
        lines.addAll(getScrapCommands(sketch, space, scale));
        lines.add("endscrap");
        return TextTools.join("\n\n", lines);

    }

    private static String getStartScrapCommands(String name, Projection2D projection, Shape frame) {

        List<String> commands = new ArrayList<>();

        commands.add("scrap " + name);

        String projectionName = "";
        if (Projection2D.PLAN.equals(projection)) {
            projectionName = "plan";
        } else if (Projection2D.EXTENDED_ELEVATION.equals(projection)) {
            projectionName = "extended";
        }
        commands.add("-projection " + projectionName);
        return TextTools.join(" ", commands);

    }

    private static List<String> getScrapCommands(Sketch sketch, Space<Coord2D> space, float scale) {
        List<String> commands = new ArrayList<>();
        for (Map.Entry<Station, Coord2D> entry : space.getStationMap().entrySet()) {
            Station station = entry.getKey();
            Coord2D coord = entry.getValue().scale(scale);
            commands.add(getPoint(coord.x, coord.y, "station", "-name", station.getName()));
        }

        for (TextDetail textDetail : sketch.getTextDetails()) {
            Coord2D coord = textDetail.getPosition().scale(scale).flipVertically();
            commands.add(
                getPoint(coord.x, coord.y, "label", "-text", textDetail.getText(), "-scale", getScale(textDetail)));
        }

        for (SymbolDetail symbolDetail : sketch.getSymbolDetails()) {
            Coord2D coord = symbolDetail.getPosition().scale(scale).flipVertically();
            Symbol symbol = symbolDetail.getSymbol();

            List<String> args = new ArrayList<>();
            args.add("-scale " + getScale(symbolDetail));
            if (symbol.isDirectional()) {
                args.add("-orientation " + symbolDetail.getAngle());
            }
            commands.add(getPoint(coord.x, coord.y, symbolDetail.getSymbol().getTherionName(), args));
        }

        return commands;
    }

    private static String getPoint(float x, float y, String name, String... args) {
        return getPoint(x, y, name, Arrays.asList(args));
    }

    private static String getPoint(float x, float y, String name, List<String> args) {
        return "point " + x + " " + y + " " + name + " " + TextTools.join(" ", args);
    }

    private static String getScale(AutoScalableDetail detail) {

        float sizeInMetres = detail.getSize();

        // These numbers were determined by creating some stal in Therion
        // at different scales and seeing how big they came out

        // Therion seems to have a small number of sizes available; not sure
        // if there's any way of handling very big symbols

        if (sizeInMetres < 0.45) {
            return "xs"; // appprox 0.4m
        } else if (sizeInMetres < 0.6) {
            return "s"; // approx 0.5m
        } else if (sizeInMetres < 0.9) {
            return "m"; // approx 0.7m
        } else if (sizeInMetres < 1.3) {
            return "l"; // approx 1.1m
        } else {
            return "xl"; // approx 1.5m
        }
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

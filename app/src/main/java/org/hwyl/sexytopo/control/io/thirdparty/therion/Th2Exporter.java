package org.hwyl.sexytopo.control.io.thirdparty.therion;

import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.common.Shape;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.AutoScalableDetail;
import org.hwyl.sexytopo.model.sketch.CrossSectionDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.Symbol;
import org.hwyl.sexytopo.model.sketch.SymbolDetail;
import org.hwyl.sexytopo.model.sketch.TextDetail;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
        return getContent(survey, projection, space, xviFilename, innerFrame, outerFrame, scale, 1, true);
    }

    public static String getContent(
        Survey survey, Projection2D projection, Space<Coord2D> space, String xviFilename,
        Shape innerFrame, Shape outerFrame, float scale,
        int scrapCount, boolean stationsInFirstScrap) {

        List<String> sections = new ArrayList<>();
        sections.add(TherionExporter.getEncodingText());
        sections.add(getXviBlock(survey, space, xviFilename, outerFrame));

        Sketch sketch = survey.getSketch(projection);
        String baseName = getBaseScrapName(survey);
        String scrapSuffix = getScrapSuffix(projection);

        for (int i = 1; i <= scrapCount; i++) {
            String scrapName = formatScrapName(baseName, scrapSuffix, i, scrapCount);
            boolean includeStations = (i == 1) && stationsInFirstScrap;
            boolean includeSketchContent = (i == 1); // sketch content only in first scrap
            sections.add(getScrap(survey, scrapName, projection, sketch, space, innerFrame, scale,
                    includeStations, includeSketchContent));
        }

        // Add cross-section scraps if enabled
        if (GeneralPreferences.isTherionCrossSectionsEnabled()) {
            sections.addAll(getCrossSectionScraps(survey, sketch, projection, baseName, scale));
        }

        return TextTools.join("\n\n", sections);
    }

    private static List<String> getCrossSectionScraps(Survey survey, Sketch sketch,
                                                       Projection2D projection, String baseName, float scale) {
        List<String> scraps = new ArrayList<>();

        String xsSuffix = getXsSuffix(projection);
        List<CrossSectionDetail> crossSections = new ArrayList<>(sketch.getCrossSectionDetails());

        // Sort by station order in survey
        List<Station> orderedStations = survey.getAllStationsInChronoOrder();
        crossSections.sort(Comparator.comparingInt(xs ->
                orderedStations.indexOf(xs.getCrossSection().getStation())));

        int index = 1;
        for (CrossSectionDetail xsDetail : crossSections) {
            Station station = xsDetail.getCrossSection().getStation();
            String scrapName = formatXsScrapName(baseName, xsSuffix, station.getName(), index);
            scraps.add(getCrossSectionScrap(scrapName, xsDetail, scale));
            index++;
        }

        return scraps;
    }

    private static String getXsSuffix(Projection2D projection) {
        if (Projection2D.PLAN.equals(projection)) {
            return GeneralPreferences.getTherionPlanXsSuffix();
        } else {
            return GeneralPreferences.getTherionEeXsSuffix();
        }
    }

    private static String formatXsScrapName(String baseName, String suffixPattern, String stationName, int index) {
        String suffix = suffixPattern;

        // Handle ## for zero-padded index
        if (suffix.contains("##")) {
            String paddedNum = String.format("%02d", index);
            suffix = suffix.replace("##", paddedNum);
        } else if (suffix.contains("#")) {
            // Handle # for simple index
            suffix = suffix.replace("#", String.valueOf(index));
        }

        return baseName + "-" + suffix;
    }

    private static String getCrossSectionScrap(String name, CrossSectionDetail xsDetail, float scale) {
        List<String> lines = new ArrayList<>();

        lines.add("scrap " + name + " -projection none");

        // Add the station point at the cross-section center
        Station station = xsDetail.getCrossSection().getStation();
        Coord2D position = xsDetail.getPosition().scale(scale).flipVertically();
        lines.add(getPoint(position.x, position.y, "station", "-name", station.getName()));

        lines.add("endscrap");

        return TextTools.join("\n\n", lines);
    }

    private static String getScrapSuffix(Projection2D projection) {
        if (Projection2D.PLAN.equals(projection)) {
            return GeneralPreferences.getTherionPlanScrapSuffix();
        } else {
            return GeneralPreferences.getTherionEeScrapSuffix();
        }
    }

    private static String formatScrapName(String baseName, String suffixPattern, int index, int total) {
        String suffix = suffixPattern;

        if (total == 1 && !suffix.contains("#")) {
            // Single scrap, no numbering needed
            return baseName + suffix;
        }

        // Handle ## for zero-padded numbers (01, 02, etc.)
        if (suffix.contains("##")) {
            String paddedNum = String.format("%02d", index);
            suffix = suffix.replace("##", paddedNum);
        } else if (suffix.contains("#")) {
            // Handle # for simple numbers (1, 2, etc.)
            suffix = suffix.replace("#", String.valueOf(index));
        } else if (total > 1) {
            // No placeholder but multiple scraps - append number
            suffix = suffix + index;
        }

        return baseName + suffix;
    }


    public static String getXviBlock(
        Survey survey, Space<Coord2D> space, String filename, Shape outerFrame) {

        List<String> lines = new ArrayList<>();

        lines.add(getXviLine("xth_me_area_adjust",
            TextTools.formatTo2dp(outerFrame.getLeft()),
            TextTools.formatTo2dp(outerFrame.getBottom()),
            TextTools.formatTo2dp(outerFrame.getRight()),
            TextTools.formatTo2dp(outerFrame.getTop())));

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

        lines.add(getXviLine("xth_me_area_zoom_to", 25));

        return TextTools.join("\n", lines);
    }

    public static String getBaseScrapName(Survey survey) {
        String name = survey.getName().toLowerCase();
        name = TextTools.intelligentlySanitise(name);
        return name;
    }

    public static String getScrapName(Survey survey, Projection2D projection) {
        String name = getBaseScrapName(survey);
        String projectionSuffix = projection.getAbbreviation();
        String joiner = "" + TextTools.getJoiner(name);
        return TextTools.join(joiner, name, projectionSuffix);
    }

    public static String getScrap(Survey survey, String name, Projection2D projection, Sketch sketch,
                                  Space<Coord2D> space, Shape frame, float scale,
                                  boolean includeStations, boolean includeSketchContent) {
        List<String> lines = new ArrayList<>();
        lines.add(getStartScrapCommands(name, projection, frame));
        lines.addAll(getScrapCommands(survey, sketch, space, scale, includeStations, includeSketchContent));
        lines.add("endscrap");
        return TextTools.join("\n\n", lines);
    }

    public static String getScrap(Survey survey, String name, Projection2D projection, Sketch sketch,
                                  Space<Coord2D> space, Shape frame, float scale) {
        return getScrap(survey, name, projection, sketch, space, frame, scale, true, true);
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

    private static List<String> getScrapCommands(Survey survey, Sketch sketch, Space<Coord2D> space,
                                                  float scale, boolean includeStations, boolean includeSketchContent) {
        List<String> commands = new ArrayList<>();

        if (includeStations) {
            Map<Station, Coord2D> stationMap = space.getStationMap();
            List<Station> orderedStations = survey.getAllStationsInChronoOrder();
            for (Station station : orderedStations) {
                Coord2D coord = stationMap.get(station).scale(scale);
                commands.add(getPoint(coord.x, coord.y, "station", "-name", station.getName()));
            }
        }

        if (includeSketchContent) {
            if (GeneralPreferences.isXviExportTextEnabled()) {
                for (TextDetail textDetail : sketch.getTextDetails()) {
                    Coord2D coord = textDetail.getPosition().scale(scale).flipVertically();
                    commands.add(
                        getPoint(coord.x, coord.y, "label", "-text \"", textDetail.getText(), "\" -scale",
                            getScale(textDetail)));
                }
            }

            if (GeneralPreferences.isXviExportSymbolsEnabled()) {
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
            }
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

        if (sizeInMetres < 0.45) {
            return "xs";
        } else if (sizeInMetres < 0.6) {
            return "s";
        } else if (sizeInMetres < 0.9) {
            return "m";
        } else if (sizeInMetres < 1.3) {
            return "l";
        } else {
            return "xl";
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

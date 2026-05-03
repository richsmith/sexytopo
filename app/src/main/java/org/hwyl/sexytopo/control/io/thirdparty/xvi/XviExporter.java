package org.hwyl.sexytopo.control.io.thirdparty.xvi;

import static org.hwyl.sexytopo.control.io.thirdparty.xvi.XviConstants.GRIDS_COMMAND;
import static org.hwyl.sexytopo.control.io.thirdparty.xvi.XviConstants.GRID_COMMAND;
import static org.hwyl.sexytopo.control.io.thirdparty.xvi.XviConstants.SHOT_COMMAND;
import static org.hwyl.sexytopo.control.io.thirdparty.xvi.XviConstants.SKETCHLINE_COMMAND;
import static org.hwyl.sexytopo.control.io.thirdparty.xvi.XviConstants.STATIONS_COMMAND;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.common.Shape;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.CrossSectionDetail;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.SymbolDetail;
import org.hwyl.sexytopo.model.sketch.TextDetail;
import org.hwyl.sexytopo.model.survey.Station;

public class XviExporter {

    private static final class XviContent {
        final List<String> stations = new ArrayList<>();
        final List<String> shots = new ArrayList<>();
        final List<String> sketchLines = new ArrayList<>();
    }

    public static String getContent(
            Sketch sketch, Space<Coord2D> space, float scale, Shape gridFrame) {
        XviContent content = new XviContent();

        // First the main survey
        addSketch(content, space, sketch, scale);

        // Then the same for each sketch, once translated and scaled
        float xsScale = sketch.getCrossSectionScale();
        for (CrossSectionDetail xsDetail : sketch.getCrossSectionDetails()) {
            Space<Coord2D> xsSpace = getScaledProjection(xsDetail, xsScale);
            Sketch xsSketch = xsDetail.getSketch().scale(xsScale).translate(xsDetail.getPosition());
            addSketch(content, xsSpace, xsSketch, scale);
            addCrossSectionConnection(content, xsDetail, space, scale);
        }

        String text = field(GRIDS_COMMAND, "1 m");
        text += multilineField(STATIONS_COMMAND, joinAll(content.stations));
        text += multilineField(SHOT_COMMAND, joinAll(content.shots));
        text += multilineField(SKETCHLINE_COMMAND, joinAll(content.sketchLines));

        text += field(GRID_COMMAND, getGridText(gridFrame, scale));
        return text;
    }

    /**
     * Inputs are in survey-frame metres (y north-positive). The XVI emit-helpers (getStationText
     * / getLegText / getPathDetailText) handle the y-flip on the way out — keep that the only
     * place flipping happens.
     */
    private static void addSketch(
            XviContent content, Space<Coord2D> space, Sketch sketch, double scale) {
        for (Map.Entry<Station, Coord2D> entry : space.getStationMap().entrySet()) {
            content.stations.add(getStationText(entry.getKey(), entry.getValue(), scale));
        }
        for (Line<Coord2D> line : space.getLegMap().values()) {
            content.shots.add(getLegText(line, scale));
        }
        for (PathDetail pathDetail : sketch.getPathDetails()) {
            content.sketchLines.add(getPathDetailText(pathDetail, scale));
        }
        for (TextDetail textDetail : sketch.getTextDetails()) {
            content.sketchLines.add(getTextDetailAsPathsText(textDetail, scale));
        }
        for (SymbolDetail symbolDetail : sketch.getSymbolDetails()) {
            content.sketchLines.add(getSymbolDetailAsPathsText(symbolDetail, scale));
        }
    }

    private static void addCrossSectionConnection(
            XviContent content, CrossSectionDetail xsDetail, Space<Coord2D> space, double scale) {
        content.sketchLines.add(getCrossSectionConnectorText(xsDetail, space, scale));
    }

    private static String joinAll(List<String> parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            builder.append(part);
        }
        return builder.toString();
    }

    private static Space<Coord2D> getScaledProjection(CrossSectionDetail xsDetail, float xsScale) {
        Space<Coord2D> rawProjection = xsDetail.getCrossSection().getProjection();
        Space<Coord2D> scaledProjection = rawProjection.scale(xsScale);
        return Space2DUtils.translate(scaledProjection, xsDetail.getPosition());
    }

    private static String getStationText(Station station, Coord2D coords, double scale) {
        String x = TextTools.formatTo2dpWithDot(coords.x * scale);
        String y = TextTools.formatTo2dpWithDot(-coords.y * scale);
        return field("\t", TextTools.joinAll(" ", x, y, station.getName()));
    }

    private static String getLegText(Line<Coord2D> line, double scale) {
        Coord2D start = line.getStart();
        String startX = TextTools.formatTo2dpWithDot(start.x * scale);
        String startY = TextTools.formatTo2dpWithDot(-start.y * scale);
        Coord2D end = line.getEnd();
        String endX = TextTools.formatTo2dpWithDot(end.x * scale);
        String endY = TextTools.formatTo2dpWithDot(-end.y * scale);
        return field("\t", TextTools.joinAll(" ", startX, startY, endX, endY));
    }

    private static String getCrossSectionConnectorText(
            CrossSectionDetail xsDetail, Space<Coord2D> space, double scale) {
        Station station = xsDetail.getCrossSection().getStation();
        Coord2D surveyStationPos = space.getStationMap().get(station);
        if (surveyStationPos == null) {
            return "";
        }
        Coord2D xsPos = xsDetail.getPosition();

        String x1 = TextTools.formatTo2dpWithDot(surveyStationPos.x * scale);
        String y1 = TextTools.formatTo2dpWithDot(-surveyStationPos.y * scale);
        String x2 = TextTools.formatTo2dpWithDot(xsPos.x * scale);
        String y2 = TextTools.formatTo2dpWithDot(-xsPos.y * scale);

        return field("\t", TextTools.joinAll(" ", "connect", x1, y1, x2, y2));
    }

    private static String getPathDetailText(PathDetail pathDetail, double scale) {
        List<Object> fields = new ArrayList<>();
        fields.add(pathDetail.getColour().toString());
        for (Coord2D coord2D : pathDetail.getPath()) {
            String x = TextTools.formatTo2dpWithDot(coord2D.x * scale);
            String y =
                    TextTools.formatTo2dpWithDot((-coord2D.y + 0.0) * scale); // +0.0 to avoid -0.0
            fields.add(x);
            fields.add(y);
        }
        return field("\t", TextTools.join(" ", fields));
    }

    private static String getTextDetailAsPathsText(TextDetail textDetail, double scale) {
        StringBuilder builder = new StringBuilder();
        List<PathDetail> pathDetails = TextDetailTranslater.getPathDetailsForText(textDetail);
        for (PathDetail pathDetail : pathDetails) {
            builder.append(getPathDetailText(pathDetail, scale));
        }
        return builder.toString();
    }

    private static String getSymbolDetailAsPathsText(SymbolDetail symbolDetail, double scale) {
        StringBuilder builder = new StringBuilder();
        List<PathDetail> pathDetails = new SymbolDetailTranslater().asPathDetails(symbolDetail);
        for (PathDetail pathDetail : pathDetails) {
            builder.append(getPathDetailText(pathDetail, scale));
        }
        return builder.toString();
    }

    private static String getGridText(Shape gridFrame, float scale) {

        float numberX = Math.round(gridFrame.getWidth() / scale);
        float numberY = Math.round(gridFrame.getHeight() / scale);

        // Grid is{bottom left x, bottom left y,
        // x1 dist, y1 dist, x2 dist, y2 dist, number of x, number of y}
        Float[] values =
                new Float[] {
                    gridFrame.getLeft(), // bottom left x
                    gridFrame.getBottom(), // bottom left y
                    scale, // x1 dist
                    0.0f, // y1 dist
                    0.0f, // x2 dist
                    scale, // y2 dist
                    numberX, // x squares
                    numberY // y squares
                };

        return TextTools.join(" ", Arrays.asList(values));
    }

    private static String field(String text, String content) {
        return text + " {" + content + "}\n";
    }

    private static String multilineField(String text, String content) {
        return text + " {\n" + content + "}\n";
    }
}

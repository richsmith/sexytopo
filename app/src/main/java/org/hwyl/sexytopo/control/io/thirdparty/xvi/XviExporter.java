package org.hwyl.sexytopo.control.io.thirdparty.xvi;

import static org.hwyl.sexytopo.control.io.thirdparty.xvi.XviConstants.GRIDS_COMMAND;
import static org.hwyl.sexytopo.control.io.thirdparty.xvi.XviConstants.GRID_COMMAND;
import static org.hwyl.sexytopo.control.io.thirdparty.xvi.XviConstants.SHOT_COMMAND;
import static org.hwyl.sexytopo.control.io.thirdparty.xvi.XviConstants.SKETCHLINE_COMMAND;
import static org.hwyl.sexytopo.control.io.thirdparty.xvi.XviConstants.STATIONS_COMMAND;

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
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class XviExporter {

    public static String getContent(Sketch sketch, Space<Coord2D> space, float scale,
                                    Shape gridFrame) {
        String text = field(GRIDS_COMMAND, "1 m");

        // Combine main survey stations with cross-section stations
        String stationsText = getStationsText(space, scale) + getCrossSectionStationsText(sketch, scale);
        text += multilineField(STATIONS_COMMAND, stationsText);

        // Combine main survey legs with cross-section splays
        String legsText = getLegsText(space, scale) + getCrossSectionLegsText(sketch, scale);
        text += multilineField(SHOT_COMMAND, legsText);

        text += multilineField(SKETCHLINE_COMMAND, getSketchLinesText(sketch, space, scale));
        text += field(GRID_COMMAND, getGridText(gridFrame, scale));
        return text;
    }

    private static String getCrossSectionStationsText(Sketch sketch, float scale) {
        StringBuilder builder = new StringBuilder();
        for (CrossSectionDetail xsDetail : sketch.getCrossSectionDetails()) {
            Space<Coord2D> xsSpace = xsDetail.getProjection();
            for (Map.Entry<Station, Coord2D> entry : xsSpace.getStationMap().entrySet()) {
                Coord2D flipped = entry.getValue().flipVertically();
                builder.append(getStationText(entry.getKey(), flipped, scale));
            }
        }
        return builder.toString();
    }

    private static String getCrossSectionLegsText(Sketch sketch, float scale) {
        StringBuilder builder = new StringBuilder();
        for (CrossSectionDetail xsDetail : sketch.getCrossSectionDetails()) {
            Space<Coord2D> xsSpace = xsDetail.getProjection();
            for (Map.Entry<Leg, Line<Coord2D>> entry : xsSpace.getLegMap().entrySet()) {
                Line<Coord2D> line = entry.getValue();
                Line<Coord2D> flipped = new Line<>(
                    line.getStart().flipVertically(),
                    line.getEnd().flipVertically());
                builder.append(getLegText(flipped, scale));
            }
        }
        return builder.toString();
    }

    private static String getStationsText(Space<Coord2D> space, double scale) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Station, Coord2D> entry: space.getStationMap().entrySet()) {
            builder.append(getStationText(entry.getKey(), entry.getValue(), scale));
        }
        return builder.toString();
    }

    private static String getStationText(Station station, Coord2D coords, double scale) {
        String x = TextTools.formatTo2dpWithDot(coords.x * scale);
        String y = TextTools.formatTo2dpWithDot(coords.y * scale);
        return field("\t", TextTools.joinAll(" ", x, y, station.getName()));
    }

    private static String getLegsText(Space<Coord2D> space, double scale) {
        StringBuilder builder = new StringBuilder();
        for (Line<Coord2D> line: space.getLegMap().values()) {
            builder.append(getLegText(line, scale));
        }
        return builder.toString();
    }

    private static String getLegText(Line<Coord2D> line, double scale) {
        Coord2D start = line.getStart();
        String startX = TextTools.formatTo2dpWithDot(start.x * scale);
        String startY = TextTools.formatTo2dpWithDot(start.y * scale);
        Coord2D end = line.getEnd();
        String endX = TextTools.formatTo2dpWithDot(end.x * scale);
        String endY = TextTools.formatTo2dpWithDot(end.y * scale);
        return field("\t", TextTools.joinAll(" ", startX, startY, endX, endY));
    }

    private static String getSketchLinesText(Sketch sketch, Space<Coord2D> space, double scale) {
        StringBuilder builder = new StringBuilder();
        for (PathDetail pathDetail : sketch.getPathDetails()) {
            builder.append(getPathDetailText(pathDetail, scale));
        }

        for (TextDetail textDetail : sketch.getTextDetails()) {
            builder.append(getTextDetailAsPathsText(textDetail, scale));
        }

        for (SymbolDetail symbolDetail : sketch.getSymbolDetails()) {
            builder.append(getSymbolDetailAsPathsText(symbolDetail, scale));
        }

        // Add connector lines from cross-section stations to their survey stations
        for (CrossSectionDetail xsDetail : sketch.getCrossSectionDetails()) {
            builder.append(getCrossSectionConnectorText(xsDetail, space, scale));
        }

        return builder.toString();
    }

    private static String getCrossSectionConnectorText(CrossSectionDetail xsDetail,
                                                        Space<Coord2D> space, double scale) {
        Station station = xsDetail.getCrossSection().getStation();
        Coord2D surveyStationPos = space.getStationMap().get(station);
        if (surveyStationPos == null) {
            return "";
        }

        // Cross-section position (flip Y for XVI coordinate system)
        Coord2D xsPos = xsDetail.getPosition().flipVertically();

        String x1 = TextTools.formatTo2dpWithDot(surveyStationPos.x * scale);
        String y1 = TextTools.formatTo2dpWithDot(surveyStationPos.y * scale);
        String x2 = TextTools.formatTo2dpWithDot(xsPos.x * scale);
        String y2 = TextTools.formatTo2dpWithDot(xsPos.y * scale);

        return field("\t", TextTools.joinAll(" ", "connect", x1, y1, x2, y2));
    }

    private static String getPathDetailText(PathDetail pathDetail, double scale) {
        List<Object> fields = new ArrayList<>();
        fields.add(pathDetail.getColour().toString());
        for (Coord2D coord2D : pathDetail.getPath()) {
            String x = TextTools.formatTo2dpWithDot(coord2D.x * scale);
            String y = TextTools.formatTo2dpWithDot((-coord2D.y + 0.0) * scale); // +0.0 to avoid -0.0
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
        Float[] values = new Float[] {
            gridFrame.getLeft(), // bottom left x
            gridFrame.getBottom(), // bottom left y
            scale, // x1 dist
            0.0f,  // y1 dist
            0.0f, // x2 dist
            scale, // y2 dist
            numberX,  // x squares
            numberY  // y squares
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

package org.hwyl.sexytopo.control.io.thirdparty.xvi;

import org.hwyl.sexytopo.control.io.thirdparty.therion.SketchDimensions;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Station;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.hwyl.sexytopo.control.io.thirdparty.xvi.XviConstants.GRIDS_COMMAND;
import static org.hwyl.sexytopo.control.io.thirdparty.xvi.XviConstants.GRID_COMMAND;
import static org.hwyl.sexytopo.control.io.thirdparty.xvi.XviConstants.SHOT_COMMAND;
import static org.hwyl.sexytopo.control.io.thirdparty.xvi.XviConstants.SKETCHLINE_COMMAND;
import static org.hwyl.sexytopo.control.io.thirdparty.xvi.XviConstants.STATIONS_COMMAND;


public class XviExporter {

    public static String getContent(Sketch sketch, Space<Coord2D> space, double scale) {

        String text = field(GRIDS_COMMAND, "1 m");
        text += multilineField(STATIONS_COMMAND, getStationsText(space, scale));
        text += multilineField(SHOT_COMMAND, getLegsText(space, scale));
        text += multilineField(SKETCHLINE_COMMAND, getSketchLinesText(sketch, scale));
        text += field(GRID_COMMAND, getGridText(space, scale));
        return text;
    }

    private static String getStationsText(Space<Coord2D> space, double scale) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Station, Coord2D> entry: space.getStationMap().entrySet()) {
            builder.append(getStationText(entry.getKey(), entry.getValue(), scale));
        }
        return builder.toString();
    }

    private static String getStationText(Station station, Coord2D coords, double scale) {
        String x = TextTools.formatTo2dp(coords.x * scale);
        String y = TextTools.formatTo2dp(coords.y * scale);
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
        String startX = TextTools.formatTo2dp(start.x * scale);
        String startY = TextTools.formatTo2dp(start.y * scale);
        Coord2D end = line.getEnd();
        String endX = TextTools.formatTo2dp(end.x * scale);
        String endY = TextTools.formatTo2dp(end.y * scale);
        return field("\t", TextTools.joinAll(" ", startX, startY, endX, endY));
    }

    private static String getSketchLinesText(Sketch sketch, double scale) {
        StringBuilder builder = new StringBuilder();
        for (PathDetail pathDetail : sketch.getPathDetails()) {
            builder.append(getSketchLineText(pathDetail, scale));
        }
        return builder.toString();
    }

    private static String getSketchLineText(PathDetail pathDetail, double scale) {
        List<Object> fields = new LinkedList<>();
        fields.add(pathDetail.getColour().toString());
        for (Coord2D coord2D : pathDetail.getPath()) {
            String x = TextTools.formatTo2dp(coord2D.x * scale);
            String y = TextTools.formatTo2dp((-coord2D.y + 0.0) * scale); // +0.0 to avoid -0.0
            fields.add(x);
            fields.add(y);
        }
        return field("\t", TextTools.join(" ", fields));
    }

    private static String getGridText(Space<Coord2D> space, double scale) {
        SketchDimensions dimensions = SketchDimensions.getDimensions(space);

        Double[] values = new Double[] {
            (dimensions.minX - 1) * scale,
            (dimensions.minY - 1) * scale,
            scale,
            0.0,
            0.0,
            scale,
            dimensions.getWidth() + 2,
            dimensions.getHeight() + 2
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

package org.hwyl.sexytopo.control.io.thirdparty.xvi;

import org.hwyl.sexytopo.control.io.thirdparty.therion.SketchDimensions;
import org.hwyl.sexytopo.control.io.thirdparty.therion.TherionExporter;
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

    public static String getContent(Sketch sketch, Space<Coord2D> space ) {

        String text = field(GRIDS_COMMAND, "1 m");
        text += multilineField(STATIONS_COMMAND, getStationsText(space));
        text += multilineField(SHOT_COMMAND, getLegsText(space));
        text += multilineField(SKETCHLINE_COMMAND, getSketchLinesText(sketch));
        text += field(GRID_COMMAND, getGridText(sketch, space));
        return text;
    }

    private static String getStationsText(Space<Coord2D> space) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Station, Coord2D> entry: space.getStationMap().entrySet()) {
            builder.append(getStationText(entry.getKey(), entry.getValue()));
        }
        return builder.toString();
    }

    private static String getStationText(Station station, Coord2D coords) {
        return field("\t", TextTools.joinAll(" ", coords.getX(), coords.getY(), station.getName()));
    }

    private static String getLegsText(Space<Coord2D> space) {
        StringBuilder builder = new StringBuilder();
        for (Line<Coord2D> line: space.getLegMap().values()) {
            builder.append(getLegText(line));
        }
        return builder.toString();
    }

    private static String getLegText(Line<Coord2D> line) {
        Coord2D start = line.getStart();
        Coord2D end = line.getEnd();
        return field("\t", TextTools.joinAll(" ",
                start.getX(), start.getY(), end.getX(), end.getY()));
    }

    private static String getSketchLinesText(Sketch sketch) {
        StringBuilder builder = new StringBuilder();
        for (PathDetail pathDetail : sketch.getPathDetails()) {
            builder.append(getSketchLineText(pathDetail));
        }
        return builder.toString();
    }

    private static String getSketchLineText(PathDetail pathDetail) {
        List<Object> fields = new LinkedList<>();
        fields.add(pathDetail.getColour().toString());
        for (Coord2D coord2D : pathDetail.getPath()) {
            fields.add(coord2D.getX());
            fields.add(- coord2D.getY());
        }
        return field("\t", TextTools.join(" ", fields));
    }

    private static String getGridText(Sketch sketch, Space<Coord2D> space) {
        // cfactor = 100 * planDPI / (2.54 * planscale)
        double scale = TherionExporter.getScale();

        SketchDimensions extremes = SketchDimensions.getDimensions(space);
        double width = Math.round(extremes.maxX - extremes.minX);
        double height = Math.round(extremes.maxY - extremes.minY);

        Double[] values = new Double[] {
            (extremes.minX - 1) * scale,
            (extremes.minY - 1) * scale,
            scale,
            0.0,
            0.0,
            scale,
            width + 2,
            height + 2
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

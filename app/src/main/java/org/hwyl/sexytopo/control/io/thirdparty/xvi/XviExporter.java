package org.hwyl.sexytopo.control.io.thirdparty.xvi;

import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.text.DecimalFormat;
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

    public static String getContent(Survey survey, Sketch sketch, Projection2D projection) {

        Space<Coord2D> space = projection.project(survey);

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
            fields.add(coord2D.getY());
        }
        return field("\t", TextTools.join(" ", fields));
    }

    private static String getGridText(Sketch sketch, Space<Coord2D> space) {
        // cfactor = 100 * planDPI / (2.54 * planscale)
        double scale = getScale();

        Extremes extremes = getExtremes(space);
        DecimalFormat df = new DecimalFormat("#.00000");
        df.format(0.912385);
        double width = Math.ceil(extremes.maxX - extremes.minX);
        double height = Math.ceil(extremes.maxY - extremes.minY);

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


    private static double getScale() {
        final double PLAN_DPI = 200;
        final double PLAN_SCALE = 100;
        final double CENTIMETRES_PER_METRE = 2.54;
        final double scale = 100 * PLAN_DPI / (CENTIMETRES_PER_METRE * PLAN_SCALE);
        return scale;
    }

    private static Extremes getExtremes(Space<Coord2D> space) {
        Double minX = null, maxX = null, minY = null, maxY = null;
        for (Line<Coord2D> line : space.getLegMap().values()) {
            for (Coord2D point : new Coord2D[]{line.getStart(), line.getEnd()}) {
                if (minX == null || point.getX() < minX) {
                    minX = point.getX();
                }
                if (maxX == null || point.getX() > maxX) {
                    maxX = point.getX();
                }
                if (minY == null || point.getY() < minY) {
                    minY = point.getY();
                }
                if (maxY == null || point.getY() > maxY) {
                    maxY = point.getY();
                }
            }
        }
        return new Extremes(minX, maxX, minY, maxY);

    }

    private static String field(String text, String content) {
        return text + " {" + content + "}\n";
    }

    private static String multilineField(String text, String content) {
        return text + " {\n" + content + "}\n";
    }


}

class Extremes {
    final double minX, maxX, minY, maxY;
    Extremes(double minX, double maxX, double minY, double maxY) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
    }
}
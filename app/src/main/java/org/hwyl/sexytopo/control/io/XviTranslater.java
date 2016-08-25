package org.hwyl.sexytopo.control.io;

import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by rls on 09/11/15.
 */
public class XviTranslater {

    public static final String GRIDS_COMMAND = "set XVIgrids";
    public static final String STATIONS_COMMAND = "set XVIstations";
    public static final String SHOT_COMMAND = "set XVIshots";
    public static final String SKETCHLINE_COMMAND = "set XVIsketchlines";
    public static final String GRID_COMMAND = "set XVIgrid";

    private static String getXvi(Survey survey, Sketch sketch, Projection2D projection) {

        Space<Coord2D> space = projection.project(survey);

        String text = field(GRIDS_COMMAND, "1 m");
        text += multilineField(STATIONS_COMMAND, getStationsText(space));
        text += multilineField(SHOT_COMMAND, getLegsText(space));
        text += multilineField(SKETCHLINE_COMMAND, getSketchLinesText(sketch));
        text += field(GRID_COMMAND, "");
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
        return field("\t", TextTools.join(" ", coords.getX(), coords.getY(), station.getName()));
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
        return field("\t", TextTools.join(" ", start.getX(), start.getY(), end.getX(), end.getY()));
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
        fields.addAll(pathDetail.getPath());
        return field("\t", TextTools.join(" ", fields));
    }

    private static String field(String text, String content) {
        return text + " {" + content + "}\n";
    }

    private static String multilineField(String text, String content) {
        return text + " {\n" + content + "}\n";
    }
}

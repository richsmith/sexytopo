package org.hwyl.sexytopo.control.io.thirdparty.pockettopo;

import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.thirdparty.survex.SurvexExporter;
import org.hwyl.sexytopo.control.io.translation.Experimental;
import org.hwyl.sexytopo.control.io.translation.SingleFileExporter;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class PocketTopoTxtExporter extends SingleFileExporter implements Experimental {


    public String getContent(Survey survey) {

        String text = "TRIP\n";

        text += "DATE ";
        if (survey.getTrip() != null) {
            Trip trip = survey.getTrip();
            Date date = trip.getDate();
            text += TextTools.toIsoDate(date);
        } else {
            text += "1970-01-01\n";
        }
        text += "\n";

        text += "DECLINATION\t0.00\n";

        text += exportData(survey) + "\n";

        text += exportPlan(survey) + "\n";

        text += exportExtendedElevation(survey);


        return text;
    }


    public static String exportData(Survey survey) {
        String data = "DATA\n";
        data += new SurvexExporter().getContent(survey);
        return data;
    }


    public static String exportPlan(Survey survey) {
        String plan = "PLAN\n";
        plan += exportStationCoords(Projection2D.PLAN.project(survey)) + "\n";
        plan += exportSketch(survey.getPlanSketch()) + "\n";
        return plan;
    }


    public static String exportExtendedElevation(Survey survey) {
        String plan = "ELEVATION\n";
        plan += exportStationCoords(Projection2D.EXTENDED_ELEVATION.project(survey)) + "\n";
        plan += exportSketch(survey.getElevationSketch()) + "\n";
        return plan;
    }


    public static String exportSketch(Sketch sketch) {
        List<String> lines = new ArrayList<>();

        for (PathDetail pathDetail : sketch.getPathDetails()) {
            lines.add("POLYLINE " + pathDetail.getColour().toString());
            for (Coord2D coords : pathDetail.getPath()) {
                lines.add(coords.x + "\t" + -coords.y);
            }

        }

        return TextTools.join("\n", lines);
    }


    public static String exportStationCoords(Space<Coord2D> space) {
        List<String> lines = new ArrayList<>();
        lines.add("STATIONS");
        for (Map.Entry<Station, Coord2D> entry : space.getStationMap().entrySet()) {
            Coord2D coords = entry.getValue();
            Station station = entry.getKey();
            lines.add(coords.x + "\t" + coords.y + "\t" + station.getName());
        }

        lines.add("SHOTS");

        for (Line<Coord2D> line : space.getLegMap().values()) {
            Coord2D start = line.getStart();
            Coord2D end = line.getEnd();
            lines.add(start.x + "\t" + start.y + "\t" + end.x + "\t" + end.y);
        }

        return TextTools.join("\n", lines);
    }


    @Override
    public String getFileExtension() {
        return "txt";
    }


    @Override
    public String getExportTypeName(Context context) {
        return context.getString(R.string.third_party_pocket_topo_txt);
    }


    @Override
    public String getExportDirectoryName() {
        return "pockettopo-txt";
    }

}

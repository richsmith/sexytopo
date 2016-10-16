package org.hwyl.sexytopo.control.io.thirdparty.therion;

import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.thirdparty.xvi.XviExporter;
import org.hwyl.sexytopo.control.io.translation.Exporter;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.IOException;


public class TherionExporter extends Exporter {


    public void export(Survey survey) throws IOException {

        String thconfigContent = ThconfigExporter.getContent(survey);
        saveToExportDirectory(survey, "thconfig", thconfigContent);

        String name = survey.getName();

        String thContent = ThExporter.getContent(survey);
        saveToExportDirectory(survey, name + ".th", thContent);

        handleProjection(survey, Projection2D.PLAN, "plan");
        handleProjection(survey, Projection2D.EXTENDED_ELEVATION, "ee");
    }

    private void handleProjection(Survey survey, Projection2D projection, String suffix)
            throws IOException {

        double scale = getScale();

        Space<Coord2D> space = projection.project(survey);
        String filename = survey.getName() + " " + suffix;
        String th2PlanContent = Th2Exporter.getContent(survey, scale, filename, space);
        saveToExportDirectory(survey, filename + ".th2", th2PlanContent);

        String xviPlanContent = XviExporter.getContent(survey.getPlanSketch(), space, scale);
        saveToExportDirectory(survey, filename + ".xvi", xviPlanContent);

    }


    @Override
    public String getExportTypeName(Context context) {
        return context.getString(R.string.third_party_therion);
    }


    @Override
    public String getExportDirectoryName() {
        return "Therion";
    }


    public static String getEncodingText() {
        return "encoding utf-8";
    }

    public static double getScale() {
        final double PLAN_DPI = 200;
        final double PLAN_SCALE = 100;
        final double CENTIMETRES_PER_METRE = 2.54;
        final double scale = 100 * PLAN_DPI / (CENTIMETRES_PER_METRE * PLAN_SCALE);
        return scale;
    }

}

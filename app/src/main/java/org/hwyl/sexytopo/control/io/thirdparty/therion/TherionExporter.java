package org.hwyl.sexytopo.control.io.thirdparty.therion;

import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.thirdparty.xvi.XviExporter;
import org.hwyl.sexytopo.control.io.translation.Exporter;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.IOException;


public class TherionExporter extends Exporter {


    public void export(Survey survey) throws IOException {

        String thconfigContent = ThconfigExporter.getContent(survey);
        saveToExportDirectory(survey, "thconfig", thconfigContent);

        String name = survey.getName();

        String thContent = ThExporter.getContent(survey);
        saveToExportDirectory(survey, name + ".th", thContent);

        String th2Content = Th2Exporter.getContent(survey);
        saveToExportDirectory(survey, name + ".th2", th2Content);

        String xviPlanContent = XviExporter.getContent(
                survey, survey.getPlanSketch(), Projection2D.PLAN);
        saveToExportDirectory(survey, name + " plan.xvi", xviPlanContent);
        String xviExtendedElevationContent = XviExporter.getContent(
                survey, survey.getElevationSketch(), Projection2D.EXTENDED_ELEVATION);
        saveToExportDirectory(survey, name + " ee.xvi", xviExtendedElevationContent);
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

}

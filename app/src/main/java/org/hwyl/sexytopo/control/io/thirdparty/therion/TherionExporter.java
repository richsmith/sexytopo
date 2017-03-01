package org.hwyl.sexytopo.control.io.thirdparty.therion;

import android.content.Context;

import org.apache.commons.io.FilenameUtils;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.Util;
import org.hwyl.sexytopo.control.io.basic.Loader;
import org.hwyl.sexytopo.control.io.thirdparty.xvi.XviExporter;
import org.hwyl.sexytopo.control.io.translation.Exporter;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class TherionExporter extends Exporter {

    public static final String PLAN_SUFFIX = "plan";
    public static final String EE_SUFFIX = "ee";

    private String originalThFileContent = null;
    private String originalTh2PlanFileContent = null;
    private String originalTh2EeFileContent = null;

    private List<String> th2Files = new ArrayList<>();

    public void export(Context context, Survey survey) throws IOException {

        th2Files.clear();
        readOriginalFilesIfPresent(context, survey);

        String thconfigContent = ThconfigExporter.getContent(survey);
        saveToExportDirectory(context, survey, "thconfig", thconfigContent);

        String name = survey.getName();

        handleProjection(context, survey, Projection2D.PLAN, PLAN_SUFFIX,
                originalTh2PlanFileContent);
        handleProjection(context, survey, Projection2D.EXTENDED_ELEVATION, EE_SUFFIX,
                originalTh2EeFileContent);

        String thContent = null;
        if (originalThFileContent == null) {
            thContent = ThExporter.getContent(survey, th2Files);
        } else {
            thContent = ThExporter.updateOriginalContent(survey, originalThFileContent, th2Files);
        }
        saveToExportDirectory(context, survey, name + ".th", thContent);

    }


    private void handleProjection(
            Context context,
            Survey survey,
            Projection2D projection,
            String suffix,
            String originalFileContent)
            throws IOException {

        double scale = getScale();

        Space<Coord2D> space = projection.project(survey);
        String baseFilename = survey.getName() + " " + suffix;
        String th2Filename = baseFilename + ".th2";
        String content = null;
        if (originalFileContent == null) {
            content = Th2Exporter.getContent(
                    survey, scale, baseFilename, space);
        } else {
            content = Th2Exporter.updateOriginalContent(
                    survey, scale, baseFilename, space, originalFileContent);
        }
        saveToExportDirectory(context, survey, th2Filename, content);

        String xviPlanContent = XviExporter.getContent(survey.getPlanSketch(), space, scale);
        saveToExportDirectory(context, survey, baseFilename + ".xvi", xviPlanContent);

        th2Files.add(th2Filename);
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


    private void readOriginalFilesIfPresent(Context context, Survey survey) {
        if (Util.wasSurveyImported(context, survey)) {
            File directory = Util.getFileSurveyWasImportedFrom(context, survey);
            if (new TherionImporter().canHandleFile(directory)) {
                File[] files = directory.listFiles();
                for (File file : files) {
                    if (FilenameUtils.getExtension(file.getName()).equals("th")) {
                        originalThFileContent = Loader.slurpFile(file);
                    } else if (file.getName().endsWith(PLAN_SUFFIX + ".th2")) {
                        originalTh2PlanFileContent = Loader.slurpFile(file);
                    } else if (file.getName().endsWith(PLAN_SUFFIX + ".th2")) {
                        originalTh2EeFileContent = Loader.slurpFile(file);
                    }

                }
            }
        }
    }

}

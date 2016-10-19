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

    public void export(Survey survey) throws IOException {

        th2Files.clear();
        readOriginalFilesIfPresent(survey);

        String thconfigContent = ThconfigExporter.getContent(survey);
        saveToExportDirectory(survey, "thconfig", thconfigContent);

        String name = survey.getName();

        handleProjection(
                survey, Projection2D.PLAN, PLAN_SUFFIX, originalTh2PlanFileContent);
        handleProjection(
                survey, Projection2D.EXTENDED_ELEVATION, EE_SUFFIX, originalTh2EeFileContent);

        String thContent = null;
        if (originalThFileContent == null) {
            thContent = ThExporter.getContent(survey, th2Files);
        } else {
            thContent = ThExporter.updateOriginalContent(survey, originalThFileContent, th2Files);
        }
        saveToExportDirectory(survey, name + ".th", thContent);

    }


    private void handleProjection(
            Survey survey, Projection2D projection, String suffix, String originalFileContent)
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
        saveToExportDirectory(survey, th2Filename, content);

        String xviPlanContent = XviExporter.getContent(survey.getPlanSketch(), space, scale);
        saveToExportDirectory(survey, baseFilename + ".xvi", xviPlanContent);

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


    private void readOriginalFilesIfPresent(Survey survey) {
        if (Util.wasSurveyImported(survey)) {
            File directory = Util.getFileSurveyWasImportedFrom(survey);
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

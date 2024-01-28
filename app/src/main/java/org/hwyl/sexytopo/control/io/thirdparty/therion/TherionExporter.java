package org.hwyl.sexytopo.control.io.thirdparty.therion;

import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.SurveyFile;
import org.hwyl.sexytopo.control.io.basic.ExportFrameFactory;
import org.hwyl.sexytopo.control.io.thirdparty.xvi.XviExporter;
import org.hwyl.sexytopo.control.io.translation.Exporter;
import org.hwyl.sexytopo.control.util.SpaceFlipper;
import org.hwyl.sexytopo.model.common.Frame;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class TherionExporter extends Exporter {

    private static SurveyFile.SurveyFileType THCONFIG =
            new SurveyFile.SurveyFileType("thconfig", "plain/text");
    private static SurveyFile.SurveyFileType TH =
            new SurveyFile.SurveyFileType("th", "plain/text");
    private static SurveyFile.SurveyFileType TH2_PLAN =
            new SurveyFile.SurveyFileType("plan.th2", "plain/text");
    private static SurveyFile.SurveyFileType XVI_PLAN =
            new SurveyFile.SurveyFileType("plan.xvi", "plain/text");
    private static SurveyFile.SurveyFileType TH2_EE =
            new SurveyFile.SurveyFileType("ee.th2", "plain/text");
    private static SurveyFile.SurveyFileType XVI_EE =
            new SurveyFile.SurveyFileType("ee.xvi", "plain/text");

    private String originalThFileContent = null;
    private String originalTh2PlanFileContent = null;
    private String originalTh2EeFileContent = null;

    private final List<String> th2Files = new ArrayList<>();

    public void run(Context context, Survey survey) throws IOException {

        // Therion export notes:
        // Therion coordinate system is in traditional graph-style, with positive y going up.

        th2Files.clear();
        readOriginalFilesIfPresent(context, survey);

        String thconfigContent = ThconfigExporter.getContent(survey);
        SurveyFile thconfig = getOutputFile(THCONFIG);
        thconfig.save(context, thconfigContent);

        SurveyFile th2_plan_file = getOutputFile(TH2_PLAN);
        SurveyFile xvi_plan_file = getOutputFile(XVI_PLAN);
        handleProjection(context, survey, Projection2D.PLAN,
                th2_plan_file, xvi_plan_file, originalTh2PlanFileContent);

        SurveyFile th2_ee_file = getOutputFile(TH2_EE);
        SurveyFile xvi_ee_file = getOutputFile(XVI_EE);
        handleProjection(context, survey, Projection2D.EXTENDED_ELEVATION,
                th2_ee_file, xvi_ee_file, originalTh2EeFileContent);

        String thContent;
        if (originalThFileContent == null) {
            thContent = ThExporter.getContent(context, survey, th2Files);
        } else {
            thContent = ThExporter.updateOriginalContent(survey, originalThFileContent, th2Files);
        }
        SurveyFile th = getOutputFile(TH);
        th.save(context, thContent);
    }


    private void handleProjection(
            Context context,
            Survey survey,
            Projection2D projectionType,
            SurveyFile th2File,
            SurveyFile xviFile,
            String originalFileContent)
            throws IOException {

        float scale = getScale();

        Space<Coord2D> space = projectionType.project(survey);
        space = space.scale(scale);
        space = SpaceFlipper.flipVertically(space);

        Sketch sketch = survey.getSketch(projectionType);

        Frame exportFrame = ExportFrameFactory.getExportFrame(survey, projectionType);
        exportFrame = exportFrame.scale(scale);

        String content;
        if (originalFileContent == null) {
            content = Th2Exporter.getContent(
                    survey, scale, xviFile.getFilename(), exportFrame);
        } else {
            content = Th2Exporter.updateOriginalContent(
                    survey, scale, xviFile.getFilename(), exportFrame, originalFileContent);
        }
        th2File.save(context, content);


        String xviContent = XviExporter.getContent(sketch, space, scale, exportFrame);
        xviFile.save(context, xviContent);

        th2Files.add(th2File.getFilename());
    }


    @Override
    public String getExportTypeName(Context context) {
        return context.getString(R.string.third_party_therion);
    }


    @Override
    protected String getExportDirectoryName() {
        return "therion";
    }


    public static String getEncodingText() {
        return "encoding utf-8";
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public static float getScale() {
        final float PLAN_DPI = 200;
        final float PLAN_SCALE = 100;
        final float CENTIMETRES_PER_METRE = 2.54f;
        final float scale = 100 * PLAN_DPI / (CENTIMETRES_PER_METRE * PLAN_SCALE);
        return scale;
    }


    private void readOriginalFilesIfPresent(Context context, Survey survey) {

        /*
        This code currently sacrificed on the altar of trying to get SexyTopo working
        with scoped storage :/

        if (IoUtils.wasSurveyImported(context, survey)) {
            DocumentFile directory = IoUtils.getFileSurveyWasImportedFrom(context, survey);

            if (new TherionImporter().canHandleFile(directory)) {
                File[] files = directory.listFiles();
                if (files == null) {
                    files = new File[] {};
                }

                for (File file : files) {
                    if (FilenameUtils.getExtension(file.getName()).equals("th")) {
                        originalThFileContent = Loader.slurpFile(file);
                    } else if (file.getName().endsWith(SexyTopo.PLAN_SUFFIX + ".th2")) {
                        originalTh2PlanFileContent = Loader.slurpFile(file);
                    } else if (file.getName().endsWith(SexyTopo.PLAN_SUFFIX + ".th2")) {
                        originalTh2EeFileContent = Loader.slurpFile(file);
                    }
                }
            }
        }*/
    }

}

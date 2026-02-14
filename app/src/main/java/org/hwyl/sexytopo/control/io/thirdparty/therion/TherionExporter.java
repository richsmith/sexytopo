package org.hwyl.sexytopo.control.io.thirdparty.therion;

import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.SurveyFile;
import org.hwyl.sexytopo.control.io.basic.ExportFrameFactory;
import org.hwyl.sexytopo.control.io.thirdparty.xvi.XviExporter;
import org.hwyl.sexytopo.control.io.translation.Exporter;
import org.hwyl.sexytopo.control.util.SpaceFlipper;
import org.hwyl.sexytopo.control.util.TextTools;
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

    public static final char COMMENT_CHAR = '#';
    private static final SurveyFile.SurveyFileType THCONFIG =
            new SurveyFile.SurveyFileType("thconfig", "plain/text");
    private static final SurveyFile.SurveyFileType TH =
            new SurveyFile.SurveyFileType("th", "plain/text");
    private static final SurveyFile.SurveyFileType TH2_PLAN =
            new SurveyFile.SurveyFileType("plan.th2", "plain/text");
    private static final SurveyFile.SurveyFileType XVI_PLAN =
            new SurveyFile.SurveyFileType("plan.xvi", "plain/text");
    private static final SurveyFile.SurveyFileType TH2_EE =
            new SurveyFile.SurveyFileType("ee.th2", "plain/text");
    private static final SurveyFile.SurveyFileType XVI_EE =
            new SurveyFile.SurveyFileType("ee.xvi", "plain/text");

    private String originalThFileContent = null;
    private String originalTh2PlanFileContent = null;
    private String originalTh2EeFileContent = null;

    private final List<String> th2Files = new ArrayList<>();

    public void run(Context context, Survey survey) throws IOException {

        String attribution = getFileAttribution(context);

        // Therion export notes:
        // Therion coordinate system is in traditional graph-style, with positive y going up.

        th2Files.clear();
        readOriginalFilesIfPresent(context, survey);

        String thconfigContent = ThconfigExporter.getContent(survey);
        SurveyFile thconfig = getOutputFile(THCONFIG);
        thconfig.save(context, attribution + thconfigContent);

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
        // FIXED: Removed 'attribution +' to prevent duplicate "Created with SexyTopo" comment
        // ThExporter now handles the creation comment in the correct location
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
        space = SpaceFlipper.flipVertically(space);
        // Therion y-coordinates are inverse of SexyTopo's
        // (it would be more consistent to either do all the processing like scaling and flipping
        // all at once or all just before using, but we currently have a mix :/)

        Sketch sketch = survey.getSketch(projectionType);

        Frame baseFrame = ExportFrameFactory.getExportFrame(survey, projectionType);
        Frame gridFrame = ExportFrameFactory.addBorder(baseFrame);
        Frame outerFrame = ExportFrameFactory.addBorder(gridFrame);

        Frame innerFrame = baseFrame.scale(scale);
        innerFrame.flipVertically();

        gridFrame = gridFrame.scale(scale);
        gridFrame.flipVertically();

        outerFrame = outerFrame.scale(scale);
        outerFrame.flipVertically();

        String content;

        content = Th2Exporter.getContent(
            survey, projectionType, space, xviFile.getFilename(), innerFrame, outerFrame, scale);
        th2File.save(context, content);

        String xviContent = XviExporter.getContent(sketch, space, scale, gridFrame);
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

    private String getFileAttribution(Context context) {
        return COMMENT_CHAR + " " + TextTools.getFileAttribution(context) + "\n\n";
    }

}

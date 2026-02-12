package org.hwyl.sexytopo.control.io.thirdparty.therion;

import android.content.Context;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.SurveyDirectory;
import org.hwyl.sexytopo.control.io.SurveyFile;
import org.hwyl.sexytopo.control.io.basic.ExportFrameFactory;
import org.hwyl.sexytopo.control.io.thirdparty.xvi.XviExporter;
import org.hwyl.sexytopo.control.io.translation.Exporter;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
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

    private String originalThFileContent = null;
    private String originalTh2PlanFileContent = null;
    private String originalTh2EeFileContent = null;

    private final List<String> th2Files = new ArrayList<>();

    private TherionExportOptions exportOptions = new TherionExportOptions();

    public void setExportOptions(TherionExportOptions options) {
        this.exportOptions = options;
    }

    public TherionExportOptions getExportOptions() {
        return exportOptions;
    }

    public void run(Context context, Survey survey) throws IOException {

        String attribution = getFileAttribution(context);

        // Therion coordinate system is in traditional graph-style, with positive y going up.

        th2Files.clear();
        readOriginalFilesIfPresent(context, survey);

        String thconfigContent = ThconfigExporter.getContent(survey);
        SurveyFile thconfig = getOutputFile(createFileType("thconfig"));
        thconfig.save(context, attribution + thconfigContent);

        String planSuffix = GeneralPreferences.getTherionPlanSuffix();
        String eeSuffix = GeneralPreferences.getTherionEeSuffix();
        String xviFolder = GeneralPreferences.getTherionXviFolder();

        SurveyFile.SurveyFileType th2PlanType = createFileType(buildExtension(planSuffix, "th2"));
        SurveyFile.SurveyFileType xviPlanType = createFileType(buildExtension(planSuffix, "xvi"));
        SurveyFile th2PlanFile = getOutputFile(th2PlanType);
        SurveyFile xviPlanFile = getXviOutputFile(context, xviPlanType, xviFolder);

        handleProjection(context, survey, Projection2D.PLAN,
                th2PlanFile, xviPlanFile, originalTh2PlanFileContent,
                exportOptions.getPlanScrapCount(),
                exportOptions.isStationsInFirstPlanScrap(),
                xviFolder);

        SurveyFile.SurveyFileType th2EeType = createFileType(buildExtension(eeSuffix, "th2"));
        SurveyFile.SurveyFileType xviEeType = createFileType(buildExtension(eeSuffix, "xvi"));
        SurveyFile th2EeFile = getOutputFile(th2EeType);
        SurveyFile xviEeFile = getXviOutputFile(context, xviEeType, xviFolder);

        handleProjection(context, survey, Projection2D.EXTENDED_ELEVATION,
                th2EeFile, xviEeFile, originalTh2EeFileContent,
                exportOptions.getEeScrapCount(),
                exportOptions.isStationsInFirstEeScrap(),
                xviFolder);

        String thContent;
        if (originalThFileContent == null) {
            thContent = ThExporter.getContent(context, survey, th2Files);
        } else {
            thContent = ThExporter.updateOriginalContent(survey, originalThFileContent, th2Files);
        }
        SurveyFile th = getOutputFile(createFileType("th"));
        th.save(context, thContent);
    }

    private SurveyFile.SurveyFileType createFileType(String extension) {
        return new SurveyFile.SurveyFileType(extension, "plain/text");
    }

    private String buildExtension(String suffix, String fileType) {
        if (suffix == null || suffix.isEmpty()) {
            return fileType;
        }
        // If suffix starts with a dot (e.g., ".plan"), preserve it
        // SurveyFile.withExtension will not add another dot if extension starts with "."
        // Result: filename.plan.th2
        if (suffix.startsWith(".")) {
            return suffix + "." + fileType;
        } else if (suffix.endsWith(".")) {
            // Suffix ends with dot already (e.g., "P.")
            // Use "|" marker for direct append (no separator)
            // Result: filenameP.th2
            return "|" + suffix + fileType;
        } else {
            // No leading dot in suffix (e.g., "P")
            // Use "|" marker for direct append (no separator between filename and suffix)
            // Result: filenameP.th2
            return "|" + suffix + "." + fileType;
        }
    }

    private SurveyFile getXviOutputFile(Context context, SurveyFile.SurveyFileType fileType, String xviFolder) {
        if (xviFolder == null || xviFolder.trim().isEmpty()) {
            return getOutputFile(fileType);
        }

        SurveyDirectory parent = getParentExportDirectory();
        SurveyDirectory exportDir = getExportDirectory(parent);

        SurveyDirectory.SurveyDirectoryType xviDirType =
                new SurveyDirectory.SurveyDirectoryType(xviFolder);
        SurveyDirectory xviDir = xviDirType.get(exportDir);
        xviDir.ensureExists(context);

        return fileType.get(xviDir);
    }

    private String getXviPathForTh2(String xviFilename, String xviFolder) {
        if (xviFolder == null || xviFolder.trim().isEmpty()) {
            return xviFilename;
        }
        return xviFolder + "/" + xviFilename;
    }


    private void handleProjection(
            Context context,
            Survey survey,
            Projection2D projectionType,
            SurveyFile th2File,
            SurveyFile xviFile,
            String originalFileContent,
            int scrapCount,
            boolean stationsInFirstScrap,
            String xviFolder)
            throws IOException {

        float scale = getScale();

        Space<Coord2D> space = projectionType.project(survey);
        space = SpaceFlipper.flipVertically(space);

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

        String xviPathForTh2 = getXviPathForTh2(xviFile.getFilename(), xviFolder);

        String content = Th2Exporter.getContent(
            survey, projectionType, space, xviPathForTh2, innerFrame, outerFrame, scale,
            scrapCount, stationsInFirstScrap);
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
        // Code currently sacrificed on the altar of trying to get SexyTopo working
        // with scoped storage :/
    }

    private String getFileAttribution(Context context) {
        return COMMENT_CHAR + " " + TextTools.getFileAttribution(context) + "\n\n";
    }

}

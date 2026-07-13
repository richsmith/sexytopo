package org.hwyl.sexytopo.tools;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.hwyl.sexytopo.control.io.basic.ExportFrameFactory;
import org.hwyl.sexytopo.control.io.thirdparty.therion.Th2Exporter;
import org.hwyl.sexytopo.control.io.thirdparty.therion.ThExporter;
import org.hwyl.sexytopo.control.io.thirdparty.therion.ThconfigExporter;
import org.hwyl.sexytopo.control.io.thirdparty.therion.TherionExporter;
import org.hwyl.sexytopo.control.io.thirdparty.xvi.XviExporter;
import org.hwyl.sexytopo.model.common.Frame;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.ExampleSurveyCreator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TherionFixtureExporter {

    @Test
    public void writeFixtures() throws Exception {
        File outputRoot = new File("build/exports");
        ensureDir(outputRoot);

        String name = "example";
        Survey survey = ExampleSurveyCreator.create(10, 5, true, true, true);
        renameSurvey(survey, name);
        File dir = new File(outputRoot, name);
        ensureDir(dir);
        exportTherion(survey, dir);
        System.out.println("Wrote " + name + " bundle to " + dir.getAbsolutePath());
    }

    private static void exportTherion(Survey survey, File dir) throws IOException {
        String baseName = survey.getName();

        write(new File(dir, baseName + ".thconfig"), ThconfigExporter.getContent(survey));

        List<String> th2Files = new ArrayList<>();
        exportProjection(survey, Projection2D.PLAN, ".plan", dir, baseName, th2Files);
        exportProjection(survey, Projection2D.EXTENDED_ELEVATION, ".ee", dir, baseName, th2Files);

        String th = ThExporter.getContent(null, survey, th2Files);
        write(new File(dir, baseName + ".th"), th);
    }

    private static void exportProjection(
            Survey survey,
            Projection2D projection,
            String suffix,
            File dir,
            String baseName,
            List<String> th2FilesOut)
            throws IOException {

        float scale = TherionExporter.getScale();

        Space<Coord2D> space = projection.project(survey);

        Sketch sketch = survey.getSketch(projection);

        Frame baseFrame = ExportFrameFactory.getExportFrame(survey, projection);
        Frame gridFrame = ExportFrameFactory.addBorder(baseFrame);
        Frame outerFrame = ExportFrameFactory.addBorder(gridFrame);

        Frame innerFrame = baseFrame.scale(scale);
        innerFrame.flipVertically();

        gridFrame = gridFrame.scale(scale);
        gridFrame.flipVertically();

        outerFrame = outerFrame.scale(scale);
        outerFrame.flipVertically();

        String th2Filename = baseName + suffix + ".th2";
        String xviFilename = baseName + suffix + ".xvi";

        String th2 =
                Th2Exporter.getContent(
                        survey, projection, space, xviFilename, innerFrame, outerFrame, scale);
        write(new File(dir, th2Filename), th2);

        String xvi = XviExporter.getContent(sketch, space, scale, gridFrame);
        write(new File(dir, xviFilename), xvi);

        th2FilesOut.add(th2Filename);
    }

    private static void write(File file, String content) throws IOException {
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private static void ensureDir(File dir) throws IOException {
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("Could not create directory: " + dir.getAbsolutePath());
        }
    }

    /** Survey.setName is private; use reflection so fixtures get readable filenames. */
    private static void renameSurvey(Survey survey, String name) {
        try {
            Method setName = Survey.class.getDeclaredMethod("setName", String.class);
            setName.setAccessible(true);
            setName.invoke(survey, name);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not rename survey", e);
        }
    }
}

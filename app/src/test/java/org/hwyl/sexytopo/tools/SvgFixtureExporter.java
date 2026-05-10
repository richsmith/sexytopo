package org.hwyl.sexytopo.tools;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.hwyl.sexytopo.control.io.thirdparty.svg.SvgExporter;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.ExampleSurveyCreator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/**
 * Dumps an SVG export of a fixture survey so it can be inspected in a browser or vector editor
 * without needing to dig files out of the emulator.
 */
@RunWith(RobolectricTestRunner.class)
public class SvgFixtureExporter {

    @Test
    public void writeFixtures() throws Exception {
        GeneralPreferences.initialise(RuntimeEnvironment.getApplication());

        File outputRoot = new File("build/exports");
        ensureDir(outputRoot);

        String displayName = "Smith's Folly";
        String fileName = "example";
        Survey survey = ExampleSurveyCreator.create(10, 5, true, true, true);
        renameSurvey(survey, displayName);
        survey.setTrip(ExampleSurveyCreator.createExampleTrip());
        File dir = new File(outputRoot, fileName);
        ensureDir(dir);

        SvgExporter exporter = new SvgExporter();
        write(
                new File(dir, fileName + ".plan.svg"),
                exporter.getContent(survey, Projection2D.PLAN));
        write(
                new File(dir, fileName + ".ee.svg"),
                exporter.getContent(survey, Projection2D.EXTENDED_ELEVATION));
        System.out.println("Wrote " + displayName + " SVG bundle to " + dir.getAbsolutePath());
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

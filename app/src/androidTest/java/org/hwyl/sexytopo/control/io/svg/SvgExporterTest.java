package org.hwyl.sexytopo.control.io.svg;

import android.content.res.Resources;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hwyl.sexytopo.control.io.thirdparty.svg.SvgExporter;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.Symbol;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSketchCreator;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class SvgExporterTest {

    public void setUp() {
        Resources resources = Resources.getSystem();
        Symbol.setResources(resources);
    }

    @Test
    public void testEmptySurveyGeneratesOk() throws IOException {
        Survey testSurvey = BasicTestSurveyCreator.createEmptySurvey();
        SvgExporter svgExporter = new SvgExporter();
        Assert.assertNotNull(testSurvey.getPlanSketch());
        String content = svgExporter.getContent(testSurvey.getPlanSketch());
        Assert.assertTrue(content.contains("<svg"));
    }

    @Test
    public void testSurveyWithLineSketchGeneratesOk() throws IOException {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();
        Sketch sketch = testSurvey.getPlanSketch();
        BasicTestSketchCreator.drawOneHorizontalLine(sketch);
        SvgExporter svgExporter = new SvgExporter();
        String content = svgExporter.getContent(testSurvey.getPlanSketch());
        Assert.assertTrue(content.contains("<svg"));
    }

    @Test
    public void testSurveyWithSymbolGeneratesOk() throws IOException {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();
        Sketch sketch = testSurvey.getPlanSketch();
        BasicTestSketchCreator.drawSymbol(sketch, Symbol.STALACTITE);
        SvgExporter svgExporter = new SvgExporter();
        String content = svgExporter.getContent(testSurvey.getPlanSketch());
        Assert.assertTrue(content.contains("<svg"));
    }

}

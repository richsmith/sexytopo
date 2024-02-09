package org.hwyl.sexytopo.control.io.svg;

import android.content.res.Resources;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.io.thirdparty.svg.SvgExporter;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.Symbol;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSketchCreator;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SvgExporterTest {

    public void setUp() {
        Resources resources = Resources.getSystem();
        Symbol.setResources(resources);
    }

    @Test
    public void testEmptySurveyGeneratesOk() throws Exception {
        Survey testSurvey = BasicTestSurveyCreator.createEmptySurvey();
        SvgExporter svgExporter = new SvgExporter();
        Assert.assertNotNull(testSurvey.getPlanSketch());
        String content = svgExporter.getContent(testSurvey, Projection2D.PLAN);
        Assert.assertTrue(content.contains("<svg"));
    }

    @Test
    public void testSurveyWithLineSketchGeneratesOk() throws Exception {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();
        Sketch sketch = testSurvey.getPlanSketch();
        BasicTestSketchCreator.drawOneHorizontalLine(sketch);
        SvgExporter svgExporter = new SvgExporter();
        String content = svgExporter.getContent(testSurvey, Projection2D.PLAN);
        Assert.assertTrue(content.contains("<svg"));
    }

    @Test
    public void testSurveyWithSymbolGeneratesOk() throws Exception {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();
        Sketch sketch = testSurvey.getPlanSketch();
        BasicTestSketchCreator.drawSymbol(sketch, Symbol.STALACTITE);
        SvgExporter svgExporter = new SvgExporter();
        String content = svgExporter.getContent(testSurvey, Projection2D.PLAN);
        Log.i(SexyTopoConstants.TAG, content);
        Assert.assertTrue(content.contains("<svg"));
    }

}

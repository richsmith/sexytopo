package org.hwyl.sexytopo.control.io.thirdparty.xvi;

import org.hwyl.sexytopo.control.io.basic.ExportFrameFactory;
import org.hwyl.sexytopo.model.common.Frame;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSketchCreator;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;


public class XviExporterTest {

    @Test
    public void testLineIsPassedThroughToXvi() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();
        Projection2D projection = Projection2D.PLAN;
        Sketch sketch = testSurvey.getSketch(projection);
        BasicTestSketchCreator.drawOneHorizontalLine(sketch);
        Frame frame = ExportFrameFactory.getExportFrame(testSurvey, Projection2D.PLAN);
        Space<Coord2D> space = projection.project(testSurvey);
        String xvi = XviExporter.getContent(sketch, space, 1.0f, frame);
        Assert.assertTrue(xvi.contains("{BLACK 5.00 0.00 10.00 0.00}"));
    }
}

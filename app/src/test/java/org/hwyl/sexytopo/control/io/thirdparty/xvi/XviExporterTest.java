package org.hwyl.sexytopo.control.io.thirdparty.xvi;

import org.hwyl.sexytopo.BasicTestSketchCreator;
import org.hwyl.sexytopo.BasicTestSurveyCreator;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.survey.Survey;
import org.junit.Assert;
import org.junit.Test;


public class XviExporterTest {

    @Test
    public void testLineIsPassedThroughToXvi() {
        Survey testSurvey = BasicTestSurveyCreator.createStraightNorth();
        BasicTestSketchCreator.drawOneHorizontalLine(testSurvey.getPlanSketch());

        String xvi = XviExporter.toXvi(testSurvey, testSurvey.getPlanSketch(), Projection2D.PLAN);

        Assert.assertTrue(xvi.contains("[BLACK, (5.00, 0.00), (10.00, 0.00)]"));
    }
}

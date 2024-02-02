package org.hwyl.sexytopo.control.io.thirdparty.therion;

import org.hwyl.sexytopo.control.io.basic.ExportFrameFactory;
import org.hwyl.sexytopo.model.common.Frame;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;


public class Th2ExporterTest {

    @Test
    public void testHappyPath() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        float scale = TherionExporter.getScale();
        Projection2D projection = Projection2D.PLAN;
        Frame exportFrame = ExportFrameFactory.getExportFrame(survey, projection);
        Space<Coord2D> space = projection.project(survey);
        exportFrame = exportFrame.scale(scale);
        String th2 = Th2Exporter.getContent(survey, projection, space, "filename.xvi", exportFrame, exportFrame);
        Assert.assertTrue(th2.contains("##XTHERION##"));
    }

}

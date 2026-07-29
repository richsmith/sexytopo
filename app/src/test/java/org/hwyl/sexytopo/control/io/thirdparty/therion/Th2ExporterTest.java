package org.hwyl.sexytopo.control.io.thirdparty.therion;

import java.util.ArrayList;
import java.util.List;
import org.hwyl.sexytopo.control.io.basic.ExportFrameFactory;
import org.hwyl.sexytopo.model.common.Frame;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.AreaDetail;
import org.hwyl.sexytopo.model.sketch.AreaType;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;

public class Th2ExporterTest {

    @Test
    public void testHappyPath() {
        Survey survey = BasicTestSurveyCreator.createWithCrossSections();
        float scale = TherionExporter.getScale();
        Projection2D projection = Projection2D.PLAN;
        Frame exportFrame = ExportFrameFactory.getExportFrame(survey, projection);
        Space<Coord2D> space = projection.project(survey);
        exportFrame = exportFrame.scale(scale);
        String th2 =
                Th2Exporter.getContent(
                        survey, projection, space, "filename.xvi", exportFrame, exportFrame, scale);
        Assert.assertTrue(th2.contains("##XTHERION##"));
    }

    @Test
    public void testAreaExportsAsInvisibleBorderPlusArea() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();

        List<Coord2D> polygon = new ArrayList<>();
        polygon.add(new Coord2D(1, 1));
        polygon.add(new Coord2D(4, 1));
        polygon.add(new Coord2D(4, 3));
        polygon.add(new Coord2D(1, 3));
        List<AreaDetail> areaDetails = new ArrayList<>();
        areaDetails.add(new AreaDetail(polygon, AreaType.WATER, Colour.BLUE));
        survey.getPlanSketch().setAreaDetails(areaDetails);

        float scale = TherionExporter.getScale();
        Projection2D projection = Projection2D.PLAN;
        Frame exportFrame = ExportFrameFactory.getExportFrame(survey, projection).scale(scale);
        Space<Coord2D> space = projection.project(survey);
        String th2 =
                Th2Exporter.getContent(
                        survey, projection, space, "filename.xvi", exportFrame, exportFrame, scale);

        Assert.assertTrue(th2.contains("line border:invisible -id water1 -close on"));
        Assert.assertTrue(th2.contains("endline"));
        Assert.assertTrue(th2.contains("area water"));
        Assert.assertTrue(th2.contains("endarea"));

        // border line: the start point appears twice (repeated at the end to close the loop)
        String firstPoint = "  " + (1 * scale) + " " + (-1 * scale);
        int firstPointCount = th2.split(firstPoint, -1).length - 1;
        Assert.assertEquals(2, firstPointCount);
    }
}

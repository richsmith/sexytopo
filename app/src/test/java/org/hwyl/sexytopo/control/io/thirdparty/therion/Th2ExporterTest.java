package org.hwyl.sexytopo.control.io.thirdparty.therion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hwyl.sexytopo.control.io.basic.ExportFrameFactory;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionUtil;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurveyFormat;
import org.hwyl.sexytopo.model.common.Frame;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.AreaDetail;
import org.hwyl.sexytopo.model.sketch.AreaType;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.LineType;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.Trip;
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

    @Test
    public void testSemanticLineExportsAsTherionLine() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();

        List<Coord2D> wallPoints = Arrays.asList(new Coord2D(1, 1), new Coord2D(4, 2));
        List<Coord2D> generalPoints = Arrays.asList(new Coord2D(6, 7), new Coord2D(8, 9));
        List<PathDetail> pathDetails = new ArrayList<>();
        pathDetails.add(new PathDetail(new ArrayList<>(wallPoints), Colour.BLACK, LineType.WALL));
        pathDetails.add(new PathDetail(new ArrayList<>(generalPoints), Colour.BLACK));
        survey.getPlanSketch().setPathDetails(pathDetails);

        float scale = TherionExporter.getScale();
        Projection2D projection = Projection2D.PLAN;
        Frame exportFrame = ExportFrameFactory.getExportFrame(survey, projection).scale(scale);
        Space<Coord2D> space = projection.project(survey);
        String th2 =
                Th2Exporter.getContent(
                        survey, projection, space, "filename.xvi", exportFrame, exportFrame, scale);

        // the wall is a first-class Therion line (points y-flipped and scaled)
        Assert.assertTrue(th2.contains("line wall\n  " + (1 * scale) + " " + (-1 * scale) + "\n"));
        // general sketch lines only appear in the XVI tracing background, not the th2
        Assert.assertFalse(th2.contains("  " + (6 * scale) + " " + (-7 * scale)));
    }

    @Test
    public void testAreaWithHoleExportsBorderPerContour() {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();

        List<Coord2D> outline =
                Arrays.asList(
                        new Coord2D(0, 0), new Coord2D(4, 0), new Coord2D(4, 4), new Coord2D(0, 4));
        List<Coord2D> hole =
                Arrays.asList(
                        new Coord2D(1, 1), new Coord2D(3, 1), new Coord2D(3, 3), new Coord2D(1, 3));
        AreaDetail areaDetail =
                new AreaDetail(
                        outline, Collections.singletonList(hole), AreaType.WATER, Colour.BLUE);
        survey.getPlanSketch().setAreaDetails(Collections.singletonList(areaDetail));

        float scale = TherionExporter.getScale();
        Projection2D projection = Projection2D.PLAN;
        Frame exportFrame = ExportFrameFactory.getExportFrame(survey, projection).scale(scale);
        Space<Coord2D> space = projection.project(survey);
        String th2 =
                Th2Exporter.getContent(
                        survey, projection, space, "filename.xvi", exportFrame, exportFrame, scale);

        // one border line per contour, both referenced from the single area block
        Assert.assertTrue(th2.contains("line border:invisible -id water1 -close on"));
        Assert.assertTrue(th2.contains("line border:invisible -id water1b -close on"));
        Assert.assertTrue(th2.contains("area water\n  water1\n  water1b\nendarea"));
    }

    @Test
    public void testCopyrightLineAppearsAfterScrapLineInEveryScrap() {
        // createWithCrossSections() gives us one main "plan" scrap plus two cross-section
        // scraps, so this exercises both getScrap() and getCrossSectionScrap().
        Survey survey = BasicTestSurveyCreator.createWithCrossSections();
        Trip trip = new Trip();
        trip.setCopyrightHolder("Caver Jane");
        trip.setLicence("CC BY 4.0");
        survey.setTrip(trip);

        float scale = TherionExporter.getScale();
        Projection2D projection = Projection2D.PLAN;
        Frame exportFrame = ExportFrameFactory.getExportFrame(survey, projection);
        Space<Coord2D> space = projection.project(survey);
        exportFrame = exportFrame.scale(scale);
        String th2 =
                Th2Exporter.getContent(
                        survey, projection, space, "filename.xvi", exportFrame, exportFrame, scale);

        String expectedCopyrightLine =
                SurvexTherionUtil.getCopyrightLine(survey, SurveyFormat.THERION).trim();

        String[] lines = th2.split("\n");
        int scrapLineCount = 0;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith("scrap ")) {
                scrapLineCount++;
                Assert.assertEquals(
                        "Copyright line should immediately follow: " + lines[i],
                        expectedCopyrightLine,
                        lines[i + 1]);
            }
        }
        // One main "plan" scrap + two cross-section scraps
        Assert.assertEquals(3, scrapLineCount);
    }

    @Test
    public void testNoCopyrightLineWhenTripHasNeitherCopyrightNorLicence() {
        Survey survey = BasicTestSurveyCreator.createWithCrossSections();
        // No trip set at all, matching a survey that has never used this feature.

        float scale = TherionExporter.getScale();
        Projection2D projection = Projection2D.PLAN;
        Frame exportFrame = ExportFrameFactory.getExportFrame(survey, projection);
        Space<Coord2D> space = projection.project(survey);
        exportFrame = exportFrame.scale(scale);
        String th2 =
                Th2Exporter.getContent(
                        survey, projection, space, "filename.xvi", exportFrame, exportFrame, scale);

        Assert.assertFalse(th2.contains("copyright"));
    }
}

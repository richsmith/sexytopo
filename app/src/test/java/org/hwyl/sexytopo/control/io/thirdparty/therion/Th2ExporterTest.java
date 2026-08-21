package org.hwyl.sexytopo.control.io.thirdparty.therion;

import org.hwyl.sexytopo.control.io.basic.ExportFrameFactory;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionUtil;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurveyFormat;
import org.hwyl.sexytopo.model.common.Frame;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
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

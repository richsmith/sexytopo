package org.hwyl.sexytopo.control.io.thirdparty.xvi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.hwyl.sexytopo.control.io.basic.ExportFrameFactory;
import org.hwyl.sexytopo.model.common.Frame;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSketchCreator;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Test;

public class XviExporterTest {

    @Test
    public void testLineIsPassedThroughToXvi() {
        Survey testSurvey = BasicTestSurveyCreator.createWithCrossSections();
        Projection2D projection = Projection2D.PLAN;
        Sketch sketch = testSurvey.getSketch(projection);
        BasicTestSketchCreator.drawOneHorizontalLine(sketch);
        Frame frame = ExportFrameFactory.getExportFrame(testSurvey, Projection2D.PLAN);
        Space<Coord2D> space = projection.project(testSurvey);
        String xvi = XviExporter.getContent(sketch, space, 1.0f, frame);
        Assert.assertTrue(xvi.contains("{BLACK 5.00 0.00 10.00 0.00}"));
    }

    // Cross-sections on the elevation

    // What BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation() gives every
    // cross-section: splays 2m east, 1.5m west, 1m north, 1m up and 1m down. Each is listed here as
    // how far it goes across the XVI's page and up it, in metres, so the up splay is (0, 1) and the
    // north one, which a vertical section facing north sees head on, is (0, 0).
    private static final List<String> VERTICAL_SPLAYS =
            sorted("2.00,0.00", "-1.50,0.00", "0.00,0.00", "0.00,1.00", "0.00,-1.00");
    private static final List<String> HORIZONTAL_SPLAYS =
            sorted("2.00,0.00", "-1.50,0.00", "0.00,1.00", "0.00,0.00", "0.00,0.00");

    private static List<String> sorted(String... vectors) {
        List<String> list = new ArrayList<>(Arrays.asList(vectors));
        Collections.sort(list);
        return list;
    }

    private static String export(Survey survey, Projection2D projection) {
        Sketch sketch = survey.getSketch(projection);
        Frame frame = ExportFrameFactory.getExportFrame(survey, projection);
        Space<Coord2D> space = projection.project(survey);
        return XviExporter.getContent(sketch, space, 1.0f, frame);
    }

    /** The entries of one of the XVI's lists, such as XVIshots, without their braces. */
    private static List<String> getEntries(String xvi, String listName) {
        List<String> entries = new ArrayList<>();
        boolean inList = false;
        for (String line : xvi.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("set " + listName)) {
                inList = true;
            } else if (inList) {
                if (trimmed.equals("}")) {
                    break;
                }
                if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                    entries.add(trimmed.substring(1, trimmed.length() - 1));
                }
            }
        }
        return entries;
    }

    /** The connectors from each station to its cross-section, as "x1 y1 x2 y2". */
    private static List<String> getConnectors(String xvi) {
        List<String> connectors = new ArrayList<>();
        for (String entry : getEntries(xvi, "XVIsketchlines")) {
            if (entry.startsWith("connect ")) {
                connectors.add(entry.substring("connect ".length()));
            }
        }
        return connectors;
    }

    /** The shots leaving a point, as how far across and up each goes, in metres, sorted. */
    private static List<String> getShotsFrom(String xvi, float x, float y) {
        List<String> shots = new ArrayList<>();
        for (String entry : getEntries(xvi, "XVIshots")) {
            String[] numbers = entry.split(" ");
            float startX = Float.parseFloat(numbers[0]);
            float startY = Float.parseFloat(numbers[1]);
            if (Math.abs(startX - x) < 0.005f && Math.abs(startY - y) < 0.005f) {
                float across = Float.parseFloat(numbers[2]) - startX;
                float up = Float.parseFloat(numbers[3]) - startY;
                shots.add(String.format(Locale.US, "%.2f,%.2f", tidy(across), tidy(up)));
            }
        }
        Collections.sort(shots);
        return shots;
    }

    // Round off float noise, and turn any negative zero into an ordinary zero
    private static float tidy(float value) {
        return Math.round(value * 100f) / 100f + 0f;
    }

    @Test
    public void testElevationHasAConnectorFromEachStationToItsCrossSection() {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();

        List<String> connectors = getConnectors(export(survey, Projection2D.EXTENDED_ELEVATION));
        Collections.sort(connectors);

        // Station 1 is at the origin and 3 is 10m along, and the cross-sections at (2, 4) and
        // (12, 4) are drawn 4m up, which is 4 the other way on the XVI's page
        Assert.assertEquals(sorted("0.00 0.00 2.00 -4.00", "10.00 0.00 12.00 -4.00"), connectors);
    }

    @Test
    public void testElevationCrossSectionsHaveTheirStationsNameAtTheirPosition() {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();

        List<String> stations =
                getEntries(export(survey, Projection2D.EXTENDED_ELEVATION), "XVIstations");

        Assert.assertTrue(stations.contains("2.00 -4.00 1"));
        Assert.assertTrue(stations.contains("12.00 -4.00 3"));
    }

    @Test
    public void testVerticalCrossSectionOnTheElevationHasItsSplaysAcrossThePassage() {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();

        String xvi = export(survey, Projection2D.EXTENDED_ELEVATION);

        Assert.assertEquals(VERTICAL_SPLAYS, getShotsFrom(xvi, 2, -4));
    }

    @Test
    public void testHorizontalCrossSectionHasItsSplaysLyingFlatWithNorthUp() {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();

        String xvi = export(survey, Projection2D.EXTENDED_ELEVATION);

        // The north splay goes up the page, and the up and down splays have nothing to show
        Assert.assertEquals(HORIZONTAL_SPLAYS, getShotsFrom(xvi, 12, -4));
    }

    @Test
    public void testTheSameStationHasADifferentCrossSectionOnThePlanAndTheElevation() {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();

        String plan = export(survey, Projection2D.PLAN);
        String elevation = export(survey, Projection2D.EXTENDED_ELEVATION);

        // Station 3 has a vertical cross-section on the plan, drawn at (2, 10)
        Assert.assertEquals(VERTICAL_SPLAYS, getShotsFrom(plan, 2, -10));
        // ...and a horizontal one on the elevation, drawn at (12, 4)
        Assert.assertEquals(HORIZONTAL_SPLAYS, getShotsFrom(elevation, 12, -4));
    }

    @Test
    public void testElevationCrossSectionsAreDrawnAtTheCrossSectionScale() {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();
        survey.setCrossSectionScale(2f);

        String xvi = export(survey, Projection2D.EXTENDED_ELEVATION);

        Assert.assertEquals(
                sorted("4.00,0.00", "-3.00,0.00", "0.00,0.00", "0.00,2.00", "0.00,-2.00"),
                getShotsFrom(xvi, 2, -4));
        Assert.assertEquals(
                sorted("4.00,0.00", "-3.00,0.00", "0.00,2.00", "0.00,0.00", "0.00,0.00"),
                getShotsFrom(xvi, 12, -4));
    }

    @Test
    public void testEachExportOnlyHasItsOwnSketchsCrossSections() {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();
        survey.getSketch(Projection2D.EXTENDED_ELEVATION)
                .addCrossSection(
                        CrossSection.horizontal(survey.getStationByName("2")), new Coord2D(7, 4));

        Assert.assertEquals(2, getConnectors(export(survey, Projection2D.PLAN)).size());
        Assert.assertEquals(
                3, getConnectors(export(survey, Projection2D.EXTENDED_ELEVATION)).size());
    }

    @Test
    public void testWhatIsDrawnInAnElevationCrossSectionIsPlacedAndScaledWithIt() {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();
        Sketch elevation = survey.getSketch(Projection2D.EXTENDED_ELEVATION);
        // Each gets a line from (5, 0) to (10, 0) in the cross-section's own drawing
        BasicTestSketchCreator.drawOneHorizontalLine(
                elevation.getCrossSectionDetail(survey.getStationByName("1")).getSketch());
        BasicTestSketchCreator.drawOneHorizontalLine(
                elevation.getCrossSectionDetail(survey.getStationByName("3")).getSketch());

        String xvi = export(survey, Projection2D.EXTENDED_ELEVATION);

        // The cross-sections are at (2, 4) and (12, 4), which the XVI has 4 the other way up
        Assert.assertTrue(xvi.contains("{BLACK 7.00 -4.00 12.00 -4.00}"));
        Assert.assertTrue(xvi.contains("{BLACK 17.00 -4.00 22.00 -4.00}"));

        survey.setCrossSectionScale(2f);
        String enlarged = export(survey, Projection2D.EXTENDED_ELEVATION);

        // At twice the size the drawing stretches away from where the cross-section is
        Assert.assertTrue(enlarged.contains("{BLACK 12.00 -4.00 22.00 -4.00}"));
        Assert.assertTrue(enlarged.contains("{BLACK 22.00 -4.00 32.00 -4.00}"));
    }
}

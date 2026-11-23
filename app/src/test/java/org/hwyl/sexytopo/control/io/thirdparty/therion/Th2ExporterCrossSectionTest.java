package org.hwyl.sexytopo.control.io.thirdparty.therion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.geometry.Projection2D;
import org.hwyl.sexytopo.model.sketch.AreaDetail;
import org.hwyl.sexytopo.model.sketch.AreaType;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.LineType;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.Symbol;
import org.hwyl.sexytopo.model.sketch.SymbolDetail;
import org.hwyl.sexytopo.model.sketch.TextDetail;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/** What is drawn in a cross-section goes into its scrap. */
@RunWith(RobolectricTestRunner.class)
public class Th2ExporterCrossSectionTest {

    @Before
    public void setUp() {
        GeneralPreferences.initialise(RuntimeEnvironment.getApplication());
        Symbol.setResources(RuntimeEnvironment.getApplication().getResources());
    }

    /** A plan with station 1's cross-section (at 2, 0) drawn on, at a cross-section scale of 2. */
    private static Survey createWithDrawnOnCrossSection() {
        Survey survey = BasicTestSurveyCreator.createWithCrossSectionsInPlanAndElevation();
        Sketch plan = survey.getPlanSketch();
        plan.setCrossSectionScale(2);

        Sketch subSketch = new Sketch();
        subSketch.setSymbolDetails(
                new ArrayList<>(
                        Collections.singletonList(
                                new SymbolDetail(
                                        new Coord2D(1, -1),
                                        Symbol.STALAGMITE,
                                        Colour.BLACK,
                                        0.5f,
                                        0))));
        subSketch.setTextDetails(
                new ArrayList<>(
                        Collections.singletonList(
                                new TextDetail(new Coord2D(0, 1), "mud", Colour.BLACK, 0.5f))));
        subSketch.setPathDetails(
                new ArrayList<>(
                        Collections.singletonList(
                                new PathDetail(
                                        Arrays.asList(new Coord2D(-1, -1), new Coord2D(-1, 1)),
                                        Colour.BLACK,
                                        LineType.WALL))));
        subSketch.setAreaDetails(
                new ArrayList<>(
                        Collections.singletonList(
                                new AreaDetail(
                                        Arrays.asList(
                                                new Coord2D(0, 0),
                                                new Coord2D(1, 0),
                                                new Coord2D(1, 1),
                                                new Coord2D(0, 1)),
                                        AreaType.WATER,
                                        Colour.BLUE))));

        Station station1 = survey.getStationByName("1");
        plan.getCrossSectionDetail(station1).setSketch(subSketch);
        return survey;
    }

    /** The text of the scrap with the given name, from its scrap line to its endscrap. */
    private static String getScrapText(String th2, String scrapName) {
        int start = th2.indexOf("scrap " + scrapName + " ");
        Assert.assertTrue("No scrap " + scrapName, start >= 0);
        int end = th2.indexOf("endscrap", start);
        return th2.substring(start, end);
    }

    private static String getStation1ScrapName(String th2) {
        for (String name : Th2ExporterTest.getSectionScrapNames(th2)) {
            if (name.endsWith("1")) {
                return name;
            }
        }
        throw new AssertionError("No cross-section scrap for station 1");
    }

    @Test
    public void testCrossSectionDrawingIsExportedIntoItsScrap() {
        Survey survey = createWithDrawnOnCrossSection();
        String th2 = Th2ExporterTest.export(survey, Projection2D.PLAN);
        String xsScrap = getScrapText(th2, getStation1ScrapName(th2));

        Assert.assertTrue(xsScrap.contains(" " + Symbol.STALAGMITE.getTherionName() + " "));
        Assert.assertTrue(xsScrap.contains("label -text") && xsScrap.contains("mud"));
        Assert.assertTrue(xsScrap.contains("line " + LineType.WALL.getTherionName()));
        Assert.assertTrue(xsScrap.contains("\narea water\n"));
    }

    @Test
    public void testCrossSectionDrawingIsNotExportedIntoTheMainScrap() {
        Survey survey = createWithDrawnOnCrossSection();
        String th2 = Th2ExporterTest.export(survey, Projection2D.PLAN);
        String mainScrap = th2.substring(th2.indexOf("scrap "), th2.indexOf("endscrap"));

        Assert.assertFalse(mainScrap.contains(Symbol.STALAGMITE.getTherionName()));
        Assert.assertFalse(mainScrap.contains("area water"));
    }

    @Test
    public void testCrossSectionDrawingIsWhereTheXviShowsIt() {
        Survey survey = createWithDrawnOnCrossSection();
        float scale = TherionExporter.getScale();
        String th2 = Th2ExporterTest.export(survey, Projection2D.PLAN);
        String xsScrap = getScrapText(th2, getStation1ScrapName(th2));

        Pattern symbolPoint =
                Pattern.compile("point (\\S+) (\\S+) " + Symbol.STALAGMITE.getTherionName() + " ");
        List<List<String>> points = Th2ExporterTest.match(xsScrap, symbolPoint);
        Assert.assertEquals(1, points.size());

        // At the cross-section's position (2, 0), plus (1, -1) at the cross-section scale of 2,
        // with y flipped
        Assert.assertEquals(4 * scale, Float.parseFloat(points.get(0).get(0)), 0.01f);
        Assert.assertEquals(2 * scale, Float.parseFloat(points.get(0).get(1)), 0.01f);
    }

    @Test
    public void testCrossSectionAreaIdsDoNotClashWithTheMainScrapsOnes() {
        Survey survey = createWithDrawnOnCrossSection();
        List<Coord2D> polygon =
                Arrays.asList(
                        new Coord2D(1, 1), new Coord2D(4, 1), new Coord2D(4, 3), new Coord2D(1, 3));
        survey.getPlanSketch()
                .setAreaDetails(
                        new ArrayList<>(
                                Collections.singletonList(
                                        new AreaDetail(polygon, AreaType.WATER, Colour.BLUE))));

        String th2 = Th2ExporterTest.export(survey, Projection2D.PLAN);
        String xsScrapName = getStation1ScrapName(th2);

        Assert.assertTrue(th2.contains("-id water1 "));
        Assert.assertTrue(th2.contains("-id " + xsScrapName + "_water1 "));
    }
}

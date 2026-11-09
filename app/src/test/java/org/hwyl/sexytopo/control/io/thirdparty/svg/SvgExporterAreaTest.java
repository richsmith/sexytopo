package org.hwyl.sexytopo.control.io.thirdparty.svg;

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
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SvgExporterAreaTest {

    @Before
    public void setUp() {
        GeneralPreferences.initialise(RuntimeEnvironment.getApplication());
    }

    private static void addWaterArea(Sketch sketch) {
        List<AreaDetail> areaDetails = new ArrayList<>();
        areaDetails.add(rectangleArea(1, AreaType.WATER, Colour.BLUE));
        sketch.setAreaDetails(areaDetails);
    }

    private static AreaDetail rectangleArea(float left, AreaType areaType, Colour colour) {
        List<Coord2D> polygon = new ArrayList<>();
        polygon.add(new Coord2D(left, 1));
        polygon.add(new Coord2D(left + 3, 1));
        polygon.add(new Coord2D(left + 3, 3));
        polygon.add(new Coord2D(left, 3));
        return new AreaDetail(polygon, areaType, colour);
    }

    private static String getPattern(String content, String id) {
        // attribute order isn't guaranteed, so find the id and step back to its element
        int idIndex = content.indexOf("id=\"" + id + "\"");
        Assert.assertTrue("no pattern " + id, idIndex >= 0);
        int start = content.lastIndexOf("<pattern", idIndex);
        return content.substring(start, content.indexOf("</pattern>", start));
    }

    private static int countOccurrences(String content, String text) {
        return content.split(Pattern.quote(text), -1).length - 1;
    }

    @Test
    public void testAreaExportsAsPatternFilledPath() throws Exception {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        addWaterArea(survey.getPlanSketch());

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertTrue(content.contains("<pattern"));
        Assert.assertTrue(content.contains("id=\"area-water-blue\""));
        Assert.assertTrue(content.contains("fill=\"url(#area-water-blue)\""));
        // 4 vertices at SCALE 50: (50,50) (200,50) (200,150) (50,150)
        Assert.assertTrue(
                content.contains("M 50.0,50.0 L 200.0,50.0 L 200.0,150.0 L 50.0,150.0 Z"));
    }

    @Test
    public void testAreaWithHoleExportsAsEvenOddPathWithTwoSubpaths() throws Exception {
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

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertTrue(content.contains("fill-rule=\"evenodd\""));
        // outline subpath followed by the hole subpath, both closed
        Assert.assertTrue(content.contains("M 0.0,0.0 L 200.0,0.0 L 200.0,200.0 L 0.0,200.0 Z"));
        Assert.assertTrue(
                content.contains("M 50.0,50.0 L 150.0,50.0 L 150.0,150.0 L 50.0,150.0 Z"));
    }

    @Test
    public void testOnePatternPerAreaTypeAndColour() throws Exception {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        survey.getPlanSketch()
                .setAreaDetails(
                        Arrays.asList(
                                rectangleArea(1, AreaType.SAND, Colour.BLACK),
                                rectangleArea(10, AreaType.SAND, Colour.BLACK),
                                rectangleArea(20, AreaType.SAND, Colour.BROWN),
                                rectangleArea(30, AreaType.BLOCKS, Colour.BLACK)));

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertEquals(3, countOccurrences(content, "<pattern"));
        Assert.assertEquals(2, countOccurrences(content, "fill=\"url(#area-sand-black)\""));
        Assert.assertEquals(1, countOccurrences(content, "fill=\"url(#area-sand-brown)\""));
        Assert.assertEquals(1, countOccurrences(content, "fill=\"url(#area-blocks-black)\""));
    }

    @Test
    public void testPatternsAreDrawnFromTheAreaTypesFill() throws Exception {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        survey.getPlanSketch()
                .setAreaDetails(
                        Arrays.asList(
                                rectangleArea(1, AreaType.SAND, Colour.BLACK),
                                rectangleArea(10, AreaType.DEBRIS, Colour.BLACK)));

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        // sand is all dots and debris all polygons, so their marks show up as those elements
        String sand = getPattern(content, "area-sand-black");
        Assert.assertEquals(
                AreaType.SAND.getFill().getMarks().size(), countOccurrences(sand, "<circle"));
        String debris = getPattern(content, "area-debris-black");
        Assert.assertEquals(
                AreaType.DEBRIS.getFill().getMarks().size(), countOccurrences(debris, "<polygon"));
    }

    @Test
    public void testNoAreasMeansNoFillPatterns() throws Exception {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertFalse(content.contains("<pattern"));
    }
}

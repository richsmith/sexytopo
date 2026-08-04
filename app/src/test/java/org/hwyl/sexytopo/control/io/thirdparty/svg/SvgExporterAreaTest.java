package org.hwyl.sexytopo.control.io.thirdparty.svg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
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
        List<Coord2D> polygon = new ArrayList<>();
        polygon.add(new Coord2D(1, 1));
        polygon.add(new Coord2D(4, 1));
        polygon.add(new Coord2D(4, 3));
        polygon.add(new Coord2D(1, 3));
        List<AreaDetail> areaDetails = new ArrayList<>();
        areaDetails.add(new AreaDetail(polygon, AreaType.WATER, Colour.BLUE));
        sketch.setAreaDetails(areaDetails);
    }

    @Test
    public void testAreaExportsAsHatchedPath() throws Exception {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        addWaterArea(survey.getPlanSketch());

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertTrue(content.contains("<pattern"));
        Assert.assertTrue(content.contains("id=\"area-hatch-blue\""));
        Assert.assertTrue(content.contains("fill=\"url(#area-hatch-blue)\""));
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
    public void testNoAreasMeansNoHatchPatterns() throws Exception {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertFalse(content.contains("area-hatch"));
    }
}

package org.hwyl.sexytopo.control.io.thirdparty.svg;

import java.util.ArrayList;
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
    public void testAreaExportsAsHatchedPolygon() throws Exception {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        addWaterArea(survey.getPlanSketch());

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertTrue(content.contains("<pattern"));
        Assert.assertTrue(content.contains("id=\"area-hatch-blue\""));
        Assert.assertTrue(content.contains("<polygon"));
        Assert.assertTrue(content.contains("fill=\"url(#area-hatch-blue)\""));
        // 4 vertices at SCALE 50: (50,50) (200,50) (200,150) (50,150)
        Assert.assertTrue(content.contains("50.0,50.0 200.0,50.0 200.0,150.0 50.0,150.0"));
    }

    @Test
    public void testNoAreasMeansNoHatchPatterns() throws Exception {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertFalse(content.contains("area-hatch"));
    }
}

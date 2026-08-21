package org.hwyl.sexytopo.control.io.thirdparty.svg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.LineType;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.testutils.BasicTestSurveyCreator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SvgExporterLineTest {

    @Before
    public void setUp() {
        GeneralPreferences.initialise(RuntimeEnvironment.getApplication());
    }

    private static void addPath(Survey survey, LineType lineType) {
        List<Coord2D> points = Arrays.asList(new Coord2D(1, 1), new Coord2D(4, 2));
        PathDetail pathDetail = new PathDetail(new ArrayList<>(points), Colour.BLACK, lineType);
        List<PathDetail> pathDetails = new ArrayList<>(survey.getPlanSketch().getPathDetails());
        pathDetails.add(pathDetail);
        survey.getPlanSketch().setPathDetails(pathDetails);
    }

    @Test
    public void testWallExportsWithClassAndBolderStroke() throws Exception {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        addPath(survey, LineType.WALL);

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertTrue(content.contains("class=\"wall\""));
        int baseWidth = GeneralPreferences.getExportSvgStrokeWidth();
        int wallWidth = Math.round(baseWidth * LineType.WALL.getStrokeWidthFactor());
        Assert.assertTrue(content.contains("stroke-width=\"" + wallWidth + "\""));
    }

    @Test
    public void testPresumedWallExportsDashed() throws Exception {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        addPath(survey, LineType.PRESUMED_WALL);

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertTrue(content.contains("class=\"presumed-wall\""));
        Assert.assertTrue(content.contains("stroke-dasharray"));
    }

    @Test
    public void testGeneralPathHasNoClassOrDash() throws Exception {
        Survey survey = BasicTestSurveyCreator.createStraightNorth();
        addPath(survey, LineType.SKETCH);

        String content = new SvgExporter().getContent(survey, Projection2D.PLAN);

        Assert.assertFalse(content.contains("class=\"general\""));
        Assert.assertFalse(content.contains("stroke-dasharray"));
    }
}

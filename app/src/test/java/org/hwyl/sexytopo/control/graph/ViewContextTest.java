package org.hwyl.sexytopo.control.graph;

import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.sketch.CrossSectionDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.junit.Assert;
import org.junit.Test;

public class ViewContextTest {

    @Test
    public void testPlanContextUsesThePlanSketch() {
        Survey survey = new Survey();
        Assert.assertSame(survey.getPlanSketch(), ViewContext.PLAN.getSketch(survey));
    }

    @Test
    public void testExtendedElevationContextUsesTheElevationSketch() {
        Survey survey = new Survey();
        Assert.assertSame(
                survey.getElevationSketch(), ViewContext.EXTENDED_ELEVATION.getSketch(survey));
    }

    @Test
    public void testContextsWithoutASketchOfTheirOwnReturnNull() {
        Survey survey = new Survey();
        Assert.assertNull(ViewContext.TABLE.getSketch(survey));
        Assert.assertNull(ViewContext.ELEVATION.getSketch(survey));
        Assert.assertNull(ViewContext.CROSS_SECTION.getSketch(survey));
        Assert.assertNull(ViewContext.THREE_D.getSketch(survey));
    }

    @Test
    public void testCrossSectionLookupIsPerContext() {
        Survey survey = new Survey();
        Station station = survey.getOrigin();
        CrossSectionDetail detail =
                new CrossSectionDetail(new CrossSection(station, 0f), new Coord2D(1, 2));
        survey.getPlanSketch().addCrossSection(detail);

        Sketch planSketch = ViewContext.PLAN.getSketch(survey);
        Sketch elevationSketch = ViewContext.EXTENDED_ELEVATION.getSketch(survey);

        Assert.assertSame(detail, planSketch.getCrossSectionDetail(station));
        Assert.assertNull(elevationSketch.getCrossSectionDetail(station));
    }

    @Test
    public void testOnlyThePlanCanRotateCrossSections() {
        Assert.assertTrue(ViewContext.PLAN.canRotateCrossSections());

        Assert.assertFalse(ViewContext.EXTENDED_ELEVATION.canRotateCrossSections());
        Assert.assertFalse(ViewContext.ELEVATION.canRotateCrossSections());
        Assert.assertFalse(ViewContext.TABLE.canRotateCrossSections());
        Assert.assertFalse(ViewContext.CROSS_SECTION.canRotateCrossSections());
        Assert.assertFalse(ViewContext.THREE_D.canRotateCrossSections());
    }
}

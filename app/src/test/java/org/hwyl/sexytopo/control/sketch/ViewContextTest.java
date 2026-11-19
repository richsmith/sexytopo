package org.hwyl.sexytopo.control.sketch;

import org.hwyl.sexytopo.model.geometry.Projection2D;
import org.junit.Assert;
import org.junit.Test;

public class ViewContextTest {

    @Test
    public void testEachProjectionHasItsViewContext() {
        Assert.assertEquals(ViewContext.PLAN, ViewContext.forProjection(Projection2D.PLAN));
        Assert.assertEquals(
                ViewContext.ELEVATION, ViewContext.forProjection(Projection2D.ELEVATION_NS));
        Assert.assertEquals(
                ViewContext.ELEVATION, ViewContext.forProjection(Projection2D.ELEVATION_EW));
        Assert.assertEquals(
                ViewContext.EXTENDED_ELEVATION,
                ViewContext.forProjection(Projection2D.EXTENDED_ELEVATION));
        Assert.assertEquals(
                ViewContext.CROSS_SECTION, ViewContext.forProjection(Projection2D.CROSS_SECTION));
    }

    @Test
    public void testNoProjectionMeansThePlan() {
        Assert.assertEquals(ViewContext.PLAN, ViewContext.forProjection(null));
    }

    @Test
    public void testOnlyTheExtendedElevationCanCreateHorizontalCrossSections() {
        Assert.assertTrue(ViewContext.EXTENDED_ELEVATION.canCreateHorizontalCrossSection());

        Assert.assertFalse(ViewContext.PLAN.canCreateHorizontalCrossSection());
        Assert.assertFalse(ViewContext.ELEVATION.canCreateHorizontalCrossSection());
        Assert.assertFalse(ViewContext.TABLE.canCreateHorizontalCrossSection());
        Assert.assertFalse(ViewContext.CROSS_SECTION.canCreateHorizontalCrossSection());
        Assert.assertFalse(ViewContext.THREE_D.canCreateHorizontalCrossSection());
    }
}

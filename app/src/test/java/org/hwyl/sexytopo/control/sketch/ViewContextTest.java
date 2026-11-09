package org.hwyl.sexytopo.control.sketch;

import org.junit.Assert;
import org.junit.Test;

public class ViewContextTest {

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

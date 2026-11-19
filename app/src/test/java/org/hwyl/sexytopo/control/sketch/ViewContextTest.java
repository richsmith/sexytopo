package org.hwyl.sexytopo.control.sketch;

import java.util.Arrays;
import org.hwyl.sexytopo.model.geometry.Projection2D;
import org.junit.Assert;
import org.junit.Test;

public class ViewContextTest {

    @Test
    public void testEveryProjectionIsCoveredByExactlyOneViewContext() {
        for (Projection2D projection : Projection2D.values()) {
            long covering =
                    Arrays.stream(ViewContext.values())
                            .filter(viewContext -> viewContext.covers(projection))
                            .count();
            Assert.assertEquals(projection + " should have one view context", 1, covering);
        }
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

package org.hwyl.sexytopo.control.graph;

import org.junit.Assert;
import org.junit.Test;

public class CrossSectionIndicatorTest {

    @Test
    public void testOnTheElevationTheArrowheadIsAtTheTopFacingEitherWay() {
        for (boolean facingRight : new boolean[] {true, false}) {
            CrossSectionIndicator indicator =
                    CrossSectionIndicator.onElevation(100, 50, 20, facingRight);

            float top = Math.min(indicator.getStartY(), indicator.getEndY());
            String message = facingRight ? "right" : "left";
            Assert.assertEquals(message, top, indicator.getArrowOuterY(), 1e-4f);
            Assert.assertEquals(
                    message, facingRight, indicator.getArrowTipX() > indicator.getArrowOuterX());
        }
    }
}

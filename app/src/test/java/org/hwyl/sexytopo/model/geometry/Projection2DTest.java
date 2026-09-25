package org.hwyl.sexytopo.model.geometry;

import org.junit.Assert;
import org.junit.Test;

public class Projection2DTest {

    @Test
    public void testFromAbbreviationRoundTripsEveryProjection() {
        for (Projection2D projection : Projection2D.values()) {
            Assert.assertSame(
                    projection, Projection2D.fromAbbreviation(projection.getAbbreviation()));
        }
    }

    @Test
    public void testFromAbbreviationFindsExtendedElevation() {
        Assert.assertSame(
                Projection2D.EXTENDED_ELEVATION,
                Projection2D.fromAbbreviation(Projection2D.EXTENDED_ELEVATION.getAbbreviation()));
    }

    @Test
    public void testFromAbbreviationDefaultsToPlanWhenNull() {
        Assert.assertSame(Projection2D.PLAN, Projection2D.fromAbbreviation(null));
    }

    @Test
    public void testFromAbbreviationDefaultsToPlanWhenUnknown() {
        Assert.assertSame(Projection2D.PLAN, Projection2D.fromAbbreviation("not-a-projection"));
        Assert.assertSame(Projection2D.PLAN, Projection2D.fromAbbreviation(""));
    }
}

package org.hwyl.sexytopo.model.geometry;

import org.junit.Assert;
import org.junit.Test;

public class Coord2DTest {

    @Test
    public void testBasicEquality() {
        Coord2D origin = Coord2D.ORIGIN;
        Coord2D zeroes = new Coord2D(0, 0);
        Assert.assertEquals(origin, zeroes);
    }

    @Test
    public void testRotateTurnsClockwiseOnAPageWithYDownwards() {
        // north (up the page) turned a quarter clockwise points east (right)
        Assert.assertEquals(new Coord2D(1, 0), new Coord2D(0, -1).rotate(90));
        Assert.assertEquals(new Coord2D(0, 1), new Coord2D(1, 0).rotate(90));
    }

    @Test
    public void testRotateByZeroIsUnchanged() {
        Coord2D point = new Coord2D(3, -4);
        Assert.assertSame(point, point.rotate(0));
    }

    @Test
    public void testRotateKeepsDistanceFromOrigin() {
        Coord2D point = new Coord2D(3, -4);
        Assert.assertEquals(5, point.rotate(37).mag(), 1e-4f);
    }
}

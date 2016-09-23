package org.hwyl.sexytopo.model.graph;

import junit.framework.Assert;

import org.junit.Test;


public class Coord2DTest {

    @Test
    public void testBasicEquality() {
        Coord2D origin = Coord2D.ORIGIN;
        Coord2D zeroes = new Coord2D(0, 0);
        Assert.assertEquals(origin, zeroes);
    }

}

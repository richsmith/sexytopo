package org.hwyl.sexytopo.model.graph;

import junit.framework.Assert;

import org.junit.Test;


public class Coord3DTest {

    @Test
    public void testBasicEquality() {
        Coord3D origin = Coord3D.ORIGIN;
        Coord3D zeroes = new Coord3D(0, 0, 0);
        Assert.assertEquals(origin, zeroes);
    }

}

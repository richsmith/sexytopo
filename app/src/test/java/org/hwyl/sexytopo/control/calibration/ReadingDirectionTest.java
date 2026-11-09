package org.hwyl.sexytopo.control.calibration;

import org.junit.Assert;
import org.junit.Test;

public class ReadingDirectionTest {

    private static final double DIP = Math.toRadians(66);
    private static final double DELTA = 0.01;

    @Test
    public void testLevelPointingNorth() {
        Vector g = new Vector(0, 0, 1);
        Vector m = new Vector((float) Math.cos(DIP), 0, (float) Math.sin(DIP));
        ReadingDirection direction = ReadingDirection.fromVectors(g, m);
        Assert.assertEquals(0, direction.inclination, DELTA);
        Assert.assertEquals(0, direction.azimuth, DELTA);
        Assert.assertEquals(0, direction.roll, DELTA);
    }

    @Test
    public void testLevelPointingEast() {
        Vector g = new Vector(0, 0, 1);
        Vector m = new Vector(0, (float) -Math.cos(DIP), (float) Math.sin(DIP));
        ReadingDirection direction = ReadingDirection.fromVectors(g, m);
        Assert.assertEquals(0, direction.inclination, DELTA);
        Assert.assertEquals(90, direction.azimuth, DELTA);

        Vector laser = direction.toVector();
        Assert.assertEquals(0, laser.x, DELTA);
        Assert.assertEquals(1, laser.y, DELTA);
        Assert.assertEquals(0, laser.z, DELTA);
    }

    @Test
    public void testPointingUp() {
        Vector g = new Vector(-1, 0, 0);
        Vector m = new Vector((float) -Math.sin(DIP), 0, (float) Math.cos(DIP));
        Assert.assertEquals(90, ReadingDirection.fromVectors(g, m).inclination, DELTA);
    }

    @Test
    public void testRolledOntoItsSide() {
        Vector g = new Vector(0, 1, 0);
        Vector m = new Vector((float) Math.cos(DIP), (float) Math.sin(DIP), 0);
        ReadingDirection direction = ReadingDirection.fromVectors(g, m);
        Assert.assertEquals(0, direction.inclination, DELTA);
        Assert.assertEquals(0, direction.azimuth, DELTA);
        Assert.assertEquals(90, direction.roll, DELTA);
    }

    @Test
    public void testLevelFaceUpIsHorizontalFaceUp() {
        Vector g = new Vector(0, 0, 1);
        Vector m = new Vector((float) Math.cos(DIP), 0, (float) Math.sin(DIP));
        ReadingDirection direction = ReadingDirection.fromVectors(g, m);
        Assert.assertEquals(ReadingDirection.Pointing.HORIZONTAL, direction.getPointing());
        Assert.assertEquals(ReadingDirection.Face.UP, direction.getFace());
    }

    @Test
    public void testCornerUpOnItsSide() {
        double inclination = Math.toRadians(35);
        // pointing north and up, rolled so gravity lies along y
        Vector g = new Vector((float) -Math.sin(inclination), (float) Math.cos(inclination), 0);
        Vector m =
                new Vector(
                        (float) Math.cos(DIP + inclination),
                        (float) Math.sin(DIP + inclination),
                        0);
        ReadingDirection direction = ReadingDirection.fromVectors(g, m);
        Assert.assertEquals(35, direction.inclination, DELTA);
        Assert.assertEquals(ReadingDirection.Pointing.CORNER_UP, direction.getPointing());
        Assert.assertEquals(ReadingDirection.Face.SIDE, direction.getFace());
    }

    @Test
    public void testPointingUpAndDown() {
        Vector m = new Vector((float) -Math.sin(DIP), 0, (float) Math.cos(DIP));
        Assert.assertEquals(
                ReadingDirection.Pointing.UP,
                ReadingDirection.fromVectors(new Vector(-1, 0, 0), m).getPointing());
        Assert.assertEquals(
                ReadingDirection.Pointing.DOWN,
                ReadingDirection.fromVectors(new Vector(1, 0, 0), m).getPointing());
    }
}

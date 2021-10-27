package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.model.graph.Coord2D;
import org.junit.Assert;
import org.junit.Test;


public class PathDetailTest {

    @Test
    public void testIntersectsRectangleReturnsTrueForIntersectingRectangle() {
        PathDetail pathDetail = new PathDetail(Coord2D.ORIGIN, Colour.BLACK);
        Assert.assertTrue(
                pathDetail.intersectsRectangle(new Coord2D(0, 0), new Coord2D(1, 1)));
    }

    @Test
    public void testIntersectsRectangleReturnsFalseForNonIntersectingRectangle() {
        PathDetail pathDetail = new PathDetail(Coord2D.ORIGIN, Colour.BLACK);
        Assert.assertFalse(
                pathDetail.intersectsRectangle(new Coord2D(1, 1), new Coord2D(2, 2)));
    }

    @Test
    public void testIntersectsRectangleReturnsTrueForRectangleThatEntersBoundingBox() {
        PathDetail pathDetail = new PathDetail(Coord2D.ORIGIN, Colour.BLACK);
        pathDetail.lineTo(new Coord2D(1.5f, 1.5f));
        Assert.assertTrue(
                pathDetail.intersectsRectangle(new Coord2D(1, 1), new Coord2D(2, 2)));
    }

    @Test
    public void testIntersectsRectangleReturnsFalseForRectangleOutsideBoundingBox() {
        PathDetail pathDetail = new PathDetail(Coord2D.ORIGIN, Colour.BLACK);
        pathDetail.lineTo(new Coord2D(1.5f, 1.5f));
        Assert.assertFalse(
                pathDetail.intersectsRectangle(new Coord2D(2, 2), new Coord2D(3, 3)));
    }

}

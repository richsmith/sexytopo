package org.hwyl.sexytopo.model.sketch;

import java.util.List;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.junit.Assert;
import org.junit.Test;

public class PathDetailTest {

    @Test
    public void testIntersectsRectangleReturnsTrueForIntersectingRectangle() {
        PathDetail pathDetail = new PathDetail(Coord2D.ORIGIN, Colour.BLACK);
        Assert.assertTrue(pathDetail.intersectsRectangle(new Coord2D(0, 0), new Coord2D(1, 1)));
    }

    @Test
    public void testIntersectsRectangleReturnsFalseForNonIntersectingRectangle() {
        PathDetail pathDetail = new PathDetail(Coord2D.ORIGIN, Colour.BLACK);
        Assert.assertFalse(pathDetail.intersectsRectangle(new Coord2D(1, 1), new Coord2D(2, 2)));
    }

    @Test
    public void testIntersectsRectangleReturnsTrueForRectangleThatEntersBoundingBox() {
        PathDetail pathDetail = new PathDetail(Coord2D.ORIGIN, Colour.BLACK);
        pathDetail.lineTo(new Coord2D(1.5f, 1.5f));
        Assert.assertTrue(pathDetail.intersectsRectangle(new Coord2D(1, 1), new Coord2D(2, 2)));
    }

    @Test
    public void testIntersectsRectangleReturnsFalseForRectangleOutsideBoundingBox() {
        PathDetail pathDetail = new PathDetail(Coord2D.ORIGIN, Colour.BLACK);
        pathDetail.lineTo(new Coord2D(1.5f, 1.5f));
        Assert.assertFalse(pathDetail.intersectsRectangle(new Coord2D(2, 2), new Coord2D(3, 3)));
    }

    @Test
    public void testSnappedEndIsBentSmoothlyRoundToTheJoin() {
        PathDetail pathDetail = new PathDetail(new Coord2D(0, 0.5f), Colour.BLACK);
        for (int x = 1; x <= 10; x++) {
            pathDetail.lineTo(new Coord2D(x, 0.5f));
        }

        pathDetail.snapEndTo(new Coord2D(10, 0), 1);

        List<Coord2D> path = pathDetail.getPath();
        Assert.assertEquals(11, path.size());
        Assert.assertEquals(new Coord2D(10, 0), pathDetail.getEnd());
        // beyond three times the snap distance back, the line is as drawn
        Assert.assertEquals(new Coord2D(7, 0.5f), path.get(7));
        // and closer in it eases down towards the join
        Assert.assertTrue(path.get(8).y < 0.5f);
        Assert.assertTrue(path.get(9).y < path.get(8).y);
        Assert.assertTrue(path.get(9).y > 0);
    }

    @Test
    public void testSnappingOneEndLeavesTheOtherWhereItWas() {
        PathDetail pathDetail = new PathDetail(Coord2D.ORIGIN, Colour.BLACK);
        pathDetail.lineTo(new Coord2D(1, 0));
        pathDetail.lineTo(new Coord2D(2, 0.5f));

        pathDetail.snapStartTo(new Coord2D(0, 0.5f), 1);

        Assert.assertEquals(new Coord2D(0, 0.5f), pathDetail.getStart());
        Assert.assertEquals(new Coord2D(2, 0.5f), pathDetail.getEnd());
    }
}

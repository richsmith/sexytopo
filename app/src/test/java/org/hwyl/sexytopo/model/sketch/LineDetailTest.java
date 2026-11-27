package org.hwyl.sexytopo.model.sketch;

import java.util.List;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.junit.Assert;
import org.junit.Test;

public class LineDetailTest {

    @Test
    public void testIntersectsRectangleReturnsTrueForIntersectingRectangle() {
        LineDetail lineDetail = new LineDetail(Coord2D.ORIGIN, Colour.BLACK);
        Assert.assertTrue(lineDetail.intersectsRectangle(new Coord2D(0, 0), new Coord2D(1, 1)));
    }

    @Test
    public void testIntersectsRectangleReturnsFalseForNonIntersectingRectangle() {
        LineDetail lineDetail = new LineDetail(Coord2D.ORIGIN, Colour.BLACK);
        Assert.assertFalse(lineDetail.intersectsRectangle(new Coord2D(1, 1), new Coord2D(2, 2)));
    }

    @Test
    public void testIntersectsRectangleReturnsTrueForRectangleThatEntersBoundingBox() {
        LineDetail lineDetail = new LineDetail(Coord2D.ORIGIN, Colour.BLACK);
        lineDetail.lineTo(new Coord2D(1.5f, 1.5f));
        Assert.assertTrue(lineDetail.intersectsRectangle(new Coord2D(1, 1), new Coord2D(2, 2)));
    }

    @Test
    public void testIntersectsRectangleReturnsFalseForRectangleOutsideBoundingBox() {
        LineDetail lineDetail = new LineDetail(Coord2D.ORIGIN, Colour.BLACK);
        lineDetail.lineTo(new Coord2D(1.5f, 1.5f));
        Assert.assertFalse(lineDetail.intersectsRectangle(new Coord2D(2, 2), new Coord2D(3, 3)));
    }

    @Test
    public void testSnappedEndIsBentSmoothlyRoundToTheJoin() {
        LineDetail lineDetail = new LineDetail(new Coord2D(0, 0.5f), Colour.BLACK);
        for (int x = 1; x <= 10; x++) {
            lineDetail.lineTo(new Coord2D(x, 0.5f));
        }

        lineDetail.snapEndTo(new Coord2D(10, 0), 1);

        List<Coord2D> path = lineDetail.getPath();
        Assert.assertEquals(11, path.size());
        Assert.assertEquals(new Coord2D(10, 0), lineDetail.getEnd());
        // beyond three times the snap distance back, the line is as drawn
        Assert.assertEquals(new Coord2D(7, 0.5f), path.get(7));
        // and closer in it eases down towards the join
        Assert.assertTrue(path.get(8).y < 0.5f);
        Assert.assertTrue(path.get(9).y < path.get(8).y);
        Assert.assertTrue(path.get(9).y > 0);
    }

    @Test
    public void testSnappingOneEndLeavesTheOtherWhereItWas() {
        LineDetail lineDetail = new LineDetail(Coord2D.ORIGIN, Colour.BLACK);
        lineDetail.lineTo(new Coord2D(1, 0));
        lineDetail.lineTo(new Coord2D(2, 0.5f));

        lineDetail.snapStartTo(new Coord2D(0, 0.5f), 1);

        Assert.assertEquals(new Coord2D(0, 0.5f), lineDetail.getStart());
        Assert.assertEquals(new Coord2D(2, 0.5f), lineDetail.getEnd());
    }
}

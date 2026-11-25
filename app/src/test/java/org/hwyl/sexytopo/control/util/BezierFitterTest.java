package org.hwyl.sexytopo.control.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.junit.Assert;
import org.junit.Test;

public class BezierFitterTest {

    private static final float TOLERANCE = 0.05f;

    @Test
    public void testFewerThanTwoDistinctPointsGiveNoCurves() {
        Assert.assertTrue(BezierFitter.fit(new ArrayList<>(), TOLERANCE).isEmpty());
        Assert.assertTrue(
                BezierFitter.fit(Arrays.asList(new Coord2D(1, 1), new Coord2D(1, 1)), TOLERANCE)
                        .isEmpty());
    }

    @Test
    public void testStraightLineIsOneCurve() {
        List<Coord2D> points =
                Arrays.asList(
                        new Coord2D(0, 0), new Coord2D(1, 1), new Coord2D(2, 2), new Coord2D(5, 5));
        List<BezierFitter.CubicBezier> curves = BezierFitter.fit(points, TOLERANCE);
        Assert.assertEquals(1, curves.size());
        assertFits(points, curves);
    }

    @Test
    public void testArcFitsWithinToleranceInFewCurves() {
        List<Coord2D> points = new ArrayList<>();
        for (int i = 0; i <= 40; i++) {
            double angle = Math.PI * i / 40;
            points.add(new Coord2D((float) (10 * Math.cos(angle)), (float) (10 * Math.sin(angle))));
        }
        List<BezierFitter.CubicBezier> curves = BezierFitter.fit(points, TOLERANCE);
        Assert.assertTrue("got " + curves.size() + " curves", curves.size() <= 4);
        assertFits(points, curves);
    }

    @Test
    public void testSharpCornerIsKept() {
        Coord2D corner = new Coord2D(4, 0);
        List<Coord2D> points =
                Arrays.asList(
                        new Coord2D(0, 0),
                        new Coord2D(2, 0),
                        corner,
                        new Coord2D(4, 2),
                        new Coord2D(4, 4));
        List<BezierFitter.CubicBezier> curves = BezierFitter.fit(points, TOLERANCE);
        Assert.assertTrue(curves.stream().anyMatch(curve -> curve.end.equals(corner)));
        assertFits(points, curves);
    }

    @Test
    public void testRepeatedPointsAreIgnored() {
        List<Coord2D> points =
                Arrays.asList(
                        new Coord2D(0, 0),
                        new Coord2D(0, 0),
                        new Coord2D(1, 0.2f),
                        new Coord2D(1, 0.2f),
                        new Coord2D(2, 0));
        List<BezierFitter.CubicBezier> curves = BezierFitter.fit(points, TOLERANCE);
        for (BezierFitter.CubicBezier curve : curves) {
            Assert.assertFalse(Float.isNaN(curve.control1.x));
            Assert.assertFalse(Float.isNaN(curve.control2.x));
        }
        assertFits(points, curves);
    }

    @Test
    public void testSmoothClosedLoopHasNoKinkWhereItJoins() {
        List<Coord2D> points = new ArrayList<>();
        for (int i = 0; i <= 24; i++) {
            double angle = 2 * Math.PI * i / 24;
            points.add(new Coord2D((float) (3 * Math.cos(angle)), (float) (3 * Math.sin(angle))));
        }
        List<BezierFitter.CubicBezier> curves = BezierFitter.fit(points, TOLERANCE);
        assertFits(points, curves);

        BezierFitter.CubicBezier first = curves.get(0);
        BezierFitter.CubicBezier last = curves.get(curves.size() - 1);
        Coord2D outgoing = first.control1.minus(first.start).normalise();
        Coord2D incoming = last.end.minus(last.control2).normalise();
        Assert.assertEquals(outgoing.x, incoming.x, 0.001);
        Assert.assertEquals(outgoing.y, incoming.y, 0.001);
    }

    @Test
    public void testCurveStaysCloseToTheLineBetweenSparsePoints() {
        // a wall as left by stroke simplification: long straight runs with gentle turns
        List<Coord2D> points =
                Arrays.asList(
                        new Coord2D(0, 0),
                        new Coord2D(0.4f, -2.5f),
                        new Coord2D(0.5f, -3),
                        new Coord2D(1.6f, -5.5f),
                        new Coord2D(2, -6),
                        new Coord2D(2.3f, -8.5f),
                        new Coord2D(3.5f, -10));
        List<BezierFitter.CubicBezier> curves = BezierFitter.fit(points, TOLERANCE);
        assertFits(points, curves);

        float worst = 0;
        for (BezierFitter.CubicBezier curve : curves) {
            for (int i = 0; i <= 100; i++) {
                worst = Math.max(worst, distanceToPolyline(curve.pointAt(i / 100f), points));
            }
        }
        Assert.assertTrue("strays " + worst + " from the line", worst <= TOLERANCE * 1.01);
    }

    private static float distanceToPolyline(Coord2D point, List<Coord2D> polyline) {
        float nearest = Float.MAX_VALUE;
        for (int i = 1; i < polyline.size(); i++) {
            nearest =
                    Math.min(
                            nearest,
                            Space2DUtils.getDistanceFromLine(
                                    point, polyline.get(i - 1), polyline.get(i)));
        }
        return nearest;
    }

    /** The chain is continuous, runs end to end, and passes within tolerance of every point. */
    private static void assertFits(List<Coord2D> points, List<BezierFitter.CubicBezier> curves) {
        Assert.assertFalse(curves.isEmpty());
        Assert.assertEquals(points.get(0), curves.get(0).start);
        Assert.assertEquals(points.get(points.size() - 1), curves.get(curves.size() - 1).end);
        for (int i = 1; i < curves.size(); i++) {
            Assert.assertEquals(curves.get(i - 1).end, curves.get(i).start);
        }
        for (Coord2D point : points) {
            Assert.assertTrue(
                    "point " + point + " is off the curve",
                    distanceToChain(point, curves) <= TOLERANCE * 1.01);
        }
    }

    private static float distanceToChain(Coord2D point, List<BezierFitter.CubicBezier> curves) {
        float nearest = Float.MAX_VALUE;
        for (BezierFitter.CubicBezier curve : curves) {
            for (int i = 0; i <= 1000; i++) {
                nearest = Math.min(nearest, curve.pointAt(i / 1000f).minus(point).mag());
            }
        }
        return nearest;
    }
}

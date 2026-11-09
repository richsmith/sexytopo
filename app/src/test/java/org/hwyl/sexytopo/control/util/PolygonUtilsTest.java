package org.hwyl.sexytopo.control.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the contour-grouping logic that decides which contours coming back from a boolean op
 * are holes.
 *
 * <p>The boolean operations themselves (union, subtract, overlap) are not covered here: they run on
 * android.graphics.Path, whose Robolectric shadow neither performs real boolean geometry nor
 * iterates multiple contours in PathMeasure, so under Robolectric they return results that say
 * nothing about the real behaviour. Grouping is where the hole logic lives, and it is pure
 * geometry, so it is tested directly.
 */
public class PolygonUtilsTest {

    /** An axis-aligned rectangle, as a closed contour. */
    private static List<Coord2D> rectangle(float left, float top, float width, float height) {
        return new ArrayList<>(
                Arrays.asList(
                        new Coord2D(left, top),
                        new Coord2D(left + width, top),
                        new Coord2D(left + width, top + height),
                        new Coord2D(left, top + height)));
    }

    private static Coord2D point(float x, float y) {
        return new Coord2D(x, y);
    }

    @Test
    public void testTrimOvershootCutsStrokeAtTheCrossing() {
        // round a 10x10 square from (2,0), carrying on past the start and out below it
        List<Coord2D> stroke =
                Arrays.asList(
                        point(2, 0),
                        point(10, 0),
                        point(10, 10),
                        point(0, 10),
                        point(0, 0),
                        point(3, 1),
                        point(5, -1));

        List<Coord2D> trimmed = PolygonUtils.trimOvershoot(stroke);

        // the overshoot (3,1)-(5,-1) crosses the opening segment at (4,0); everything outside
        // that is cut off
        Assert.assertEquals(6, trimmed.size());
        Assert.assertEquals(4, trimmed.get(0).x, 1e-4);
        Assert.assertEquals(0, trimmed.get(0).y, 1e-4);
        Assert.assertEquals(point(10, 0), trimmed.get(1));
        Assert.assertEquals(point(3, 1), trimmed.get(5));
    }

    @Test
    public void testTrimOvershootDropsLeadIn() {
        // starts outside the shape, so the start is a lead-in the closing stretch cuts across
        List<Coord2D> stroke =
                Arrays.asList(
                        point(5, -2),
                        point(5, 1),
                        point(10, 1),
                        point(10, 10),
                        point(0, 10),
                        point(0, 0),
                        point(8, 0));

        List<Coord2D> trimmed = PolygonUtils.trimOvershoot(stroke);

        // the closing segment (0,0)-(8,0) crosses the lead-in (5,-2)-(5,1) at (5,0)
        Assert.assertEquals(point(5, 0), trimmed.get(0));
        Assert.assertEquals(point(5, 1), trimmed.get(1));
        Assert.assertEquals(point(0, 0), trimmed.get(trimmed.size() - 1));
    }

    @Test
    public void testTrimOvershootLeavesAStrokeThatFallsShortAlone() {
        List<Coord2D> stroke =
                Arrays.asList(point(1, 0), point(10, 0), point(10, 10), point(0, 10), point(0, 1));

        Assert.assertSame(stroke, PolygonUtils.trimOvershoot(stroke));
    }

    @Test
    public void testTrimOvershootIgnoresCrossingsAwayFromTheEnds() {
        // a figure of eight (a lemniscate) started at one end, so it crosses itself a quarter
        // and three quarters of the way along; stops just short of closing
        List<Coord2D> stroke = new ArrayList<>();
        for (int i = 0; i < 95; i++) {
            double t = 2 * Math.PI * i / 100;
            double scale = 10 / (1 + Math.sin(t) * Math.sin(t));
            stroke.add(
                    point(
                            (float) (scale * Math.cos(t)),
                            (float) (scale * Math.sin(t) * Math.cos(t))));
        }

        Assert.assertSame(stroke, PolygonUtils.trimOvershoot(stroke));
    }

    @Test
    public void testAreaOfRectangle() {
        Assert.assertEquals(12, PolygonUtils.area(rectangle(1, 2, 3, 4)), 1e-6);
    }

    @Test
    public void testAreaIgnoresWhichWayTheContourRuns() {
        List<Coord2D> reversed = rectangle(1, 2, 3, 4);
        Collections.reverse(reversed);
        Assert.assertEquals(12, PolygonUtils.area(reversed), 1e-6);
    }

    @Test
    public void testRegionAreaExcludesHoles() {
        PolygonUtils.Region region =
                new PolygonUtils.Region(
                        rectangle(0, 0, 10, 10), Collections.singletonList(rectangle(2, 2, 6, 6)));
        Assert.assertEquals(64, region.getArea(), 1e-6);
    }

    @Test
    public void testCourtyardContourBecomesAHole() {
        // what a union of four walls of water produces: an outer square plus the courtyard inside
        List<Coord2D> outer = rectangle(0, 0, 10, 10);
        List<Coord2D> courtyard = rectangle(2, 2, 6, 6);

        List<PolygonUtils.Region> regions =
                PolygonUtils.groupIntoRegions(Arrays.asList(outer, courtyard));

        Assert.assertEquals(1, regions.size());
        PolygonUtils.Region ring = regions.get(0);
        Assert.assertSame(outer, ring.getOutline());
        Assert.assertEquals(1, ring.getHoles().size());
        Assert.assertSame(courtyard, ring.getHoles().get(0));
    }

    @Test
    public void testContourOrderDoesNotMatter() {
        // PathMeasure gives no guarantee that the outline comes first
        List<Coord2D> outer = rectangle(0, 0, 10, 10);
        List<Coord2D> courtyard = rectangle(2, 2, 6, 6);

        List<PolygonUtils.Region> regions =
                PolygonUtils.groupIntoRegions(Arrays.asList(courtyard, outer));

        Assert.assertEquals(1, regions.size());
        Assert.assertSame(outer, regions.get(0).getOutline());
        Assert.assertEquals(1, regions.get(0).getHoles().size());
    }

    @Test
    public void testDisjointContoursBecomeSeparateRegions() {
        List<Coord2D> first = rectangle(0, 0, 2, 2);
        List<Coord2D> second = rectangle(10, 10, 2, 2);

        List<PolygonUtils.Region> regions =
                PolygonUtils.groupIntoRegions(Arrays.asList(first, second));

        Assert.assertEquals(2, regions.size());
        Assert.assertTrue(regions.get(0).getHoles().isEmpty());
        Assert.assertTrue(regions.get(1).getHoles().isEmpty());
    }

    @Test
    public void testEachHoleGoesToTheRegionContainingIt() {
        // two separate ring-shaped areas, each with its own courtyard
        List<Coord2D> firstOuter = rectangle(0, 0, 10, 10);
        List<Coord2D> firstHole = rectangle(2, 2, 6, 6);
        List<Coord2D> secondOuter = rectangle(100, 100, 10, 10);
        List<Coord2D> secondHole = rectangle(102, 102, 6, 6);

        List<PolygonUtils.Region> regions =
                PolygonUtils.groupIntoRegions(
                        Arrays.asList(firstHole, secondOuter, firstOuter, secondHole));

        Assert.assertEquals(2, regions.size());
        for (PolygonUtils.Region region : regions) {
            Assert.assertEquals(1, region.getHoles().size());
            // the hole must be the one belonging to this region, not the other
            Coord2D holePoint = region.getHoles().get(0).get(0);
            Assert.assertTrue(PolygonUtils.contains(region.getOutline(), holePoint));
        }
    }

    @Test
    public void testRegionWithSeveralHoles() {
        List<Coord2D> outer = rectangle(0, 0, 20, 20);
        List<Coord2D> holeA = rectangle(2, 2, 4, 4);
        List<Coord2D> holeB = rectangle(12, 12, 4, 4);

        List<PolygonUtils.Region> regions =
                PolygonUtils.groupIntoRegions(Arrays.asList(outer, holeA, holeB));

        Assert.assertEquals(1, regions.size());
        Assert.assertEquals(2, regions.get(0).getHoles().size());
    }

    @Test
    public void testSingleContourHasNoHoles() {
        List<Coord2D> only = rectangle(0, 0, 5, 5);

        List<PolygonUtils.Region> regions =
                PolygonUtils.groupIntoRegions(Collections.singletonList(only));

        Assert.assertEquals(1, regions.size());
        Assert.assertSame(only, regions.get(0).getOutline());
        Assert.assertTrue(regions.get(0).getHoles().isEmpty());
    }

    @Test
    public void testInteriorLoopsAreDroppedWhenKeepingOutlinesOnly() {
        // What normalise does with a self-crossing stroke: group the resolved contours, then keep
        // only the outlines. An accidental loop inside the stroke must not become a hole.
        List<Coord2D> outer = rectangle(0, 0, 10, 10);
        List<Coord2D> accidentalLoop = rectangle(3, 3, 2, 2);

        List<PolygonUtils.Region> grouped =
                PolygonUtils.groupIntoRegions(Arrays.asList(outer, accidentalLoop));
        Assert.assertEquals(1, grouped.size());
        Assert.assertEquals(
                "grouping alone would treat the loop as a hole",
                1,
                grouped.get(0).getHoles().size());

        // keeping outlines only is what discards it
        PolygonUtils.Region outlineOnly = new PolygonUtils.Region(grouped.get(0).getOutline());
        Assert.assertTrue(outlineOnly.getHoles().isEmpty());
        Assert.assertSame(outer, outlineOnly.getOutline());
    }

    @Test
    public void testTwoLobesRemainSeparateRegions() {
        // a figure-8 stroke encloses two lobes; each should survive as its own region
        List<Coord2D> leftLobe = rectangle(0, 0, 4, 4);
        List<Coord2D> rightLobe = rectangle(10, 0, 4, 4);

        List<PolygonUtils.Region> grouped =
                PolygonUtils.groupIntoRegions(Arrays.asList(leftLobe, rightLobe));

        Assert.assertEquals(2, grouped.size());
        for (PolygonUtils.Region region : grouped) {
            Assert.assertTrue(region.getHoles().isEmpty());
        }
    }

    @Test
    public void testContainsDistinguishesInsideFromOutside() {
        List<Coord2D> square = rectangle(0, 0, 10, 10);

        Assert.assertTrue(PolygonUtils.contains(square, new Coord2D(5, 5)));
        Assert.assertFalse(PolygonUtils.contains(square, new Coord2D(15, 5)));
        Assert.assertFalse(PolygonUtils.contains(square, new Coord2D(5, -1)));
    }
}

package org.hwyl.sexytopo.control.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hwyl.sexytopo.model.graph.Coord2D;
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

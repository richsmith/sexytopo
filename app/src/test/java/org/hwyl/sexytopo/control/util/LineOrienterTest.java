package org.hwyl.sexytopo.control.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.geometry.Line;
import org.junit.Assert;
import org.junit.Test;

public class LineOrienterTest {

    // A west-to-east wall with the leg north of it (survey y is down, so north is -y).
    // Cross product of direction (east) with vector to the leg (north/-y) is negative, so the
    // points should be reversed to put the interior on the canonical side.
    @Test
    public void testWallIsReversedWhenInteriorOnWrongSide() {
        List<Coord2D> points =
                new ArrayList<>(
                        Arrays.asList(new Coord2D(0, 0), new Coord2D(5, 0), new Coord2D(10, 0)));
        List<Line<Coord2D>> legs = legs(new Coord2D(0, -3), new Coord2D(10, -3));

        boolean reversed = LineOrienter.orientToInterior(points, legs);

        Assert.assertTrue(reversed);
        Assert.assertEquals(10f, points.get(0).x, 0.0001f);
        Assert.assertEquals(0f, points.get(2).x, 0.0001f);
    }

    @Test
    public void testWallIsLeftAloneWhenAlreadyOriented() {
        List<Coord2D> points =
                new ArrayList<>(
                        Arrays.asList(new Coord2D(10, 0), new Coord2D(5, 0), new Coord2D(0, 0)));
        List<Line<Coord2D>> legs = legs(new Coord2D(0, -3), new Coord2D(10, -3));

        boolean reversed = LineOrienter.orientToInterior(points, legs);

        Assert.assertFalse(reversed);
        Assert.assertEquals(10f, points.get(0).x, 0.0001f);
    }

    // Whichever direction the same wall is drawn in, the result should be identical.
    @Test
    public void testOrientationIsCanonicalRegardlessOfDrawnDirection() {
        List<Coord2D> drawnEast =
                new ArrayList<>(
                        Arrays.asList(new Coord2D(0, 0), new Coord2D(5, 1), new Coord2D(10, 0)));
        List<Coord2D> drawnWest = new ArrayList<>(drawnEast);
        Collections.reverse(drawnWest);
        List<Line<Coord2D>> legs = legs(new Coord2D(0, 4), new Coord2D(10, 4));

        LineOrienter.orientToInterior(drawnEast, legs);
        LineOrienter.orientToInterior(drawnWest, legs);

        Assert.assertEquals(drawnEast, drawnWest);
    }

    // Each segment votes with the leg nearest to it, weighted by length, so a couple of
    // stray points at the end can't flip a long wall whose bulk is clearly oriented.
    @Test
    public void testLongWallNotFlippedByStrayEnd() {
        List<Coord2D> points = new ArrayList<>();
        for (int x = 0; x <= 10; x++) {
            points.add(new Coord2D(x, 0));
        }
        points.add(new Coord2D(10, -1)); // stray hook back at the end
        List<Line<Coord2D>> legs = legs(new Coord2D(0, 3), new Coord2D(10, 3));

        boolean reversed = LineOrienter.orientToInterior(points, legs);

        Assert.assertFalse(reversed);
    }

    @Test
    public void testDegenerateInputsAreLeftAlone() {
        List<Coord2D> single = new ArrayList<>(Collections.singletonList(new Coord2D(0, 0)));
        Assert.assertFalse(
                LineOrienter.orientToInterior(single, legs(new Coord2D(1, 1), new Coord2D(2, 1))));

        List<Coord2D> points = new ArrayList<>(Arrays.asList(new Coord2D(0, 0), new Coord2D(1, 0)));
        Assert.assertFalse(LineOrienter.orientToInterior(points, Collections.emptyList()));
    }

    // A wall between two parallel passages. The passage to the north has one long leg whose
    // stations are far away; the passage to the south has a station just beyond the wall, but its
    // leg runs off away from it. The nearest station is to the south, but the nearest leg (and
    // the passage the wall belongs to) is to the north.
    @Test
    public void testNearestLegWinsOverNearerStationOfParallelPassage() {
        List<Coord2D> points =
                new ArrayList<>(Arrays.asList(new Coord2D(0, 0), new Coord2D(10, 0)));
        List<Line<Coord2D>> legs =
                Arrays.asList(
                        new Line<>(new Coord2D(-100, -2), new Coord2D(100, -2)),
                        new Line<>(new Coord2D(5, 5), new Coord2D(5, 50)));

        boolean reversed = LineOrienter.orientToInterior(points, legs);

        // the same answer as with the northern leg alone
        List<Coord2D> expected =
                new ArrayList<>(Arrays.asList(new Coord2D(0, 0), new Coord2D(10, 0)));
        LineOrienter.orientToInterior(expected, legs(new Coord2D(-100, -2), new Coord2D(100, -2)));
        Assert.assertTrue(reversed);
        Assert.assertEquals(expected, points);
    }

    private static List<Line<Coord2D>> legs(Coord2D start, Coord2D end) {
        return Collections.singletonList(new Line<>(start, end));
    }
}

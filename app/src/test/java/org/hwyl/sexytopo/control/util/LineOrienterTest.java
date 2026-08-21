package org.hwyl.sexytopo.control.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.junit.Assert;
import org.junit.Test;

public class LineOrienterTest {

    // A west-to-east wall with the station north of it (survey y is down, so north is -y).
    // Cross product of direction (east) with vector to station (north/-y) is negative, so the
    // points should be reversed to put the interior on the canonical side.
    @Test
    public void testWallIsReversedWhenInteriorOnWrongSide() {
        List<Coord2D> points =
                new ArrayList<>(
                        Arrays.asList(new Coord2D(0, 0), new Coord2D(5, 0), new Coord2D(10, 0)));
        List<Coord2D> stations = Collections.singletonList(new Coord2D(5, -3));

        boolean reversed = LineOrienter.orientToInterior(points, stations);

        Assert.assertTrue(reversed);
        Assert.assertEquals(10f, points.get(0).x, 0.0001f);
        Assert.assertEquals(0f, points.get(2).x, 0.0001f);
    }

    @Test
    public void testWallIsLeftAloneWhenAlreadyOriented() {
        List<Coord2D> points =
                new ArrayList<>(
                        Arrays.asList(new Coord2D(10, 0), new Coord2D(5, 0), new Coord2D(0, 0)));
        List<Coord2D> stations = Collections.singletonList(new Coord2D(5, -3));

        boolean reversed = LineOrienter.orientToInterior(points, stations);

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
        List<Coord2D> stations = Collections.singletonList(new Coord2D(5, 4));

        LineOrienter.orientToInterior(drawnEast, stations);
        LineOrienter.orientToInterior(drawnWest, stations);

        Assert.assertEquals(drawnEast, drawnWest);
    }

    // Each segment votes with the station nearest to it, weighted by length, so a couple of
    // stray points at the end can't flip a long wall whose bulk is clearly oriented.
    @Test
    public void testLongWallNotFlippedByStrayEnd() {
        List<Coord2D> points = new ArrayList<>();
        for (int x = 0; x <= 10; x++) {
            points.add(new Coord2D(x, 0));
        }
        points.add(new Coord2D(10, -1)); // stray hook back at the end
        List<Coord2D> stations = Collections.singletonList(new Coord2D(5, 3));

        boolean reversed = LineOrienter.orientToInterior(points, stations);

        Assert.assertFalse(reversed);
    }

    @Test
    public void testDegenerateInputsAreLeftAlone() {
        List<Coord2D> single = new ArrayList<>(Collections.singletonList(new Coord2D(0, 0)));
        Assert.assertFalse(
                LineOrienter.orientToInterior(
                        single, Collections.singletonList(new Coord2D(1, 1))));

        List<Coord2D> points = new ArrayList<>(Arrays.asList(new Coord2D(0, 0), new Coord2D(1, 0)));
        Assert.assertFalse(LineOrienter.orientToInterior(points, Collections.emptyList()));
    }
}

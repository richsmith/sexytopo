package org.hwyl.sexytopo.control.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.junit.Assert;
import org.junit.Test;

public class LoopOrienterTest {

    // A square drawn clockwise on screen (survey y is down) has its inside on the right, so a
    // line wanting its inside on the left is reversed.
    @Test
    public void testClosedLoopIsReversedToPutTicksInside() {
        List<Coord2D> points = clockwiseSquare();

        boolean reversed =
                LoopOrienter.orientInsideOfLoop(points, false, Collections.emptyList(), 1);

        Assert.assertTrue(reversed);
        Assert.assertEquals(new Coord2D(0, 1), points.get(0));
    }

    @Test
    public void testClosedLoopIsLeftAloneWhenTicksAlreadyInside() {
        List<Coord2D> points = clockwiseSquare();

        boolean reversed =
                LoopOrienter.orientInsideOfLoop(points, true, Collections.emptyList(), 1);

        Assert.assertFalse(reversed);
        Assert.assertEquals(new Coord2D(0, 0), points.get(0));
    }

    // A U hanging below a west-to-east wall, drawn left to right: with the stretch of wall between
    // its ends it runs anticlockwise on screen, so its inside is on the left.
    @Test
    public void testLineClosedByWallIsReversedToPutTicksInside() {
        List<Coord2D> points = uUnderWall();

        boolean reversed =
                LoopOrienter.orientInsideOfLoop(
                        points, true, Collections.singletonList(westToEastWall()), 1);

        Assert.assertTrue(reversed);
        Assert.assertEquals(10f, points.get(0).x, 0.0001f);
    }

    @Test
    public void testLineClosedByWallIsLeftAloneWhenTicksAlreadyInside() {
        List<Coord2D> points = uUnderWall();

        boolean reversed =
                LoopOrienter.orientInsideOfLoop(
                        points, false, Collections.singletonList(westToEastWall()), 1);

        Assert.assertFalse(reversed);
    }

    // The wall is drawn the other way, but the stretch between the line's ends is the same.
    @Test
    public void testWallDirectionDoesNotMatter() {
        List<Coord2D> wall = westToEastWall();
        Collections.reverse(wall);
        List<Coord2D> points = uUnderWall();

        boolean reversed =
                LoopOrienter.orientInsideOfLoop(points, true, Collections.singletonList(wall), 1);

        Assert.assertTrue(reversed);
    }

    // Ends on two different walls (e.g. a pit edge across a passage) enclose nothing.
    @Test
    public void testLineBetweenTwoWallsIsLeftAlone() {
        List<Coord2D> points =
                new ArrayList<>(
                        Arrays.asList(
                                new Coord2D(0, 0.2f), new Coord2D(1, 5), new Coord2D(0, 9.8f)));
        List<List<Coord2D>> walls =
                Arrays.asList(
                        Arrays.asList(new Coord2D(-10, 0), new Coord2D(10, 0)),
                        Arrays.asList(new Coord2D(-10, 10), new Coord2D(10, 10)));

        Assert.assertFalse(LoopOrienter.orientInsideOfLoop(points, true, walls, 1));
        Assert.assertFalse(LoopOrienter.orientInsideOfLoop(points, false, walls, 1));
    }

    @Test
    public void testOpenLineIsLeftAlone() {
        List<Coord2D> points =
                new ArrayList<>(
                        Arrays.asList(new Coord2D(0, 0), new Coord2D(5, 5), new Coord2D(10, 0)));

        Assert.assertFalse(
                LoopOrienter.orientInsideOfLoop(points, true, Collections.emptyList(), 1));
        Assert.assertFalse(
                LoopOrienter.orientInsideOfLoop(points, false, Collections.emptyList(), 1));
    }

    private static List<Coord2D> clockwiseSquare() {
        return new ArrayList<>(
                Arrays.asList(
                        new Coord2D(0, 0),
                        new Coord2D(10, 0),
                        new Coord2D(10, 10),
                        new Coord2D(0, 10),
                        new Coord2D(0, 1)));
    }

    private static List<Coord2D> uUnderWall() {
        return new ArrayList<>(
                Arrays.asList(new Coord2D(0, 0.2f), new Coord2D(5, 8), new Coord2D(10, 0.2f)));
    }

    private static List<Coord2D> westToEastWall() {
        return new ArrayList<>(
                Arrays.asList(
                        new Coord2D(-10, 0),
                        new Coord2D(0, 0),
                        new Coord2D(10, 0),
                        new Coord2D(20, 0)));
    }
}

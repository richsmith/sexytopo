package org.hwyl.sexytopo.control.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.junit.Assert;
import org.junit.Test;

public class LineReshaperTest {

    private static final List<Coord2D> STRAIGHT_LINE =
            Arrays.asList(new Coord2D(0, 0), new Coord2D(10, 0));

    private static final Coord2D UP_ONE = new Coord2D(0, 1);

    @Test
    public void testGrabbedPointGoesWithThePull() {
        List<Coord2D> pulled =
                LineReshaper.pull(STRAIGHT_LINE, false, new Coord2D(5, 0.1f), UP_ONE, 3, 0.5f);

        Assert.assertTrue(pulled.contains(new Coord2D(5, 1)));
    }

    @Test
    public void testLineCurvesBackToItsCourseAReachAway() {
        List<Coord2D> pulled =
                LineReshaper.pull(STRAIGHT_LINE, false, new Coord2D(5, 0), UP_ONE, 3, 0);

        // halfway out, the curve is halfway back
        Assert.assertEquals(0.5, yAt(pulled, 3.5f), 0.02);
        Assert.assertEquals(0.5, yAt(pulled, 6.5f), 0.02);
        // and beyond the reach the line is as it was
        Assert.assertEquals(0, yAt(pulled, 1.5f), 0);
        Assert.assertEquals(0, yAt(pulled, 8.5f), 0);
    }

    @Test
    public void testBentLineHasNoSharpBends() {
        List<Coord2D> pulled =
                LineReshaper.pull(STRAIGHT_LINE, false, new Coord2D(5, 0), UP_ONE, 3, 0);

        for (int i = 1; i < pulled.size() - 1; i++) {
            Coord2D in = pulled.get(i).minus(pulled.get(i - 1)).normalise();
            Coord2D out = pulled.get(i + 1).minus(pulled.get(i)).normalise();
            float cosine = in.x * out.x + in.y * out.y;
            Assert.assertTrue("sharp bend at " + pulled.get(i), cosine > 0.95);
        }
    }

    @Test
    public void testEndsStayPut() {
        List<Coord2D> pulled =
                LineReshaper.pull(STRAIGHT_LINE, false, new Coord2D(1, 0), UP_ONE, 3, 0.5f);

        Assert.assertEquals(STRAIGHT_LINE.get(0), pulled.get(0));
        Assert.assertEquals(STRAIGHT_LINE.get(1), pulled.get(pulled.size() - 1));
        Assert.assertTrue(pulled.contains(new Coord2D(1, 1)));
    }

    @Test
    public void testPullingNearAnEndTakesTheEnd() {
        List<Coord2D> pulled =
                LineReshaper.pull(STRAIGHT_LINE, false, new Coord2D(9.8f, 0), UP_ONE, 3, 0.5f);

        Assert.assertEquals(new Coord2D(0, 0), pulled.get(0));
        Assert.assertEquals(new Coord2D(10, 1), pulled.get(pulled.size() - 1));
    }

    @Test
    public void testLineIsUnchangedAwayFromTheBend() {
        List<Coord2D> line = new ArrayList<>();
        for (int x = 0; x <= 20; x++) {
            line.add(new Coord2D(x, (x % 2) * 0.1f));
        }

        List<Coord2D> pulled = LineReshaper.pull(line, false, new Coord2D(15, 0), UP_ONE, 2, 0);

        Assert.assertEquals(line.subList(0, 14), pulled.subList(0, 14));
        Assert.assertEquals(line.get(20), pulled.get(pulled.size() - 1));
    }

    @Test
    public void testLoopStaysClosedWhenBentRoundItsStart() {
        List<Coord2D> loop =
                Arrays.asList(
                        new Coord2D(0, 0),
                        new Coord2D(4, 0),
                        new Coord2D(4, 4),
                        new Coord2D(0, 4),
                        new Coord2D(0, 0));

        List<Coord2D> pulled =
                LineReshaper.pull(loop, true, new Coord2D(0.5f, 0), new Coord2D(0, -1), 2, 0);

        Assert.assertEquals(pulled.get(0), pulled.get(pulled.size() - 1));
        Assert.assertTrue(pulled.contains(new Coord2D(0.5f, -1)));
        // the corner it starts from is pulled along too
        Assert.assertNotEquals(new Coord2D(0, 0), pulled.get(0));
        // and the far corner stays put
        Assert.assertTrue(pulled.contains(new Coord2D(4, 4)));
    }

    /** How far up the line is at x, for a line running left to right. */
    private static float yAt(List<Coord2D> line, float x) {
        for (int i = 0; i < line.size() - 1; i++) {
            Coord2D a = line.get(i);
            Coord2D b = line.get(i + 1);
            if (a.x <= x && x <= b.x) {
                float t = a.x == b.x ? 0 : (x - a.x) / (b.x - a.x);
                return a.y + t * (b.y - a.y);
            }
        }
        throw new AssertionError("no point at x = " + x);
    }
}

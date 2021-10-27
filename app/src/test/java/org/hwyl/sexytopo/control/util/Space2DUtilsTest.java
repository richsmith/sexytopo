package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.graph.Coord2D;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class Space2DUtilsTest {

    @Test
    public void testSimplifyEmpty() {

        ArrayList<Coord2D> path = new ArrayList<>();
        List<Coord2D> simplifiedPath = Space2DUtils.simplify(path, 1);
        Assert.assertEquals(path, simplifiedPath);
    }

    @Test
    public void testSimplifyPoint() {

        ArrayList<Coord2D> path = new ArrayList<>();
        path.add(new Coord2D(0,0));

        List<Coord2D> simplifiedPath = Space2DUtils.simplify(path, 1);

        // a single point gets converted to a line with coincident points
        ArrayList<Coord2D> expectedPath = new ArrayList<>();
        expectedPath.add(new Coord2D(0,0));
        expectedPath.add(new Coord2D(0,0));

        Assert.assertEquals(expectedPath, simplifiedPath);
    }

    @Test
    public void testSimplifyLine() {

        ArrayList<Coord2D> path = new ArrayList<>();
        path.add(new Coord2D(0,0));
        path.add(new Coord2D(10,0));

        List<Coord2D> simplifiedPath = Space2DUtils.simplify(path, 0.01f);

        Assert.assertEquals(path, simplifiedPath);
    }

    @Test
    public void testSimplifyStraightPath() {

        ArrayList<Coord2D> path = new ArrayList<>();
        path.add(new Coord2D(0,0));
        path.add(new Coord2D(10,0));
        path.add(new Coord2D(20,0));
        path.add(new Coord2D(30,0));
        path.add(new Coord2D(40,0));
        path.add(new Coord2D(50,0));

        float epsilon = Space2DUtils.simplificationEpsilon(50, 50);
        List<Coord2D> simplifiedPath = Space2DUtils.simplify(path, epsilon);

        ArrayList<Coord2D> expectedPath = new ArrayList<>();
        expectedPath.add(new Coord2D(0,0));
        expectedPath.add(new Coord2D(50,0));

        Assert.assertEquals(expectedPath, simplifiedPath);
    }

    @Test
    public void testSimplifyRightAngledPath() {

        ArrayList<Coord2D> path = new ArrayList<>();
        path.add(new Coord2D(0,0));
        path.add(new Coord2D(5,0));
        path.add(new Coord2D(10,0));
        path.add(new Coord2D(10,5));
        path.add(new Coord2D(10,10));

        float epsilon = Space2DUtils.simplificationEpsilon(10, 10);
        List<Coord2D> simplifiedPath = Space2DUtils.simplify(path, epsilon);

        ArrayList<Coord2D> expectedPath = new ArrayList<>();
        expectedPath.add(new Coord2D(0,0));
        expectedPath.add(new Coord2D(10,0));
        expectedPath.add(new Coord2D(10,10));

        Assert.assertEquals(expectedPath, simplifiedPath);
    }

    @Test
    public void testSimplifyDensifiedCircle() {

        float pi2 = 2 * (float)Math.PI;
        float step = pi2/12;
        float radius = 5;
        int offset = 10;

        // Make a 12 point circle approximation
        ArrayList<Coord2D> path = new ArrayList<>();
        for(double theta = 0; theta < pi2; theta += step)
        {
            float x = offset + radius * (float)Math.cos(theta);
            float y = offset - radius * (float)Math.sin(theta);
            path.add(new Coord2D(x, y));
        }
        path.add(path.get(0));

        {
            float epsilon = Space2DUtils.simplificationEpsilon(10, 10);
            List<Coord2D> simplifiedPath = Space2DUtils.simplify(path, epsilon);

            // Remains unchanged
            Assert.assertEquals(path, simplifiedPath);
        }

        {
            float epsilon = 4;
            List<Coord2D> simplifiedPath = Space2DUtils.simplify(path, epsilon);

            // Aggressive epsilon simplifies to a diamond
            ArrayList<Coord2D> expectedPath = new ArrayList<>();
            expectedPath.add(new Coord2D(15, 10));
            expectedPath.add(new Coord2D(10,  5));
            expectedPath.add(new Coord2D( 5, 10));
            expectedPath.add(new Coord2D(10, 15));
            expectedPath.add(new Coord2D(15, 10));

            Assert.assertEquals(expectedPath, simplifiedPath);
        }
    }
}

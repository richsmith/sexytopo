package org.hwyl.sexytopo.model.graph;

import junit.framework.TestCase;

import org.hwyl.sexytopo.model.common.Frame;

public class FrameTest extends TestCase {

    final Frame irregularFrame = new Frame(-6.0f, 10.0f, -9.0f, 3.0f);
    final Frame regularFrame = new Frame(-10.0f, 10.0f, -10.0f, 10.0f);
    final Frame offsetFrame = new Frame(0.0f, 20.0f, 0.0f, 20.0f);

    public void testExpanding() {

        Frame rounded = irregularFrame.expandToNearest(10);
        assertEquals(new Coord2D(-10, -10), rounded.getTopLeft());
        assertEquals(new Coord2D(10, 10), rounded.getBottomRight());
    }

    public void testAddPadding() {
        Frame bordered = regularFrame.addPadding(10, 10);
        assertEquals(new Coord2D(-20, -20), bordered.getTopLeft());
        assertEquals(new Coord2D(20, 20), bordered.getBottomRight());
    }

    public void testUnionWithBigger() {
        Frame union = irregularFrame.union(regularFrame);
        assertEquals(new Coord2D(-10, -10), union.getTopLeft());
        assertEquals(new Coord2D(10, 10), union.getBottomRight());
    }

    public void testUnionWithOffset() {
        Frame union = regularFrame.union(offsetFrame);
        assertEquals(new Coord2D(-10, -10), union.getTopLeft());
        assertEquals(new Coord2D(20, 20), union.getBottomRight());
    }
}
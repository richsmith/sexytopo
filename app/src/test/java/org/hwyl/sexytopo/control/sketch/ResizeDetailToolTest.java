package org.hwyl.sexytopo.control.sketch;

import org.junit.Assert;
import org.junit.Test;

public class ResizeDetailToolTest {

    private static final float DELTA = 0.0001f;

    @Test
    public void testNoDragLeavesTheSizeAlone() {
        Assert.assertEquals(1, ResizeDetailTool.calculateScale(0, 100, 50, 8), DELTA);
    }

    @Test
    public void testDraggingUpTheDoublingDistanceDoublesTheSize() {
        Assert.assertEquals(2, ResizeDetailTool.calculateScale(100, 100, 50, 8), DELTA);
    }

    @Test
    public void testDraggingDownTheDoublingDistanceHalvesTheSize() {
        Assert.assertEquals(0.5f, ResizeDetailTool.calculateScale(-100, 100, 50, 8), DELTA);
    }

    @Test
    public void testShrinkingStopsAtTheMinimumSize() {
        Assert.assertEquals(0.25f, ResizeDetailTool.calculateScale(-1000, 100, 32, 8), DELTA);
    }

    @Test
    public void testSomethingBelowTheMinimumSizeIsNotShrunkFurtherButCanGrow() {
        Assert.assertEquals(1, ResizeDetailTool.calculateScale(-100, 100, 4, 8), DELTA);
        Assert.assertEquals(2, ResizeDetailTool.calculateScale(100, 100, 4, 8), DELTA);
    }
}

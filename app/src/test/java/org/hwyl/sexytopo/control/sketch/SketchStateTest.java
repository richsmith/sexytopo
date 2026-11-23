package org.hwyl.sexytopo.control.sketch;

import org.hwyl.sexytopo.model.sketch.SketchTool;
import org.junit.Assert;
import org.junit.Test;

public class SketchStateTest {

    @Test
    public void testOneUseToolGoesBackToTheToolItInterrupted() {
        SketchState state = new SketchState();
        state.setSketchTool(SketchTool.LINE);
        state.startTemporaryTool(SketchTool.MOVE_DETAIL);

        state.endTemporaryTool();

        Assert.assertEquals(SketchTool.LINE, state.getSketchTool());
    }

    @Test
    public void testPinchDuringOneUseToolGoesBackToItThenToTheToolBefore() {
        SketchState state = new SketchState();
        state.setSketchTool(SketchTool.LINE);
        state.startTemporaryTool(SketchTool.RESIZE_DETAIL);
        state.startTemporaryTool(SketchTool.PINCH_TO_ZOOM);
        Assert.assertEquals(SketchTool.RESIZE_DETAIL, state.getUnderlyingSketchTool());

        state.endTemporaryTool();
        Assert.assertEquals(SketchTool.RESIZE_DETAIL, state.getSketchTool());

        state.endTemporaryTool();
        Assert.assertEquals(SketchTool.LINE, state.getSketchTool());
    }

    @Test
    public void testModalToolReplacingAnotherGoesBackToTheToolBeneathBoth() {
        SketchState state = new SketchState();
        state.setSketchTool(SketchTool.AREA);
        state.startTemporaryTool(SketchTool.PINCH_TO_ZOOM);
        state.startTemporaryTool(SketchTool.MODAL_MOVE);

        state.endTemporaryTool();

        Assert.assertEquals(SketchTool.AREA, state.getSketchTool());
    }

    @Test
    public void testChoosingAnOrdinaryToolForgetsWhatWasInterrupted() {
        SketchState state = new SketchState();
        state.setSketchTool(SketchTool.LINE);
        state.startTemporaryTool(SketchTool.MOVE_DETAIL);
        state.setSketchTool(SketchTool.SYMBOL);

        state.endTemporaryTool();

        Assert.assertEquals(SketchTool.MOVE, state.getSketchTool());
    }

    @Test
    public void testRevertingWithNothingInterruptedGoesToMove() {
        SketchState state = new SketchState();
        state.startTemporaryTool(SketchTool.PINCH_TO_ZOOM);

        state.endTemporaryTool();

        Assert.assertEquals(SketchTool.MOVE, state.getSketchTool());
    }
}

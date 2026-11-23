package org.hwyl.sexytopo.control.sketch;

import android.graphics.Canvas;
import android.view.MotionEvent;
import org.hwyl.sexytopo.model.sketch.SketchDetail;

/** What a sketch tool does with the touch events dispatched to it while it's selected. */
interface ToolHandler {

    /** Returns whether the event was handled. */
    boolean onTouch(MotionEvent event);

    /**
     * Draw anything showing what the tool is in the middle of doing, such as a guide line or a
     * highlight, over the survey. Most tools have nothing to show.
     */
    default void drawFeedback(Canvas canvas) {}

    /**
     * A detail the tool draws itself as feedback, so it's left out of the sketch's normal drawing,
     * or null if there isn't one.
     */
    default SketchDetail getHiddenDetail() {
        return null;
    }
}

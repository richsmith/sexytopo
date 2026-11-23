package org.hwyl.sexytopo.control.sketch;

import android.graphics.Canvas;
import android.view.MotionEvent;

/** What a sketch tool does with the touch events dispatched to it while it's selected. */
interface ToolHandler {

    /** Returns whether the event was handled. */
    boolean onTouch(MotionEvent event);

    /**
     * Draw anything showing what the tool is in the middle of doing, such as a guide line or a
     * highlight, over the survey. Most tools have nothing to show.
     */
    default void drawFeedback(Canvas canvas) {}
}

package org.hwyl.sexytopo.control.sketch;

import android.view.MotionEvent;

/** What a sketch tool does with the touch events dispatched to it while it's selected. */
interface ToolHandler {

    /** Returns whether the event was handled. */
    boolean onTouch(MotionEvent event);
}

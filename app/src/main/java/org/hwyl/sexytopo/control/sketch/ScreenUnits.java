package org.hwyl.sexytopo.control.sketch;

import android.content.Context;
import org.hwyl.sexytopo.control.SexyTopo;

/** Converts sizes given in dp and sp, as designed, to pixels on this device's screen. */
final class ScreenUnits {

    private ScreenUnits() {}

    /** Converts dp to pixels, never giving less than one pixel. */
    static float dpToPixels(float dp) {
        return Math.max(1f, SexyTopo.dpToPixels(dp));
    }

    /** Converts sp, which also follows the user's font size setting, to pixels. */
    static float spToPixels(Context context, float sp) {
        return sp * context.getResources().getDisplayMetrics().scaledDensity;
    }
}

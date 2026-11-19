package org.hwyl.sexytopo.control.sketch;

import android.content.Context;
import android.graphics.Paint;
import android.util.TypedValue;
import org.hwyl.sexytopo.control.util.GeneralPreferences;

/**
 * What the paints in a sketch view share: the solid and faded alphas they draw the survey with, how
 * the user's anti-aliasing preference is applied to them, and looking up theme colours for them.
 * The paints themselves belong to the classes that draw with them.
 */
final class SketchPaints {

    static final int SOLID_ALPHA = 0xff;
    static final int FADED_ALPHA = 0xff / 5;

    private SketchPaints() {}

    /** Turn anti-aliasing on or off for each paint, as the user's preference says. */
    static void applyAntiAliasPreference(Paint... paints) {
        boolean applyAntiAlias = GeneralPreferences.isAntialiasingModeOn();
        for (Paint paint : paints) {
            if (paint.isAntiAlias() != applyAntiAlias) {
                paint.setAntiAlias(applyAntiAlias);
            }
        }
    }

    /** The colour a theme attribute (such as colorPrimary) has in the given context's theme. */
    static int resolveThemeColor(Context context, int attr) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }
}

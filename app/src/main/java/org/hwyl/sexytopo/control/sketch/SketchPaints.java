package org.hwyl.sexytopo.control.sketch;

import android.content.Context;
import android.graphics.Paint;
import android.util.TypedValue;
import androidx.core.content.ContextCompat;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.util.GeneralPreferences;

/**
 * What the paints in a sketch view share: the solid and faded alphas they draw the survey with, how
 * the user's anti-aliasing preference is applied to them, and looking up theme colours for them.
 * The paints themselves belong to the classes that draw with them.
 */
final class SketchPaints {

    static final int SOLID_ALPHA = 0xff;
    static final int FADED_ALPHA = 0xff / 5;

    // the line width of the highlight round the active station, and anything else highlighted
    private static final float HIGHLIGHT_STROKE_WIDTH_DP = 2.5f;

    private SketchPaints() {}

    /** Set a paint up to draw highlight brackets, as round the active station. */
    static void setUpHighlightPaint(Context context, Paint paint) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(ScreenUnits.dpToPixels(HIGHLIGHT_STROKE_WIDTH_DP));
        paint.setColor(ContextCompat.getColor(context, R.color.activeStationHighlight));
    }

    /**
     * Set a paint up to draw what sits on the screen rather than in the cave, such as the legend
     * and the compass: in the legend colour, at the legend font size.
     */
    static void setUpLegendPaint(Context context, Paint paint) {
        paint.setColor(ContextCompat.getColor(context, R.color.legend));
        float legendSizeSp = GeneralPreferences.getLegendFontSizeSp();
        paint.setTextSize(ScreenUnits.spToPixels(context, legendSizeSp));
        paint.setStrokeWidth(ScreenUnits.spToPixels(context, legendSizeSp * 0.1f));
    }

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

package org.hwyl.sexytopo.control.sketch;

import android.content.Context;
import android.graphics.Paint;
import android.util.TypedValue;
import androidx.core.content.ContextCompat;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.SexyTopo;
import org.hwyl.sexytopo.control.util.GeneralPreferences;

/**
 * The paints for the parts of a sketch view drawn over the survey (legend, grid, hot corners), set
 * up from the theme and preferences by initialise, and helpers shared by everything that draws: the
 * alphas, pattern scale, anti-aliasing preference and dp and sp conversion.
 */
class SketchPaints {

    static final int SOLID_ALPHA = 0xff;
    static final int FADED_ALPHA = 0xff / 5;

    private static final int LEGEND_TICK_SIZE_DP = 5;

    // the most a dp of line ornamentation or area fill can stand for in the cave, so zoomed far
    // out a decoration can't dwarf what it decorates (the default 12dp tick comes to 0.6m)
    private static final float MAX_PATTERN_METRES_PER_DP = 0.05f;

    final Paint legendPaint = new Paint();
    final Paint gridPaint = new Paint();
    final Paint hotCornersPaint = new Paint();

    private final Paint[] ANTI_ALIAS_PAINTS = new Paint[] {legendPaint};

    float legendTickSizePx;

    /** Set everything up from the theme and current preferences. */
    void initialise(Context context) {

        applyAntiAliasPreference(ANTI_ALIAS_PAINTS);

        int gridColour = ContextCompat.getColor(context, R.color.grid);
        gridPaint.setColor(gridColour);

        int legendColour = ContextCompat.getColor(context, R.color.legend);
        legendPaint.setColor(legendColour);
        float legendSizeSp = GeneralPreferences.getLegendFontSizeSp();
        float legendSizePixels = spToPixels(context, legendSizeSp);
        float strokeWidthPixels = spToPixels(context, legendSizeSp * 0.1f);
        legendPaint.setStrokeWidth(strokeWidthPixels);
        legendPaint.setTextSize(legendSizePixels);

        hotCornersPaint.setColor(ContextCompat.getColor(context, R.color.hotCorner));
        hotCornersPaint.setAlpha(FADED_ALPHA);

        legendTickSizePx = dpToPixels(LEGEND_TICK_SIZE_DP);
    }

    /**
     * How much to scale line ornamentation and area fills from their designed size, so they zoom
     * with the cave instead of crawling across it.
     */
    static float getPatternScale(float viewScale) {
        // re-pick the density each time the zoom doubles or halves, so it never strays more than
        // √2 from its designed size; doubling keeps existing marks in place and adds new ones
        double octave = Math.round(Math.log(viewScale) / Math.log(2));
        float scale = (float) (viewScale / Math.pow(2, octave));
        // but zoomed far out, don't let a tick dwarf the line it's on
        float maxScale = MAX_PATTERN_METRES_PER_DP * viewScale / dpToPixels(1);
        return Math.min(scale, maxScale);
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

    /** Converts dp to pixels, never giving less than one pixel. */
    static float dpToPixels(float dp) {
        return Math.max(1f, SexyTopo.dpToPixels(dp));
    }

    static float spToPixels(Context context, float sp) {
        return sp * context.getResources().getDisplayMetrics().scaledDensity;
    }

    static int resolveThemeColor(Context context, int attr) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }
}

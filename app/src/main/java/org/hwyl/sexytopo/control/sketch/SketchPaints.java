package org.hwyl.sexytopo.control.sketch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.TypedValue;
import androidx.core.content.ContextCompat;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.SexyTopo;
import org.hwyl.sexytopo.control.util.GeneralPreferences;

/**
 * The paints, shaders, icons and on-screen sizes a sketch view draws with, set up from the theme
 * and preferences by initialise.
 *
 * <p>Line ornamentation and wall rock-side shading are path effects whose size depends on the zoom.
 * They're cached here alongside the size they were built for and only rebuilt when that changes,
 * not on every frame.
 */
class SketchPaints {

    static final int SOLID_ALPHA = 0xff;
    static final int FADED_ALPHA = 0xff / 5;

    private static final int STATION_STROKE_WIDTH_DP = 2;
    private static final int DASHED_LINE_INTERVAL_DP = 4;
    private static final int LEGEND_TICK_SIZE_DP = 5;
    private static final int STATION_LABEL_OFFSET_DP = 10;

    // the most a dp of line ornamentation or area fill can stand for in the cave, so zoomed far
    // out a decoration can't dwarf what it decorates (the default 12dp tick comes to 0.6m)
    private static final float MAX_PATTERN_METRES_PER_DP = 0.05f;

    final Paint stationPaint = new Paint();
    final Paint iconPaint = new Paint();

    final Paint legPaint = new Paint();
    final Paint latestLegPaint = new Paint();
    final Paint splayPaint = new Paint();

    final Paint fadedLegPaint = new Paint();
    final Paint fadedLatestLegPaint = new Paint();
    final Paint fadedSplayPaint = new Paint();

    final Paint highlightPaint = new Paint();
    final Paint legendPaint = new Paint();
    final Paint gridPaint = new Paint();
    final Paint hotCornersPaint = new Paint();

    private final Paint[] ANTI_ALIAS_PAINTS =
            new Paint[] {
                stationPaint,
                iconPaint,
                legendPaint,
                latestLegPaint,
                splayPaint,
                fadedLegPaint,
                fadedLatestLegPaint,
                fadedSplayPaint
            };

    float stationCrossDiameterPx;
    float legendTickSizePx;
    float dashedLineIntervalPx;
    float stationLabelOffsetPx;

    Bitmap commentIcon;
    Bitmap linkIcon;

    /** Set everything up from the theme and current preferences. */
    void initialise(Context context) {

        applyAntiAliasPreference(ANTI_ALIAS_PAINTS);

        int gridColour = ContextCompat.getColor(context, R.color.grid);
        gridPaint.setColor(gridColour);

        int stationColour = ContextCompat.getColor(context, R.color.station);
        stationPaint.setColor(stationColour);
        float stationStrokeWidthPx = dpToPixels(STATION_STROKE_WIDTH_DP);
        stationPaint.setStrokeWidth(stationStrokeWidthPx);
        int stationLabelFontSizeSp = GeneralPreferences.getStationLabelFontSizeSp();
        float stationLabelFontSizePixels = spToPixels(context, stationLabelFontSizeSp);
        stationPaint.setTextSize(stationLabelFontSizePixels);

        iconPaint.setColorFilter(new PorterDuffColorFilter(stationColour, PorterDuff.Mode.SRC_IN));

        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setStrokeWidth(stationStrokeWidthPx * 1.25f);
        int activeStationHighlightColor =
                ContextCompat.getColor(context, R.color.activeStationHighlight);
        highlightPaint.setColor(activeStationHighlightColor);

        // active legs/splays
        int legColour = ContextCompat.getColor(context, R.color.leg);
        int latestLegColour = ContextCompat.getColor(context, R.color.legLatest);
        int splayColour = ContextCompat.getColor(context, R.color.splay);
        float legStrokeWidth = dpToPixels(GeneralPreferences.getLegStrokeWidthDp());
        legPaint.setStrokeWidth(legStrokeWidth);
        legPaint.setColor(legColour);

        latestLegPaint.setStrokeWidth(legStrokeWidth);
        latestLegPaint.setColor(latestLegColour);

        float splayStrokeWidth = dpToPixels(GeneralPreferences.getSplayStrokeWidthDp());
        splayPaint.setStrokeWidth(splayStrokeWidth);
        splayPaint.setColor(splayColour);

        // faded legs/splays
        fadedLegPaint.setStrokeWidth(legStrokeWidth);
        fadedLegPaint.setColor(legColour);
        fadedLegPaint.setAlpha(FADED_ALPHA);

        fadedLatestLegPaint.setStrokeWidth(legStrokeWidth);
        fadedLatestLegPaint.setColor(latestLegColour);
        fadedLatestLegPaint.setAlpha(FADED_ALPHA);

        fadedSplayPaint.setStrokeWidth(splayStrokeWidth);
        fadedSplayPaint.setColor(splayColour);
        fadedSplayPaint.setAlpha(FADED_ALPHA);

        int legendColour = ContextCompat.getColor(context, R.color.legend);
        legendPaint.setColor(legendColour);
        float legendSizeSp = GeneralPreferences.getLegendFontSizeSp();
        float legendSizePixels = spToPixels(context, legendSizeSp);
        float strokeWidthPixels = spToPixels(context, legendSizeSp * 0.1f);
        legendPaint.setStrokeWidth(strokeWidthPixels);
        legendPaint.setTextSize(legendSizePixels);

        hotCornersPaint.setColor(ContextCompat.getColor(context, R.color.hotCorner));
        hotCornersPaint.setAlpha(FADED_ALPHA);

        stationCrossDiameterPx = dpToPixels(GeneralPreferences.getStationCrossDiameterDp());
        legendTickSizePx = dpToPixels(LEGEND_TICK_SIZE_DP);
        dashedLineIntervalPx = dpToPixels(DASHED_LINE_INTERVAL_DP);
        stationLabelOffsetPx = dpToPixels(STATION_LABEL_OFFSET_DP);

        commentIcon =
                BitmapFactory.decodeResource(context.getResources(), R.drawable.speech_bubble);
        linkIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.link);
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

package org.hwyl.sexytopo.control.sketch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PathEffect;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Shader;
import android.util.TypedValue;
import androidx.core.content.ContextCompat;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.SexyTopo;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.model.sketch.AreaType;
import org.hwyl.sexytopo.model.sketch.LineType;

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
    private static final int CROSS_SECTION_CONNECTOR_WIDTH_DP = 2;
    private static final int CROSS_SECTION_INDICATOR_WIDTH_DP = 2;
    private static final int CROSS_SECTION_BORDER_WIDTH_DP = 2;
    private static final float CROSS_SECTION_GLYPH_SIZE_DP = 13f;
    private static final int CROSS_SECTION_HANDLE_GRIP_WIDTH_DP = 2;
    private static final int LEGEND_TICK_SIZE_DP = 5;
    private static final int STATION_LABEL_OFFSET_DP = 10;

    // fill tiles are rendered at this multiple of their on-screen size and scaled down to draw
    static final int AREA_FILL_OVERSAMPLE = 2;
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

    final Paint drawPaint = new Paint();
    // one paint per semantic line type (stroke width and ornamentation baked in), alongside the
    // on-screen ornament size each paint's effect was last built for, so the effect is only
    // rebuilt when that changes (i.e. on zoom)
    private final Map<LineType, Paint> lineTypePaints = new EnumMap<>(LineType.class);
    private final Map<LineType, Float> lineTypePaintOrnamentSizePixels =
            new EnumMap<>(LineType.class);
    // one paint per layer of wall rock-side shading
    private final Paint[] wallRockSidePaints = createWallRockSidePaints();
    private float wallRockSidePaintsOrnamentSizePixels = -1;
    final Paint areaPaint = new Paint();
    // fills areas with their type's pattern; the shader is swapped per area type
    final Paint areaFillPaint = new Paint();
    private final Map<AreaType, BitmapShader> areaFillShaders = new EnumMap<>(AreaType.class);
    final Paint labelPaint = new Paint();
    final Paint highlightPaint = new Paint();
    final Paint legendPaint = new Paint();
    final Paint gridPaint = new Paint();
    final Paint crossSectionConnectorPaint = new Paint();
    final Paint crossSectionIndicatorPaint = new Paint();
    final Paint crossSectionHandlePaint = new Paint();
    final Paint crossSectionHandleGripPaint = new Paint();
    final Paint crossSectionBorderPaint = new Paint();
    final Paint crossSectionHorizontalHandlePaint = new Paint();
    final Paint crossSectionHorizontalBorderPaint = new Paint();
    final Paint crossSectionGlyphPaint = new Paint();
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
                fadedSplayPaint,
                drawPaint,
                areaPaint,
                labelPaint,
                crossSectionConnectorPaint,
                crossSectionIndicatorPaint,
                crossSectionHandlePaint,
                crossSectionHandleGripPaint,
                crossSectionBorderPaint,
                crossSectionHorizontalHandlePaint,
                crossSectionHorizontalBorderPaint,
                crossSectionGlyphPaint
            };

    float stationCrossDiameterPx;
    float legendTickSizePx;
    float dashedLineIntervalPx;
    float stationLabelOffsetPx;

    Bitmap commentIcon;
    Bitmap linkIcon;

    /** Set everything up from the theme and current preferences. */
    void initialise(Context context) {

        boolean applyAntiAlias = GeneralPreferences.isAntialiasingModeOn();
        for (Paint paint : ANTI_ALIAS_PAINTS) {
            if (paint.isAntiAlias() != applyAntiAlias) {
                paint.setAntiAlias(applyAntiAlias);
            }
        }

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

        int sketchDefaultColour = ContextCompat.getColor(context, R.color.sketchDefault);
        drawPaint.setColor(sketchDefaultColour);
        drawPaint.setStrokeWidth(dpToPixels(GeneralPreferences.getSketchLineWidthDp()));
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        // the stipple is sized by the line width
        wallRockSidePaintsOrnamentSizePixels = -1;

        lineTypePaints.clear();
        for (LineType lineType : LineType.values()) {
            Paint paint = new Paint(drawPaint);
            paint.setStrokeWidth(drawPaint.getStrokeWidth() * lineType.getStrokeWidthFactor());
            lineTypePaints.put(lineType, paint);
        }
        lineTypePaintOrnamentSizePixels.clear();

        areaPaint.setStyle(Paint.Style.STROKE);
        areaPaint.setStrokeJoin(Paint.Join.ROUND);
        areaPaint.setStrokeCap(Paint.Cap.ROUND);

        areaFillPaint.setStyle(Paint.Style.FILL);
        areaFillPaint.setFilterBitmap(true);
        areaFillPaint.setAntiAlias(true);
        areaFillShaders.clear();
        for (AreaType areaType : AreaType.values()) {
            areaFillShaders.put(areaType, createAreaFillShader(areaType.getFill()));
        }

        int legendColour = ContextCompat.getColor(context, R.color.legend);
        legendPaint.setColor(legendColour);
        float legendSizeSp = GeneralPreferences.getLegendFontSizeSp();
        float legendSizePixels = spToPixels(context, legendSizeSp);
        float strokeWidthPixels = spToPixels(context, legendSizeSp * 0.1f);
        legendPaint.setStrokeWidth(strokeWidthPixels);
        legendPaint.setTextSize(legendSizePixels);

        int labelColour = ContextCompat.getColor(context, R.color.station);
        labelPaint.setColor(labelColour);
        int labelSizeSp = GeneralPreferences.getLabelFontSizeSp();
        float labelSizePixels = spToPixels(context, labelSizeSp);
        labelPaint.setTextSize(labelSizePixels);

        crossSectionConnectorPaint.setColor(
                ContextCompat.getColor(context, R.color.crossSectionConnection));
        crossSectionConnectorPaint.setStrokeWidth(dpToPixels(CROSS_SECTION_CONNECTOR_WIDTH_DP));
        crossSectionConnectorPaint.setStyle(Paint.Style.STROKE);

        crossSectionIndicatorPaint.setColor(
                ContextCompat.getColor(context, R.color.crossSectionIndicator));
        crossSectionIndicatorPaint.setStrokeWidth(dpToPixels(CROSS_SECTION_INDICATOR_WIDTH_DP));
        crossSectionIndicatorPaint.setStyle(Paint.Style.FILL);

        int primaryColor = resolveThemeColor(context, androidx.appcompat.R.attr.colorPrimary);

        crossSectionHandlePaint.setColor(primaryColor);
        crossSectionHandlePaint.setStyle(Paint.Style.FILL);
        crossSectionHandlePaint.setAntiAlias(true);

        crossSectionHandleGripPaint.setColor(
                resolveThemeColor(context, com.google.android.material.R.attr.colorOnPrimary));
        crossSectionHandleGripPaint.setStrokeWidth(dpToPixels(CROSS_SECTION_HANDLE_GRIP_WIDTH_DP));
        crossSectionHandleGripPaint.setStyle(Paint.Style.STROKE);
        crossSectionHandleGripPaint.setStrokeCap(Paint.Cap.ROUND);

        crossSectionBorderPaint.setColor(primaryColor);
        crossSectionBorderPaint.setStrokeWidth(dpToPixels(CROSS_SECTION_BORDER_WIDTH_DP));
        crossSectionBorderPaint.setStyle(Paint.Style.STROKE);

        // Horizontal cross-sections get a differently coloured box so they can be told apart from
        // vertical ones at a glance
        int horizontalBoxColour =
                ContextCompat.getColor(context, R.color.crossSectionHorizontalBox);

        crossSectionHorizontalHandlePaint.setColor(horizontalBoxColour);
        crossSectionHorizontalHandlePaint.setStyle(Paint.Style.FILL);
        crossSectionHorizontalHandlePaint.setAntiAlias(true);

        crossSectionHorizontalBorderPaint.setColor(horizontalBoxColour);
        crossSectionHorizontalBorderPaint.setStrokeWidth(dpToPixels(CROSS_SECTION_BORDER_WIDTH_DP));
        crossSectionHorizontalBorderPaint.setStyle(Paint.Style.STROKE);

        crossSectionGlyphPaint.setTextSize(dpToPixels(CROSS_SECTION_GLYPH_SIZE_DP));
        crossSectionGlyphPaint.setTextAlign(Paint.Align.CENTER);
        crossSectionGlyphPaint.setStyle(Paint.Style.FILL);

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

    private static float getOrnamentSizePixels(float patternScale) {
        return SexyTopo.dpToPixels(GeneralPreferences.getLineOrnamentSizeDp()) * patternScale;
    }

    /**
     * The paint for a semantic line type, its ornamentation sized for the given pattern scale.
     * Falls back to the plain sketch paint if the paints haven't been initialised yet.
     */
    Paint getLineTypePaint(LineType lineType, float patternScale) {
        Paint paint = lineTypePaints.get(lineType);
        if (paint == null) { // paints not initialised yet
            return drawPaint;
        }
        updateOrnamentation(paint, lineType, getOrnamentSizePixels(patternScale));
        return paint;
    }

    /**
     * Rebuilds a line type's path effect if the ornament size on screen differs from the one the
     * cached effect was built for, which happens on zoom — not on every frame.
     */
    private void updateOrnamentation(Paint paint, LineType lineType, float ornamentSizePixels) {
        Float builtFor = lineTypePaintOrnamentSizePixels.get(lineType);
        if (builtFor != null && builtFor == ornamentSizePixels) {
            return;
        }
        PathEffect pathEffect =
                lineType.createPathEffect(ornamentSizePixels, paint.getStrokeWidth());
        paint.setPathEffect(pathEffect);
        // Stamped shapes are filled as well as outlined, so overhang teeth come out solid. Only
        // while the stamp is in use, though: filling a plain path would fill the area it encloses.
        paint.setStyle(
                pathEffect instanceof PathDashPathEffect
                        ? Paint.Style.FILL_AND_STROKE
                        : Paint.Style.STROKE);
        lineTypePaintOrnamentSizePixels.put(lineType, ornamentSizePixels);
    }

    /**
     * One paint per layer of wall rock-side shading (matching WallRockSide.getLayers), their
     * effects sized for the given pattern scale.
     */
    Paint[] getWallRockSidePaints(float patternScale) {
        float ornamentSizePixels = getOrnamentSizePixels(patternScale);
        if (ornamentSizePixels != wallRockSidePaintsOrnamentSizePixels) {
            List<WallRockSide.Layer> layers = WallRockSide.getLayers();
            float lineWidth = drawPaint.getStrokeWidth();
            for (int i = 0; i < layers.size(); i++) {
                wallRockSidePaints[i].setPathEffect(
                        layers.get(i).createEffect(ornamentSizePixels, lineWidth));
            }
            wallRockSidePaintsOrnamentSizePixels = ornamentSizePixels;
        }
        return wallRockSidePaints;
    }

    private static Paint[] createWallRockSidePaints() {
        Paint[] paints = new Paint[WallRockSide.getLayers().size()];
        for (int i = 0; i < paints.length; i++) {
            paints[i] = new Paint();
            paints[i].setStyle(Paint.Style.FILL);
            paints[i].setAntiAlias(true);
        }
        return paints;
    }

    /** The repeating fill pattern for an area type, AREA_FILL_OVERSAMPLE times its drawn size. */
    BitmapShader getAreaFillShader(AreaType areaType) {
        return areaFillShaders.get(areaType);
    }

    /**
     * Render one tile of an area fill pattern into an alpha mask, to be repeated by a shader. The
     * tile is rounded to a whole number of pixels so the repeats line up exactly, and rendered
     * oversampled so it stays crisp when scaled up between zoom octaves (see drawAreas).
     */
    private static BitmapShader createAreaFillShader(AreaType.Fill fill) {
        int sizePixels =
                Math.max(1, Math.round(dpToPixels(fill.getTileSize()) * AREA_FILL_OVERSAMPLE));
        float scale = sizePixels / fill.getTileSize();

        Bitmap tile = Bitmap.createBitmap(sizePixels, sizePixels, Bitmap.Config.ALPHA_8);
        Canvas tileCanvas = new Canvas(tile);

        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(
                dpToPixels(AreaType.Fill.STROKE_WIDTH_DP) * AREA_FILL_OVERSAMPLE);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        for (AreaType.Mark mark : fill.getMarks()) {
            float[] values = mark.getValues();
            Path path = new Path();
            switch (mark.getKind()) {
                case LINE:
                case POLYGON:
                    path.moveTo(values[0] * scale, values[1] * scale);
                    for (int i = 2; i < values.length; i += 2) {
                        path.lineTo(values[i] * scale, values[i + 1] * scale);
                    }
                    if (mark.getKind() == AreaType.Mark.Kind.POLYGON) {
                        path.close();
                    }
                    tileCanvas.drawPath(path, strokePaint);
                    break;
                case DOT:
                case RING:
                    path.addCircle(
                            values[0] * scale,
                            values[1] * scale,
                            values[2] * scale,
                            Path.Direction.CW);
                    Paint paint =
                            mark.getKind() == AreaType.Mark.Kind.DOT ? fillPaint : strokePaint;
                    tileCanvas.drawPath(path, paint);
                    break;
            }
        }

        return new BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
    }

    /** Converts dp to pixels, never giving less than one pixel. */
    static float dpToPixels(float dp) {
        return Math.max(1f, SexyTopo.dpToPixels(dp));
    }

    static float spToPixels(Context context, float sp) {
        return sp * context.getResources().getDisplayMetrics().scaledDensity;
    }

    private static int resolveThemeColor(Context context, int attr) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }
}

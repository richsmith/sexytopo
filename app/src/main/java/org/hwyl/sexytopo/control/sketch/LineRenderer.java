package org.hwyl.sexytopo.control.sketch;

import static org.hwyl.sexytopo.control.sketch.ScreenUnits.dpToPixels;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PathEffect;
import androidx.core.content.ContextCompat;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.SexyTopo;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.LineType;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;

/**
 * Draws a sketch's lines. Plain freehand lines are batched for speed; typed lines are drawn one by
 * one with their type's ornamentation, and walls with their rock side shaded.
 *
 * <p>Ornamentation and shading are path effects sized by the zoom. They're cached alongside the
 * size they were built for and only rebuilt when that changes, not on every frame.
 */
class LineRenderer {

    private final Viewport viewport;

    // plain freehand lines
    private final Paint drawPaint = new Paint();
    // one paint per semantic line type (stroke width and ornamentation baked in), alongside the
    // on-screen ornament size each paint's effect was last built for, so the effect is only
    // rebuilt when that changes (i.e. on zoom)
    private final Map<LineType, Paint> lineTypePaints = new EnumMap<>(LineType.class);
    private final Map<LineType, Float> lineTypePaintOrnamentSizePixels =
            new EnumMap<>(LineType.class);
    // one paint per layer of wall rock-side shading
    private final Paint[] wallRockSidePaints = createWallRockSidePaints();
    private float wallRockSidePaintsOrnamentSizePixels = -1;

    // scratch object reused when drawing typed paths, to avoid per-frame allocation
    private final Path typedPathScratch = new Path();

    LineRenderer(Viewport viewport) {
        this.viewport = viewport;
    }

    /** Set the paints up from the theme and current preferences. */
    void initialise(Context context) {
        SketchPaints.applyAntiAliasPreference(drawPaint);

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
    }

    void draw(
            Canvas canvas,
            Sketch sketch,
            int alpha,
            boolean isDarkModeActive,
            boolean isDebugMode) {
        float viewOffsetX = viewport.getOffsetX();
        float viewOffsetY = viewport.getOffsetY();
        float viewScale = viewport.getScale();

        Colour lastColour = Colour.BLACK;

        drawPaint.setColor(lastColour.intValue);
        drawPaint.setAlpha(alpha);

        for (PathDetail pathDetail : sketch.getPathDetails()) {

            if (!viewport.couldBeVisible(pathDetail)) {
                continue;
            }

            if (pathDetail.getLineType() != LineType.SKETCH) {
                drawTypedPath(
                        canvas,
                        pathDetail,
                        alpha,
                        isDarkModeActive,
                        isFinishedWall(sketch, pathDetail));
                continue;
            }

            // Avoiding constantly updating the paint colour saves approx. 10% of sketch draw time.
            // Ideally getPathDetails() would return the paths in colour order but HashSets
            // are unordered collections
            Colour drawColour = pathDetail.getDrawColour(isDarkModeActive);
            if (drawColour != lastColour) {
                lastColour = drawColour;
                drawPaint.setColor(lastColour.intValue);
            }

            List<Coord2D> path = pathDetail.getPath();

            int lineIndex = 0;
            float fromX = -1, fromY = -1;
            float[] lines = new float[path.size() * 4];

            // This loop is the slowest part of the draw phase. Pulling out the calculations from
            // within Viewport.toView saves a not insignificant amount of time by not
            // constructing many thousands of Coord2D objects (approx. 10% of sketch draw time)
            for (Coord2D point : path) {
                if (fromX == -1) {
                    // from = viewport.toView(point);
                    fromX = (point.x - viewOffsetX) * viewScale;
                    fromY = (point.y - viewOffsetY) * viewScale;

                    if (isDebugMode) {
                        canvas.drawCircle(fromX, fromY, 3, drawPaint);
                    }
                } else {
                    // Coord2D to = viewport.toView(point);
                    float toX = (point.x - viewOffsetX) * viewScale;
                    float toY = (point.y - viewOffsetY) * viewScale;

                    lines[lineIndex++] = fromX;
                    lines[lineIndex++] = fromY;
                    lines[lineIndex++] = toX;
                    lines[lineIndex++] = toY;

                    if (isDebugMode) {
                        canvas.drawCircle(toX, toY, 3, drawPaint);
                    }

                    fromX = toX;
                    fromY = toY;
                }
            }

            canvas.drawLines(lines, drawPaint);
        }
    }

    /**
     * Semantic lines are drawn individually via drawPath so their type's ornamentation (dashes,
     * tick stamps) can apply. Ornamentation is sized by getPatternScale.
     */
    private void drawTypedPath(
            Canvas canvas,
            PathDetail pathDetail,
            int alpha,
            boolean isDarkModeActive,
            boolean shadeRockSide) {
        float viewOffsetX = viewport.getOffsetX();
        float viewOffsetY = viewport.getOffsetY();
        float viewScale = viewport.getScale();
        Paint paint = getLineTypePaint(pathDetail.getLineType(), viewport.getPatternScale());
        paint.setColor(pathDetail.getDrawColour(isDarkModeActive).intValue);
        paint.setAlpha(alpha);

        typedPathScratch.rewind();
        boolean first = true;
        for (Coord2D point : pathDetail.getPath()) {
            float x = (point.x - viewOffsetX) * viewScale;
            float y = (point.y - viewOffsetY) * viewScale;
            if (first) {
                typedPathScratch.moveTo(x, y);
                first = false;
            } else {
                typedPathScratch.lineTo(x, y);
            }
        }
        if (shadeRockSide) {
            drawRockSide(canvas, typedPathScratch, paint.getColor(), alpha);
        }
        canvas.drawPath(typedPathScratch, paint);
    }

    /** Shades the outer side of a wall line type. */
    private void drawRockSide(Canvas canvas, Path path, int colour, int alpha) {
        List<WallRockSide.Layer> layers = WallRockSide.getLayers();
        Paint[] layerPaints = getWallRockSidePaints(viewport.getPatternScale());
        for (int i = 0; i < layers.size(); i++) {
            Paint paint = layerPaints[i];
            paint.setColor(colour);
            paint.setAlpha(alpha * layers.get(i).getAlpha() / 255);
            canvas.drawPath(path, paint);
        }
    }

    /**
     * Whether a line can have its rock side shaded: any wall-kind line, excluding a stroke still in
     * progress (its orientation isn't decided until the stroke finishes). Ticked types need no
     * shading — their ornamentation already shows their orientation.
     */
    private static boolean isFinishedWall(Sketch sketch, PathDetail pathDetail) {
        return pathDetail.getLineType().isWall() && pathDetail != sketch.getActivePath();
    }

    private static float getOrnamentSizePixels(float patternScale) {
        return SexyTopo.dpToPixels(GeneralPreferences.getLineOrnamentSizeDp()) * patternScale;
    }

    /**
     * The paint for a semantic line type, its ornamentation sized for the given pattern scale.
     * Falls back to the plain sketch paint if the paints haven't been initialised yet.
     */
    private Paint getLineTypePaint(LineType lineType, float patternScale) {
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
    private Paint[] getWallRockSidePaints(float patternScale) {
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
}

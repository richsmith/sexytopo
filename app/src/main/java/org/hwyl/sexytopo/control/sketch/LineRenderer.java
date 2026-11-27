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
import org.hwyl.sexytopo.control.preferences.GeneralPreferences;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.LineDetail;
import org.hwyl.sexytopo.model.sketch.LineType;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.SketchDetail;

/**
 * Draws a sketch's lines. Plain freehand lines are batched for speed; typed lines are drawn one by
 * one with their type's ornamentation, and walls with their rock side shaded.
 *
 * <p>Ornamentation and shading are path effects sized by the zoom. They're cached alongside the
 * size they were built for and only rebuilt when that changes, not on every frame.
 */
class LineRenderer {

    // Points closer than this to the last one drawn are skipped, as they'd make no visible
    // difference; zoomed out, a long line can have many points to a pixel. Squared, to compare
    // against squared distances. The last point of a line is always drawn, so it ends in place.
    private static final float MIN_POINT_SPACING_PIXELS_SQUARED = 1;

    private final Viewport viewport;

    // plain freehand lines
    private final Paint drawPaint = new Paint();
    // one paint per semantic line type (stroke width and ornamentation baked in), alongside the
    // on-screen ornament size each paint's effect was last built for, so the effect is only
    // rebuilt when that changes (i.e. on zoom)
    private final Map<LineType, Paint> lineTypePaints = new EnumMap<>(LineType.class);
    private final Map<LineType, Float> lineTypePaintOrnamentSizePixels =
            new EnumMap<>(LineType.class);
    // the points of each line in debug mode: plain fill, as a typed line's paint has ornamentation
    private final Paint debugPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // one paint per layer of wall rock-side shading
    private final Paint[] wallRockSidePaints = createWallRockSidePaints();
    private float wallRockSidePaintsOrnamentSizePixels = -1;

    // the line ornament size preference in pixels, before scaling with the zoom; read once here, as
    // reading it is slow and it's needed for every typed line on every frame
    private float ornamentSizePixelsUnscaled;

    // scratch object reused when drawing typed paths, to avoid per-frame allocation
    private final Path typedPathScratch = new Path();
    // scratch arrays, grown as needed and reused to avoid allocating for every line on every frame:
    // a line's points on screen (x, y pairs) after thinning, and a plain line's segment end points
    private float[] pointsScratch = new float[0];
    private float[] linesScratch = new float[0];

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

        ornamentSizePixelsUnscaled =
                SexyTopo.dpToPixels(GeneralPreferences.getLineOrnamentSizeDp());

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
            boolean isDebugMode,
            SketchDetail hidden) {
        Colour lastColour = Colour.BLACK;

        drawPaint.setColor(lastColour.intValue);
        drawPaint.setAlpha(alpha);

        for (LineDetail lineDetail : sketch.getLineDetails()) {

            if (lineDetail == hidden || !viewport.couldBeVisible(lineDetail)) {
                continue;
            }

            if (lineDetail.getLineType() != LineType.SKETCH) {
                drawTypedPath(
                        canvas,
                        lineDetail,
                        alpha,
                        isDarkModeActive,
                        isFinishedWall(sketch, lineDetail),
                        isDebugMode);
                continue;
            }

            // Avoiding constantly updating the paint colour saves approx. 10% of sketch draw time.
            // Ideally getLineDetails() would return the lines in colour order but HashSets
            // are unordered collections
            Colour drawColour = lineDetail.getDrawColour(isDarkModeActive);
            if (drawColour != lastColour) {
                lastColour = drawColour;
                // setColor replaces the alpha too, so a faded sketch has to be faded again
                drawPaint.setColor(lastColour.intValue);
                drawPaint.setAlpha(alpha);
            }

            int pointValues = toPointsOnScreen(lineDetail.getPath());
            float[] points = pointsScratch;

            // four values (the start and end x and y) for each segment between points
            int segmentValues = Math.max(0, pointValues - 2) * 2;
            if (linesScratch.length < segmentValues) {
                linesScratch = new float[Math.max(segmentValues, linesScratch.length * 2)];
            }
            float[] lines = linesScratch;

            int lineIndex = 0;
            for (int i = 2; i < pointValues; i += 2) {
                lines[lineIndex++] = points[i - 2];
                lines[lineIndex++] = points[i - 1];
                lines[lineIndex++] = points[i];
                lines[lineIndex++] = points[i + 1];
            }
            canvas.drawLines(lines, 0, lineIndex, drawPaint);

            if (isDebugMode) {
                drawDebugPoints(canvas, pointValues, drawPaint);
            }
        }
    }

    /**
     * Draws a single line on its own, such as one being reshaped, with its ornamentation and any
     * rock-side shading.
     */
    void drawLine(Canvas canvas, LineDetail lineDetail, int alpha, boolean isDarkModeActive) {
        if (lineDetail.getLineType() != LineType.SKETCH) {
            drawTypedPath(
                    canvas,
                    lineDetail,
                    alpha,
                    isDarkModeActive,
                    lineDetail.getLineType().isWall(),
                    false);
            return;
        }
        drawPaint.setColor(lineDetail.getDrawColour(isDarkModeActive).intValue);
        drawPaint.setAlpha(alpha);
        int pointValues = toPointsOnScreen(lineDetail.getPath());
        float[] points = pointsScratch;
        typedPathScratch.rewind();
        if (pointValues > 0) {
            typedPathScratch.moveTo(points[0], points[1]);
        }
        for (int i = 2; i < pointValues; i += 2) {
            typedPathScratch.lineTo(points[i], points[i + 1]);
        }
        canvas.drawPath(typedPathScratch, drawPaint);
    }

    /**
     * Semantic lines are drawn individually via drawPath so their type's ornamentation (dashes,
     * tick stamps) can apply. Ornamentation is sized by getPatternScale.
     */
    private void drawTypedPath(
            Canvas canvas,
            LineDetail lineDetail,
            int alpha,
            boolean isDarkModeActive,
            boolean shadeRockSide,
            boolean isDebugMode) {
        Paint paint = getLineTypePaint(lineDetail.getLineType(), viewport.getPatternScale());
        paint.setColor(lineDetail.getDrawColour(isDarkModeActive).intValue);
        paint.setAlpha(alpha);

        int pointValues = toPointsOnScreen(lineDetail.getPath());
        float[] points = pointsScratch;

        typedPathScratch.rewind();
        if (pointValues > 0) {
            typedPathScratch.moveTo(points[0], points[1]);
        }
        for (int i = 2; i < pointValues; i += 2) {
            typedPathScratch.lineTo(points[i], points[i + 1]);
        }
        if (shadeRockSide) {
            drawRockSide(canvas, typedPathScratch, paint.getColor(), alpha);
        }
        canvas.drawPath(typedPathScratch, paint);

        if (isDebugMode) {
            drawDebugPoints(canvas, pointValues, paint);
        }
    }

    /**
     * Marks each of the points in pointsScratch (the first pointValues values) with a dot in the
     * line's colour, twice the line's width across so it stands out from the line.
     */
    private void drawDebugPoints(Canvas canvas, int pointValues, Paint linePaint) {
        debugPointPaint.setColor(linePaint.getColor());
        float radius = linePaint.getStrokeWidth();
        float[] points = pointsScratch;
        for (int i = 0; i < pointValues; i += 2) {
            canvas.drawCircle(points[i], points[i + 1], radius, debugPointPaint);
        }
    }

    /**
     * Put a line's points on screen into pointsScratch as x, y pairs, skipping any too close to the
     * last one kept to make a visible difference (but always keeping the first and last). Returns
     * how many values were written: twice the number of points kept.
     */
    private int toPointsOnScreen(List<Coord2D> path) {
        if (pointsScratch.length < path.size() * 2) {
            pointsScratch = new float[Math.max(path.size() * 2, pointsScratch.length * 2)];
        }
        float[] points = pointsScratch;

        float viewOffsetX = viewport.getOffsetX();
        float viewOffsetY = viewport.getOffsetY();
        float viewScale = viewport.getScale();

        int lastPointIndex = path.size() - 1;
        int pointIndex = 0;
        int pointValues = 0;

        // This loop is the slowest part of the draw phase. Pulling out the calculations from
        // within Viewport.toView saves a not insignificant amount of time by not constructing many
        // thousands of Coord2D objects (approx. 10% of sketch draw time)
        for (Coord2D point : path) {
            // Coord2D onScreen = viewport.toView(point);
            float x = (point.x - viewOffsetX) * viewScale;
            float y = (point.y - viewOffsetY) * viewScale;

            boolean keep = pointIndex == 0 || pointIndex == lastPointIndex;
            if (!keep) {
                float dx = x - points[pointValues - 2];
                float dy = y - points[pointValues - 1];
                keep = dx * dx + dy * dy >= MIN_POINT_SPACING_PIXELS_SQUARED;
            }
            if (keep) {
                points[pointValues++] = x;
                points[pointValues++] = y;
            }
            pointIndex++;
        }
        return pointValues;
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
    private static boolean isFinishedWall(Sketch sketch, LineDetail lineDetail) {
        return lineDetail.getLineType().isWall() && lineDetail != sketch.getActivePath();
    }

    private float getOrnamentSizePixels(float patternScale) {
        return ornamentSizePixelsUnscaled * patternScale;
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

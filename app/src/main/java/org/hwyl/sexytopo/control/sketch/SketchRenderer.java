package org.hwyl.sexytopo.control.sketch;

import static org.hwyl.sexytopo.control.sketch.SketchPaints.dpToPixels;

import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RotateDrawable;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.hwyl.sexytopo.control.util.SketchPreferences;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.sketch.AreaDetail;
import org.hwyl.sexytopo.model.sketch.AreaType;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.LineType;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.SketchDetail;
import org.hwyl.sexytopo.model.sketch.Symbol;
import org.hwyl.sexytopo.model.sketch.SymbolDetail;
import org.hwyl.sexytopo.model.sketch.TextDetail;

/**
 * Draws a sketch's areas, lines, text and symbols. Used for the main sketch, for linked surveys'
 * sketches, and for each cross-section's own sketch.
 */
class SketchRenderer {

    private final Viewport viewport;
    private final SketchPaints paints;
    // whether to mark each point of each line, as an aid to debugging
    private final BooleanSupplier isDebugMode;

    private boolean isDarkModeActive = false;

    // scratch object reused when drawing typed paths, to avoid per-frame allocation
    private final Path typedPathScratch = new Path();
    // scratch object reused to position the fill pattern, to avoid per-frame allocation
    private final Matrix areaFillMatrix = new Matrix();

    SketchRenderer(Viewport viewport, SketchPaints paints, BooleanSupplier isDebugMode) {
        this.viewport = viewport;
        this.paints = paints;
        this.isDebugMode = isDebugMode;
    }

    /** In dark mode, details drawn in black are drawn in white instead. */
    void setDarkModeActive(boolean isDarkModeActive) {
        this.isDarkModeActive = isDarkModeActive;
    }

    void draw(Canvas canvas, Sketch sketch, int alpha) {
        float viewOffsetX = viewport.getOffsetX();
        float viewOffsetY = viewport.getOffsetY();
        float viewScale = viewport.getScale();

        if (!SketchPreferences.Toggle.SHOW_SKETCH.isOn()) {
            return;
        }

        drawAreas(canvas, sketch, alpha);

        Colour lastColour = Colour.BLACK;

        paints.drawPaint.setColor(lastColour.intValue);
        paints.drawPaint.setAlpha(alpha);

        boolean isDebugMode = this.isDebugMode.getAsBoolean();

        for (PathDetail pathDetail : sketch.getPathDetails()) {

            if (!viewport.couldBeVisible(pathDetail)) {
                continue;
            }

            if (pathDetail.getLineType() != LineType.SKETCH) {
                drawTypedPath(canvas, pathDetail, alpha, isFinishedWall(sketch, pathDetail));
                continue;
            }

            // Avoiding constantly updating the paint colour saves approx. 10% of sketch draw time.
            // Ideally getPathDetails() would return the paths in colour order but HashSets
            // are unordered collections
            Colour drawColour = pathDetail.getDrawColour(isDarkModeActive);
            if (drawColour != lastColour) {
                lastColour = drawColour;
                paints.drawPaint.setColor(lastColour.intValue);
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
                        canvas.drawCircle(fromX, fromY, 3, paints.drawPaint);
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
                        canvas.drawCircle(toX, toY, 3, paints.drawPaint);
                    }

                    fromX = toX;
                    fromY = toY;
                }
            }

            canvas.drawLines(lines, paints.drawPaint);
        }

        paints.labelPaint.setAlpha(alpha);

        for (TextDetail textDetail : sketch.getTextDetails()) {
            Coord2D location = viewport.toView(textDetail.getPosition());
            float x = location.x, y = location.y;
            String text = textDetail.getText();
            setDrawColour(paints.labelPaint, textDetail);
            paints.labelPaint.setTextSize(textDetail.getSize() * viewScale);
            for (String line : text.split("\n")) {
                canvas.drawText(line, x, y, paints.labelPaint);
                y += paints.labelPaint.descent() - paints.labelPaint.ascent();
            }
        }

        for (SymbolDetail symbolDetail : sketch.getSymbolDetails()) {
            if (!viewport.couldBeVisible(symbolDetail)) {
                continue;
            }
            Coord2D location = viewport.toView(symbolDetail.getPosition());

            Symbol symbol = symbolDetail.getSymbol();

            int size = Math.round(symbolDetail.getSize() * viewScale);
            if (size < 1) {
                continue;
            }

            Drawable drawable = symbolDetail.getDrawable();
            float offset = size / 2f;
            int x = Math.round(location.x - offset), y = Math.round(location.y - offset);
            drawable.setBounds(x, y, x + size, y + size);
            drawable.setAlpha(alpha);
            Colour drawColour = symbolDetail.getDrawColour(isDarkModeActive);
            drawable.setColorFilter(
                    new PorterDuffColorFilter(drawColour.intValue, PorterDuff.Mode.SRC_IN));

            if (symbol.isDirectional()) {
                RotateDrawable rotateDrawable = new RotateDrawable();
                rotateDrawable.setDrawable(drawable);
                rotateDrawable.setPivotX(0.5f);
                rotateDrawable.setPivotY(0.5f);
                rotateDrawable.setLevel((int) (10000 * (symbolDetail.getAngle() / 360f)));
                rotateDrawable.setBounds(x, y, x + size, y + size);
                rotateDrawable.draw(canvas);

            } else { // skip some calcs for efficiency
                drawable.draw(canvas);
            }
        }
    }

    /**
     * Semantic lines are drawn individually via drawPath so their type's ornamentation (dashes,
     * tick stamps) can apply. Ornamentation is sized by getPatternScale.
     */
    private void drawTypedPath(
            Canvas canvas, PathDetail pathDetail, int alpha, boolean shadeRockSide) {
        float viewOffsetX = viewport.getOffsetX();
        float viewOffsetY = viewport.getOffsetY();
        float viewScale = viewport.getScale();
        Paint paint =
                paints.getLineTypePaint(
                        pathDetail.getLineType(), SketchPaints.getPatternScale(viewScale));
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
        Paint[] layerPaints =
                paints.getWallRockSidePaints(SketchPaints.getPatternScale(viewport.getScale()));
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

    /**
     * Draw the sketch's areas: each region is filled with its type's pattern and outlined. Holes
     * are drawn as further contours of the same path under the even-odd fill rule, so they are
     * outlined too but left unfilled.
     *
     * <p>The pattern is anchored to survey space and sized by getPatternScale, so it stays put on
     * the cave as the view is panned or zoomed. Every area of a type shares the same pattern, so
     * adjacent areas join seamlessly.
     */
    private void drawAreas(Canvas canvas, Sketch sketch, int alpha) {
        float viewOffsetX = viewport.getOffsetX();
        float viewOffsetY = viewport.getOffsetY();
        float viewScale = viewport.getScale();

        float patternScale = SketchPaints.getPatternScale(viewScale);
        areaFillMatrix.setScale(
                patternScale / SketchPaints.AREA_FILL_OVERSAMPLE,
                patternScale / SketchPaints.AREA_FILL_OVERSAMPLE);
        areaFillMatrix.postTranslate(-viewOffsetX * viewScale, -viewOffsetY * viewScale);

        for (AreaDetail areaDetail : sketch.getAreaDetails()) {

            if (!viewport.couldBeVisible(areaDetail)) {
                continue;
            }

            Path outline = new Path();
            outline.setFillType(Path.FillType.EVEN_ODD);
            for (List<Coord2D> contour : areaDetail.getContours()) {
                boolean first = true;
                for (Coord2D point : contour) {
                    float x = (point.x - viewOffsetX) * viewScale;
                    float y = (point.y - viewOffsetY) * viewScale;
                    if (first) {
                        outline.moveTo(x, y);
                        first = false;
                    } else {
                        outline.lineTo(x, y);
                    }
                }
                outline.close();
            }

            Colour colour = areaDetail.getDrawColour(isDarkModeActive);

            // the fill tile is an alpha mask, so it takes the paint's colour
            BitmapShader shader = paints.getAreaFillShader(areaDetail.getAreaType());
            shader.setLocalMatrix(areaFillMatrix);
            paints.areaFillPaint.setShader(shader);
            paints.areaFillPaint.setColor(colour.intValue);
            paints.areaFillPaint.setAlpha(alpha);
            canvas.drawPath(outline, paints.areaFillPaint);

            paints.areaPaint.setColor(colour.intValue);
            if (areaDetail.getAreaType().hasDefinedEdge()) {
                paints.areaPaint.setAlpha(alpha);
                paints.areaPaint.setStrokeWidth(paints.drawPaint.getStrokeWidth());
            } else {
                paints.areaPaint.setAlpha(Math.round(alpha * AreaType.LIGHT_OUTLINE_OPACITY));
                paints.areaPaint.setStrokeWidth(dpToPixels(AreaType.Fill.STROKE_WIDTH_DP));
            }
            canvas.drawPath(outline, paints.areaPaint);
        }
    }

    private void setDrawColour(Paint paint, SketchDetail sketchDetail) {
        Colour colour = sketchDetail.getDrawColour(isDarkModeActive);
        paint.setColor(colour.intValue);
    }
}

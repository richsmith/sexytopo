package org.hwyl.sexytopo.control.sketch;

import static org.hwyl.sexytopo.control.sketch.SketchPaints.dpToPixels;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.sketch.AreaDetail;
import org.hwyl.sexytopo.model.sketch.AreaType;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.Sketch;

/**
 * Draws a sketch's areas: each region is filled with its type's pattern and outlined. Holes are
 * drawn as further contours of the same path under the even-odd fill rule, so they are outlined too
 * but left unfilled.
 *
 * <p>The pattern is anchored to survey space and sized by getPatternScale, so it stays put on the
 * cave as the view is panned or zoomed. Every area of a type shares the same pattern, so adjacent
 * areas join seamlessly.
 */
class AreaRenderer {

    // fill tiles are rendered at this multiple of their on-screen size and scaled down to draw
    private static final int AREA_FILL_OVERSAMPLE = 2;

    private final Viewport viewport;

    private final Paint areaPaint = new Paint();
    // fills areas with their type's pattern; the shader is swapped per area type
    private final Paint areaFillPaint = new Paint();
    private final Map<AreaType, BitmapShader> areaFillShaders = new EnumMap<>(AreaType.class);
    // scratch object reused to position the fill pattern, to avoid per-frame allocation
    private final Matrix areaFillMatrix = new Matrix();

    // an area with a defined edge is outlined as heavily as a sketch line
    private float definedEdgeWidth;

    AreaRenderer(Viewport viewport) {
        this.viewport = viewport;
    }

    /** Set the paints and fill patterns up from the current preferences. */
    void initialise() {
        SketchPaints.applyAntiAliasPreference(areaPaint);

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

        definedEdgeWidth = dpToPixels(GeneralPreferences.getSketchLineWidthDp());
    }

    void draw(Canvas canvas, Sketch sketch, int alpha, boolean isDarkModeActive) {
        float viewOffsetX = viewport.getOffsetX();
        float viewOffsetY = viewport.getOffsetY();
        float viewScale = viewport.getScale();

        float patternScale = SketchPaints.getPatternScale(viewScale);
        areaFillMatrix.setScale(
                patternScale / AREA_FILL_OVERSAMPLE, patternScale / AREA_FILL_OVERSAMPLE);
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
            BitmapShader shader = areaFillShaders.get(areaDetail.getAreaType());
            shader.setLocalMatrix(areaFillMatrix);
            areaFillPaint.setShader(shader);
            areaFillPaint.setColor(colour.intValue);
            areaFillPaint.setAlpha(alpha);
            canvas.drawPath(outline, areaFillPaint);

            areaPaint.setColor(colour.intValue);
            if (areaDetail.getAreaType().hasDefinedEdge()) {
                areaPaint.setAlpha(alpha);
                areaPaint.setStrokeWidth(definedEdgeWidth);
            } else {
                areaPaint.setAlpha(Math.round(alpha * AreaType.LIGHT_OUTLINE_OPACITY));
                areaPaint.setStrokeWidth(dpToPixels(AreaType.Fill.STROKE_WIDTH_DP));
            }
            canvas.drawPath(outline, areaPaint);
        }
    }

    /**
     * Render one tile of an area fill pattern into an alpha mask, to be repeated by a shader. The
     * tile is rounded to a whole number of pixels so the repeats line up exactly, and rendered
     * oversampled so it stays crisp when scaled up between zoom octaves (see draw).
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
}

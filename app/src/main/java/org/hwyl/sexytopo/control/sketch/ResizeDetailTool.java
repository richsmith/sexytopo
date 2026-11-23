package org.hwyl.sexytopo.control.sketch;

import static org.hwyl.sexytopo.control.sketch.ScreenUnits.dpToPixels;

import android.graphics.RectF;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.sketch.SinglePositionDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.SketchTool;

/**
 * Resizes a symbol or piece of text: dragging up makes it bigger and dragging down smaller. The
 * size changes by the same factor for the same distance whatever the zoom, doubling or halving
 * every DOUBLING_DISTANCE_DP. A symbol keeps its centre and text its starting point.
 */
class ResizeDetailTool extends DragDetailTool {

    private static final float DOUBLING_DISTANCE_DP = 100;

    // stops anything being shrunk too small to see or long-press again
    private static final float MINIMUM_SIZE_DP = 8;

    ResizeDetailTool(
            Viewport viewport, SketchState state, SketchRenderer sketchRenderer, ToolHost host) {
        super(
                viewport,
                state,
                sketchRenderer,
                host,
                SketchTool.RESIZE_DETAIL,
                R.string.sketch_resize_detail_instruction);
    }

    @Override
    SinglePositionDetail drag(SinglePositionDetail detail, Coord2D from, Coord2D to) {
        float scale = getScale(detail, from, to);
        return scale == 1 ? detail : detail.scale(scale);
    }

    @Override
    void commit(Sketch sketch, SinglePositionDetail detail, Coord2D from, Coord2D to) {
        float scale = getScale(detail, from, to);
        if (scale != 1) {
            sketch.resizeDetail(detail, scale);
        }
    }

    private float getScale(SinglePositionDetail detail, Coord2D from, Coord2D to) {
        RectF bounds = sketchRenderer.getBoundsOnView(detail);
        float currentSize = Math.max(bounds.width(), bounds.height());
        return calculateScale(
                from.y - to.y,
                dpToPixels(DOUBLING_DISTANCE_DP),
                currentSize,
                dpToPixels(MINIMUM_SIZE_DP));
    }

    /**
     * The factor to scale by for a drag the given distance up the screen (negative for down). It
     * won't shrink something below the minimum size, but something already smaller than that can
     * still be made bigger.
     */
    static float calculateScale(
            float upwards, float doublingDistance, float currentSize, float minimumSize) {
        float scale = (float) Math.pow(2, upwards / doublingDistance);
        if (currentSize <= 0) {
            return scale;
        }
        float smallestScale = Math.min(1, minimumSize / currentSize);
        return Math.max(scale, smallestScale);
    }
}

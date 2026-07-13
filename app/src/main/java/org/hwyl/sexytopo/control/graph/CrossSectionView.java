package org.hwyl.sexytopo.control.graph;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Line;

/**
 * Overrides parent provide a slightly more minimal editing experience for the cross-section sketch
 * view.
 */
public class CrossSectionView extends GraphView {

    /** Fraction of the smaller screen dimension that the longest splay should occupy. */
    private static final float AUTO_FIT_SCREEN_FRACTION = 0.4f;

    /** Fallback zoom when there are no splays to measure. */
    private static final float DEFAULT_FALLBACK_ZOOM = 60.0f;

    private boolean autoFitted = false;

    public CrossSectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void checkForChangedSurvey() {
        if (getWidth() > 0 && getHeight() > 0) {
            if (!autoFitted) {
                autoFitZoom();
                autoFitted = true;
            }
            centreViewOnSurveyPoint(Coord2D.ORIGIN);
        }
    }

    /**
     * Pick a zoom level so the longest splay in the projection occupies AUTO_FIT_SCREEN_FRACTION of
     * the smaller screen dimension.
     */
    private void autoFitZoom() {
        if (projection == null) {
            return;
        }
        float longestSplay = 0f;
        for (Line<Coord2D> line : projection.getLegMap().values()) {
            float length = Space2DUtils.getDistance(line.getStart(), line.getEnd());
            if (length > longestSplay) {
                longestSplay = length;
            }
        }
        if (longestSplay <= 0f) {
            setZoom(DEFAULT_FALLBACK_ZOOM);
            return;
        }
        float smallerScreenDim = Math.min(getWidth(), getHeight());
        float targetPixels = smallerScreenDim * AUTO_FIT_SCREEN_FRACTION;
        setZoom(targetPixels / longestSplay);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        updateViewBounds();

        drawGrid(canvas);
        drawSurvey(canvas, survey, projection, SOLID_ALPHA);
        drawLegend(canvas);
    }

    protected ViewContext getViewContext() {
        return ViewContext.CROSS_SECTION;
    }
}

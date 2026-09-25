package org.hwyl.sexytopo.control.graph;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.survey.Leg;

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

    private CrossSection.Orientation orientation = CrossSection.Orientation.VERTICAL;

    // Decides which legs count as being in the plane of the section (and so are drawn solid). A
    // vertical section is judged by the direction of its splays across the passage; a horizontal
    // one by how level they are.
    private Projection2D legPlaneProjection = Projection2D.CROSS_SECTION;

    public CrossSectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** Tells the view which cross-section is being edited, so it can show it appropriately. */
    public void setCrossSection(CrossSection crossSection) {
        this.orientation = crossSection.getOrientation();
        this.legPlaneProjection = crossSection.getProjectionType();
        invalidate();
    }

    /** A horizontal section is drawn like the plan, with north at the top, so it has a compass. */
    @Override
    protected boolean isCompassShown() {
        return orientation == CrossSection.Orientation.HORIZONTAL;
    }

    @Override
    protected boolean isLegInPlane(Leg leg) {
        return legPlaneProjection.isLegInPlane(leg);
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
        drawCompass(canvas);
    }

    protected ViewContext getViewContext() {
        return ViewContext.CROSS_SECTION;
    }
}

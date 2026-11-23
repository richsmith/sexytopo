package org.hwyl.sexytopo.control.sketch;

import org.hwyl.sexytopo.model.common.Shape;
import org.hwyl.sexytopo.model.geometry.Coord2D;

/**
 * Which part of the survey a sketch view is showing, and at what zoom. Maps between survey
 * coordinates (metres) and view coordinates (pixels).
 *
 * <p>The offset is the survey point at the top-left of the view; the scale is pixels per metre. The
 * visible bounds are cached by updateBounds, which the view calls once per frame, so culling checks
 * during drawing don't recompute them.
 *
 * <p>Hot drawing loops shouldn't call toView per point: read getOffsetX, getOffsetY and getScale
 * into locals once and apply the transform inline, to avoid allocating a Coord2D for every point.
 */
public class Viewport {

    public static final float MIN_ZOOM = 0.1f;
    public static final float MAX_ZOOM = 500.0f;
    public static final float DEFAULT_SCALE = 60.0f;

    // the most a dp of line ornamentation or area fill can stand for in the cave, so zoomed far
    // out a decoration can't dwarf what it decorates (the default 12dp tick comes to 0.6m)
    private static final float MAX_PATTERN_METRES_PER_DP = 0.05f;

    private Coord2D offset = Coord2D.ORIGIN;
    private float scale = DEFAULT_SCALE;

    private Coord2D canvasBottomRight = Coord2D.ORIGIN;
    private Coord2D topLeftOnSurvey = Coord2D.ORIGIN;
    private Coord2D bottomRightOnSurvey = Coord2D.ORIGIN;

    // whether this is an inset in another view; if so, it keeps the pattern scale of the view it's
    // in, so its ornamentation and fills match what's around it
    private boolean isInset = false;
    private float insetPatternScale;

    public Coord2D getOffset() {
        return offset;
    }

    public float getOffsetX() {
        return offset.x;
    }

    public float getOffsetY() {
        return offset.y;
    }

    public void setOffset(Coord2D offset) {
        this.offset = offset;
    }

    /** Pixels per metre. */
    public float getScale() {
        return scale;
    }

    /** A length on screen, given in dp, as metres in the survey at the current zoom. */
    public float dpToSurvey(float dp) {
        return ScreenUnits.dpToPixels(dp) / scale;
    }

    /**
     * How much to scale line ornamentation and area fills from their designed size, so they zoom
     * with the cave instead of crawling across it.
     */
    public float getPatternScale() {
        if (isInset) {
            return insetPatternScale;
        }
        // re-pick the density each time the zoom doubles or halves, so it never strays more than
        // √2 from its designed size; doubling keeps existing marks in place and adds new ones
        double octave = Math.round(Math.log(scale) / Math.log(2));
        float patternScale = (float) (scale / Math.pow(2, octave));
        // but zoomed far out, don't let a tick dwarf the line it's on
        float maxScale = MAX_PATTERN_METRES_PER_DP * scale / ScreenUnits.dpToPixels(1);
        return Math.min(patternScale, maxScale);
    }

    public Coord2D toSurvey(final Coord2D coords) {
        // The more elegant way to do this is:
        // return coords.scale(1 / scale).plus(offset);
        // ...but this method gets hit hard (profiled) so let's avoid creating intermediate objects:
        return new Coord2D(
                ((coords.x * (1 / scale)) + offset.x), ((coords.y * (1 / scale)) + offset.y));
    }

    // Warning: In tight loops during the draw phase we duplicate this logic to avoid
    //          creating too many Coord2D objects - be sure to mirror any updates in those places
    public Coord2D toView(final Coord2D coords) {
        // The more elegant way to do this is:
        // return coords.minus(offset).scale(scale);
        // ...but this method gets hit hard (profiled) so let's avoid creating intermediate objects:
        return new Coord2D(((coords.x - offset.x) * scale), ((coords.y - offset.y) * scale));
    }

    /**
     * Zoom to a new scale, keeping the survey point under focusOnView where it is on screen. Scales
     * at or beyond MIN_ZOOM or MAX_ZOOM are ignored.
     */
    public void setZoom(float newScale, Coord2D focusOnView) {

        if (MIN_ZOOM >= newScale || newScale >= MAX_ZOOM) {
            return;
        }

        Coord2D focusInSurveyCoords = toSurvey(focusOnView);

        Coord2D delta = focusInSurveyCoords.minus(offset);

        Coord2D scaledDelta = delta.scale(scale / newScale);
        offset = focusInSurveyCoords.minus(scaledDelta);

        scale = newScale;
    }

    /** Pan so that the given survey point is in the middle of a view of the given size. */
    public void centreOn(Coord2D point, int viewWidth, int viewHeight) {

        float xDeltaInMetres = (viewWidth / 2f) / scale;
        float yDeltaInMetres = (viewHeight / 2f) / scale;

        float x = point.x - xDeltaInMetres;
        float y = point.y - yDeltaInMetres;

        offset = new Coord2D(x, y);
    }

    /** Recompute the visible area for a view of the given size, at the current offset and zoom. */
    public void updateBounds(int viewWidth, int viewHeight) {
        canvasBottomRight = new Coord2D(viewWidth, viewHeight);
        topLeftOnSurvey = toSurvey(Coord2D.ORIGIN);
        bottomRightOnSurvey = toSurvey(canvasBottomRight);
    }

    /**
     * Make this an inset in the parent's view: something drawn in coordinates of its own, scaled by
     * insetScale and placed with its origin at origin in the parent, as a cross-section's sketch
     * is. A point p is drawn where p * insetScale + origin would be in the parent, so the inset can
     * be drawn without making a moved and scaled copy of it. Call this after the parent's bounds
     * are up to date; insetScale must be positive. Line ornamentation and area fills keep the
     * parent's size, so they match those around the inset.
     */
    public void setAsInsetOf(Viewport parent, Coord2D origin, float insetScale) {
        offset = parent.offset.minus(origin).scale(1 / insetScale);
        scale = parent.scale * insetScale;
        isInset = true;
        insetPatternScale = parent.getPatternScale();
        canvasBottomRight = parent.canvasBottomRight;
        topLeftOnSurvey = toSurvey(Coord2D.ORIGIN);
        bottomRightOnSurvey = toSurvey(canvasBottomRight);
    }

    /** Whether a rectangle in view coordinates overlaps the visible area. */
    public boolean isRectOnCanvas(float left, float top, float right, float bottom) {
        return right >= 0
                && left <= canvasBottomRight.x
                && bottom >= 0
                && top <= canvasBottomRight.y;
    }

    /**
     * Returns true if a shape could be visible in the current view — i.e. its bounding box
     * intersects the visible area and it is large enough to occupy at least one pixel at the
     * current zoom level. Shapes that fail either check can be skipped without drawing.
     */
    public boolean couldBeVisible(Shape shape) {
        boolean possiblyOnScreen = shape.intersectsRectangle(topLeftOnSurvey, bottomRightOnSurvey);
        boolean bigEnough = shape.couldBeVisibleAtScale(scale);

        return (possiblyOnScreen && bigEnough);
    }
}

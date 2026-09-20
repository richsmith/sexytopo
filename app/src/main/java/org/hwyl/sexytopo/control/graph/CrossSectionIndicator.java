package org.hwyl.sexytopo.control.graph;

import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.ExtendedElevationDirection;
import org.hwyl.sexytopo.model.sketch.CrossSection;

/**
 * The little mark drawn over a station to show that it has a cross-section: a line representing the
 * plane of the section seen edge-on, with a flag showing which way the section faces. This holds
 * only the geometry, in view coordinates, so that it can be worked out and tested without a canvas.
 */
public final class CrossSectionIndicator {

    private static final float ARROW_LENGTH_FRACTION = 0.4f;
    private static final float ARROW_INNER_OFFSET_FRACTION = 0.05f;

    // Angles on the elevation, where the page is read like a compass rose: 0 is up the page
    private static final float FACING_RIGHT = 90f;
    private static final float FACING_DOWN = 180f;
    private static final float FACING_LEFT = 270f;

    private final float startX;
    private final float startY;
    private final float endX;
    private final float endY;

    // innerX, innerY, outerX, outerY, tipX, tipY
    private final float[] arrowhead;

    private CrossSectionIndicator(
            float startX, float startY, float endX, float endY, float[] arrowhead) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.arrowhead = arrowhead;
    }

    /**
     * An indicator for a section facing the given angle: a line of the given length through (x, y)
     * running across the direction the section faces, with an arrowhead at the end on the left as
     * you look that way, pointing the way the section faces.
     *
     * <p>On the plan the angle is a compass bearing, with 0 up the page. On the elevation there are
     * no bearings, but the page can be read the same way; see getElevationFacingAngle.
     */
    public static CrossSectionIndicator atAngle(
            float x, float y, float length, float angleDegrees) {

        float angle = (float) Math.toRadians(angleDegrees);
        float startX = x - ((length / 2) * (float) Math.cos(angle));
        float startY = y - ((length / 2) * (float) Math.sin(angle));
        float endX = x + ((length / 2) * (float) Math.cos(angle));
        float endY = y + ((length / 2) * (float) Math.sin(angle));

        float lineLength =
                Space2DUtils.getDistance(new Coord2D(startX, startY), new Coord2D(endX, endY));
        float arrowLength = lineLength * ARROW_LENGTH_FRACTION;
        float innerX =
                startX + ((lineLength * ARROW_INNER_OFFSET_FRACTION) * (float) Math.cos(angle));
        float innerY =
                startY + ((lineLength * ARROW_INNER_OFFSET_FRACTION) * (float) Math.sin(angle));
        float arrowAngle = (float) Math.toRadians(Space2DUtils.adjustAngle(angleDegrees, -90));
        float tipX = startX + (arrowLength * (float) Math.cos(arrowAngle));
        float tipY = startY + (arrowLength * (float) Math.sin(arrowAngle));

        return new CrossSectionIndicator(
                startX,
                startY,
                endX,
                endY,
                new float[] {innerX, innerY, startX, startY, tipX, tipY});
    }

    /**
     * The angle to give atAngle for a cross-section on the elevation, where the page is read like a
     * compass rose: up is 0, right is 90, down is 180 and left is 270.
     *
     * <p>A vertical section faces along the survey, which on the elevation is to the right or to
     * the left. A horizontal section is looked at from above, so it faces down the page.
     *
     * @param surveyDirection the way the survey is heading at the section's station; VERTICAL,
     *     which isn't a way the survey can head, is treated as the default, to the right
     */
    public static float getElevationFacingAngle(
            CrossSection.Orientation orientation, ExtendedElevationDirection surveyDirection) {
        if (orientation == CrossSection.Orientation.HORIZONTAL) {
            return FACING_DOWN;
        }
        return surveyDirection == ExtendedElevationDirection.LEFT ? FACING_LEFT : FACING_RIGHT;
    }

    public float getStartX() {
        return startX;
    }

    public float getStartY() {
        return startY;
    }

    public float getEndX() {
        return endX;
    }

    public float getEndY() {
        return endY;
    }

    public float getArrowInnerX() {
        return arrowhead[0];
    }

    public float getArrowInnerY() {
        return arrowhead[1];
    }

    public float getArrowOuterX() {
        return arrowhead[2];
    }

    public float getArrowOuterY() {
        return arrowhead[3];
    }

    public float getArrowTipX() {
        return arrowhead[4];
    }

    public float getArrowTipY() {
        return arrowhead[5];
    }
}

package org.hwyl.sexytopo.control.sketch;

import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.graph.Coord2D;

/**
 * The little mark drawn over a station to show that it has a cross-section: a line representing the
 * plane of the section seen edge-on, with a flag showing which way the section faces. This holds
 * only the geometry, in view coordinates, so that it can be worked out and tested without a canvas.
 */
public final class CrossSectionIndicator {

    private static final float ARROW_LENGTH_FRACTION = 0.4f;
    private static final float ARROW_INNER_OFFSET_FRACTION = 0.05f;

    // Angles on the elevation for atAngle, where the page is read like a compass rose: 0 is up
    private static final float FACING_RIGHT = 90f;
    private static final float FACING_DOWN = 180f;

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
     * no bearings; see onElevation and lookingDown.
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
     * An indicator for a vertical section on the elevation: an upright line through (x, y), the
     * section seen edge-on, with the arrowhead always at the top, pointing the way it faces.
     */
    public static CrossSectionIndicator onElevation(
            float x, float y, float length, boolean facingRight) {
        // Facing right, the arrowhead is on the top end already, so facing left is its mirror image
        CrossSectionIndicator right = atAngle(x, y, length, FACING_RIGHT);
        return facingRight ? right : right.mirroredAbout(x);
    }

    /**
     * An indicator for a horizontal section on the elevation: a level line through (x, y), with an
     * arrowhead pointing down, since the section is looked at from above.
     */
    public static CrossSectionIndicator lookingDown(float x, float y, float length) {
        return atAngle(x, y, length, FACING_DOWN);
    }

    private CrossSectionIndicator mirroredAbout(float x) {
        float[] mirroredArrowhead = arrowhead.clone();
        for (int i = 0; i < mirroredArrowhead.length; i += 2) {
            mirroredArrowhead[i] = 2 * x - mirroredArrowhead[i];
        }
        return new CrossSectionIndicator(
                2 * x - startX, startY, 2 * x - endX, endY, mirroredArrowhead);
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

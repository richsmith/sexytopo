package org.hwyl.sexytopo.control.graph;

import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.graph.Coord2D;

/**
 * The little mark drawn over a station to show that it has a cross-section: a line representing the
 * plane of the section seen edge-on, optionally with a flag showing which way the section faces.
 * This holds only the geometry, in view coordinates, so that it can be worked out and tested
 * without a canvas.
 */
public final class CrossSectionIndicator {

    private static final float ARROW_LENGTH_FRACTION = 0.4f;
    private static final float ARROW_INNER_OFFSET_FRACTION = 0.05f;

    private final float startX;
    private final float startY;
    private final float endX;
    private final float endY;

    // innerX, innerY, outerX, outerY, tipX, tipY - or null if the indicator has no arrowhead
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
     * An indicator for a section facing a compass direction on a plan: a line of the given length
     * through (x, y) running in the direction of the angle, with an arrowhead showing the side the
     * section is viewed from.
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
     * An indicator for a vertical section on an elevation: a vertical line of the given length
     * through (x, y). The plane of the section is at right angles to the page, so there is no side
     * to point an arrowhead at.
     */
    public static CrossSectionIndicator vertical(float x, float y, float length) {
        return new CrossSectionIndicator(x, y - (length / 2), x, y + (length / 2), null);
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

    public boolean hasArrowhead() {
        return arrowhead != null;
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

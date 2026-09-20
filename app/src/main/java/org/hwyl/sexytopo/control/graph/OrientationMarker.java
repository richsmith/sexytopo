package org.hwyl.sexytopo.control.graph;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.sketch.CrossSection;

/**
 * Where the arrow and label go for the compass on the plan and for the marker in the cross-section
 * editor, which say how a sketch is oriented. The compass is a north arrow that turns with the
 * plan. The editor's marker doesn't turn: it is an "Up" arrow for a vertical cross-section and a
 * north arrow for a horizontal one. Both point up the screen, since up is up in a vertical section
 * and a horizontal one is always drawn with north at the top, so it is the label that tells them
 * apart.
 *
 * <p>This only works out where things go, so that it can be tested without a canvas.
 */
public final class OrientationMarker {

    private final float centreX;
    private final float centreY;
    private final float tipY;
    private final float tailY;
    private final float headSize;
    private final float labelBaselineY;

    private OrientationMarker(
            float centreX,
            float centreY,
            float tipY,
            float tailY,
            float headSize,
            float labelBaselineY) {
        this.centreX = centreX;
        this.centreY = centreY;
        this.tipY = tipY;
        this.tailY = tailY;
        this.headSize = headSize;
        this.labelBaselineY = labelBaselineY;
    }

    /**
     * Lays the marker out above the scale bar, at the left of the view, where the legend is.
     *
     * @param textSize size of the legend text, in pixels
     * @param textHeight full height of a line of the legend text, in pixels
     * @param viewHeight height of the view, in pixels
     */
    public static OrientationMarker layout(float textSize, float textHeight, float viewHeight) {
        float offsetX = textSize * 1.25f; // matches the legend
        float arrowLength = textSize * 2.5f;
        float headSize = textSize * 0.6f;
        float centreX = offsetX + arrowLength / 2f + textSize;
        float scaleBarY = viewHeight - textSize * 4f;
        float centreY = scaleBarY - arrowLength / 2f - textHeight;
        float tipY = centreY - arrowLength / 2f;
        float tailY = centreY + arrowLength / 2f;
        float labelBaselineY = tipY - textSize * 0.2f;
        return new OrientationMarker(centreX, centreY, tipY, tailY, headSize, labelBaselineY);
    }

    /** The string resource for the label that goes with the arrow for this orientation. */
    public static int getLabelResource(CrossSection.Orientation orientation) {
        return orientation == CrossSection.Orientation.HORIZONTAL
                ? R.string.xsection_orientation_north
                : R.string.xsection_orientation_up;
    }

    /** The string resource for the title of the cross-section editor for this orientation. */
    public static int getTitleResource(CrossSection.Orientation orientation) {
        return orientation == CrossSection.Orientation.HORIZONTAL
                ? R.string.title_activity_cross_section_horizontal
                : R.string.title_activity_cross_section_vertical;
    }

    /** Horizontal centre of the arrow, which is also the centre of the label. */
    public float getCentreX() {
        return centreX;
    }

    /** Vertical centre of the arrow, which the compass turns about. */
    public float getCentreY() {
        return centreY;
    }

    /** Where the point of the arrow is. */
    public float getTipY() {
        return tipY;
    }

    /** Where the tail of the arrow ends. */
    public float getTailY() {
        return tailY;
    }

    /** How far the sides of the arrowhead reach across and back from the tip. */
    public float getHeadSize() {
        return headSize;
    }

    /** The baseline to draw the label on, just above the tip. */
    public float getLabelBaselineY() {
        return labelBaselineY;
    }
}

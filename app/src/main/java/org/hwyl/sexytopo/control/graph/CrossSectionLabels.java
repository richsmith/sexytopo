package org.hwyl.sexytopo.control.graph;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.sketch.CrossSection;

/**
 * How each orientation of cross-section is labelled: the glyph that ties the "New ... Section" menu
 * items to the sections they make on the elevation, and the title of the cross-section editor.
 */
public final class CrossSectionLabels {

    // The plane of each kind of section as it appears edge-on in the elevation. Plain characters
    // rather than emoji, so they are drawn like the text around them.
    private static final String VERTICAL_GLYPH = "↕";
    private static final String HORIZONTAL_GLYPH = "↔";

    private CrossSectionLabels() {}

    /** The glyph for this orientation, shown on its menu item and on its sections' frames. */
    public static String getGlyph(CrossSection.Orientation orientation) {
        return orientation == CrossSection.Orientation.HORIZONTAL
                ? HORIZONTAL_GLYPH
                : VERTICAL_GLYPH;
    }

    /** The string resource for the title of the cross-section editor for this orientation. */
    public static int getTitleResource(CrossSection.Orientation orientation) {
        return orientation == CrossSection.Orientation.HORIZONTAL
                ? R.string.title_activity_cross_section_horizontal
                : R.string.title_activity_cross_section_vertical;
    }
}

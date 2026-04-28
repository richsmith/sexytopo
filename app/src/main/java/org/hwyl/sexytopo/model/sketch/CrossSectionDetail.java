package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.control.SexyTopo;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Space;

public class CrossSectionDetail extends SinglePositionDetail {

    // Limit for how small to draw cross-sections on the sketch
    // This is a bit arbitrary... is there a better way of determining this?
    private static final float MIN_VISIBLE_PIXELS = SexyTopo.dpToPixels(8);

    private final CrossSection crossSection;
    private final Sketch sketch;

    public CrossSectionDetail(CrossSection crossSection, Coord2D position) {
        this(crossSection, position, new Sketch());
    }

    public CrossSectionDetail(CrossSection crossSection, Coord2D position, Sketch sketch) {
        super(Colour.NONE, position);
        this.crossSection = crossSection;
        this.sketch = sketch;
        refreshBoundingBox();
    }

    // Minimum half-extent in metres so a cross-section with no splays is still
    // visible and interactive
    private static final float MIN_HALF_EXTENT = 1.0f;
    private static final Coord2D MIN_EXTENT_NW = new Coord2D(-MIN_HALF_EXTENT, -MIN_HALF_EXTENT);
    private static final Coord2D MIN_EXTENT_SE = new Coord2D(MIN_HALF_EXTENT, MIN_HALF_EXTENT);

    private void refreshBoundingBox() {

        // Ensure a minimum size so cross-sections with no splays are still usable
        updateBoundingBox(position.plus(MIN_EXTENT_NW));
        updateBoundingBox(position.plus(MIN_EXTENT_SE));

        // refresh bbox with outer ends of projection legs
        for (Line<Coord2D> line : crossSection.getProjection().getLegMap().values()) {
            updateBoundingBox(line.getEnd().plus(position));
        }

        // refresh bbox with sketch extremities
        // updateBoundingBox(sketch.translate(position)); is cleaner but wasteful
        updateBoundingBox(sketch.getTopLeft().plus(position));
        updateBoundingBox(sketch.getBottomRight().plus(position));
    }

    public CrossSection getCrossSection() {
        return crossSection;
    }

    public Sketch getSketch() {
        return sketch;
    }

    public Space<Coord2D> getProjection() {
        Space<Coord2D> relativeProjection = crossSection.getProjection();

        // This is a bit hacky to have getProjection have a
        // side-effect of updating the bbox, but it does keep
        // everything in sync
        refreshBoundingBox();

        // convert legs from relative to origin to relative to x-section centre
        Space<Coord2D> projection = Space2DUtils.translate(relativeProjection, getPosition());
        return projection;
    }

    /**
     * Return a new detail at the same position and with the same sub-sketch, but a new
     * cross-section angle (compass azimuth in degrees).
     */
    public CrossSectionDetail withAngle(float newAngle) {
        CrossSection rotated = new CrossSection(crossSection.getStation(), newAngle);
        return new CrossSectionDetail(rotated, getPosition(), sketch);
    }

    @Override
    public boolean couldBeVisibleAtScale(float scale) {
        float pixelSize = getMaxDimension() * scale;
        return pixelSize >= MIN_VISIBLE_PIXELS;
    }

    @Override
    public CrossSectionDetail translate(Coord2D translation) {
        return new CrossSectionDetail(getCrossSection(), getPosition().plus(translation), sketch);
    }

    @Override
    public boolean couldBeVisibleAtScale(float scale) {
        // Bounding box is only populated lazily in getProjection(), so the
        // generic min-size cull would hide freshly placed cross-sections.
        return true;
    }
}

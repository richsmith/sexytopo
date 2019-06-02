package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.control.util.SpaceMover;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Space;


public class CrossSectionDetail extends SinglePositionDetail {

    private final CrossSection crossSection;

    public CrossSectionDetail(CrossSection crossSection, Coord2D position) {
        super(Colour.NONE, position);
        this.crossSection = crossSection;
    }

    public CrossSection getCrossSection() {
        return crossSection;
    }

    public Space<Coord2D> getProjection() {
        // FIXME shouldn't recalculate this each time
        Space<Coord2D> relativeProjection = crossSection.getProjection();

        // convert legs from relative to origin to relative to x-section centre
        Space<Coord2D> projection = SpaceMover.move(relativeProjection, getPosition());

        for (Line<Coord2D> line : projection.getLegMap().values()) {
            updateBoundingBox(line.getEnd());
        }

        return projection;
    }

    @Override
    public CrossSectionDetail translate(Coord2D point) {
        return new CrossSectionDetail(getCrossSection(), getPosition().plus(point));
    }
}

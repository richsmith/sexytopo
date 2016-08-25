package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.control.util.SpaceFlipper;
import org.hwyl.sexytopo.control.util.SpaceMover;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Space;

/**
 * Created by rls on 19/09/15.
 */
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
        // FIXME probably don't need to recalculate this each time
        Space<Coord2D> relativeProjection = crossSection.getProjection();

        // flip vertically to account for +ve axis being down on screen and up on survey
        relativeProjection = SpaceFlipper.flipVertically(relativeProjection);

        // convert legs from relative to origin to relative to x-section centre
        Space<Coord2D> projection = SpaceMover.move(relativeProjection, getPosition());

        return projection;
    }
}

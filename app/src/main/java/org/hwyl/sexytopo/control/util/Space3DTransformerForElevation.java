package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.graph.ExtendedElevationDirection;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;

public class Space3DTransformerForElevation extends Space3DTransformer {

    protected void update(Space<Coord3D> space, Station station, Coord3D coord3D) {
        update(space, station, coord3D, 0);
    }

    protected void update(Space<Coord3D> space, Station station, Coord3D coord3D, float rotation) {
        space.addStation(station, coord3D);
        for (Leg leg : station.getOnwardLegs()) {
            if (leg.hasDestination()) {
                updateLeg(space, leg, coord3D);
            } else {
                updateSplay(space, leg, coord3D, rotation);
            }
        }
    }

    protected void updateLeg(Space<Coord3D> space, Leg leg, Coord3D start) {
        Station destination = leg.getDestination();
        Leg projected = projectLeg(leg, destination.getExtendedElevationDirection());

        Coord3D end = Space3DUtils.toCartesian(start, projected);
        space.addLeg(leg, new Line<>(start, end));

        float rotation = projected.getAzimuth() - leg.getAzimuth();
        update(space, destination, end, rotation);
    }

    /**
     * A leg as it is drawn in the extended elevation, which unrolls the cave onto a single plane so
     * that one dimension can be dropped. The section is laid out along the y axis and x is
     * discarded, so a leg's real bearing is replaced by whichever one puts its horizontal run where
     * this direction wants it: north to run rightwards, south to run leftwards, and east to run
     * into the page, which leaves only the height change visible.
     */
    private static Leg projectLeg(Leg leg, ExtendedElevationDirection direction) {
        switch (direction) {
            case RIGHT:
                return leg.adjustAzimuth(0);
            case LEFT:
                return leg.adjustAzimuth(180);
            case VERTICAL:
                return leg.adjustAzimuth(90);
            default:
                throw new IllegalStateException("Unhandled direction: " + direction);
        }
    }

    protected void updateSplay(Space<Coord3D> space, Leg leg, Coord3D start, float rotation) {
        Leg adjustedLeg = leg.rotate(rotation);
        Coord3D end = Space3DUtils.toCartesian(start, adjustedLeg);
        Line<Coord3D> line = new Line<>(start, end);
        space.addLeg(leg, line);
    }
}

package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.graph.Direction;
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
        Direction direction = leg.getDestination().getExtendedElevationDirection();

        Coord3D end = computeEnd(start, leg, direction);
        space.addLeg(leg, new Line<>(start, end));

        if (leg.hasDestination()) {
            float delta = computeDelta(leg, direction);
            update(space, leg.getDestination(), end, delta);
        }
    }

    /**
     * Computes the end coordinate of a leg in the extended elevation projection. Vertical legs
     * preserve x/y and shift only z by the true height change. Left/right legs are projected via
     * toCartesian using their adjusted azimuth.
     */
    private static Coord3D computeEnd(Coord3D start, Leg leg, Direction direction) {
        if (direction == Direction.VERTICAL) {
            return Space3DUtils.toCartesianVertical(start, leg);
        }
        Leg adjustedLeg = adjustLegForDirection(leg, direction);
        return Space3DUtils.toCartesian(start, adjustedLeg);
    }

    /**
     * Computes the azimuth delta to pass to child stations. Vertical legs pass delta=0 so
     * subsequent legs are not rotated by this leg's azimuth. Left/right legs pass the difference
     * introduced by the direction adjustment.
     */
    private static float computeDelta(Leg leg, Direction direction) {
        if (direction == Direction.VERTICAL) {
            return 0;
        }
        Leg adjustedLeg = adjustLegForDirection(leg, direction);
        return adjustedLeg.getAzimuth() - leg.getAzimuth();
    }

    private static Leg adjustLegForDirection(Leg leg, Direction direction) {
        if (direction == Direction.LEFT) {
            return leg.adjustAzimuth(180);
        } else {
            return leg.adjustAzimuth(0);
        }
    }

    protected void updateSplay(Space<Coord3D> space, Leg leg, Coord3D start, float rotation) {
        Leg adjustedLeg = leg.rotate(rotation);
        Coord3D end = Space3DUtils.toCartesian(start, adjustedLeg);
        Line<Coord3D> line = new Line<>(start, end);
        space.addLeg(leg, line);
    }
}

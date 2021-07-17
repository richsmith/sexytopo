package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.graph.Direction;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;


public class Space3DTransformerForElevation  extends Space3DTransformer {


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

        Leg adjustedLeg;
        if (leg.getDestination().getExtendedElevationDirection() == Direction.LEFT) {
            adjustedLeg = leg.adjustAzimuth(180);
        } else {
            adjustedLeg = leg.adjustAzimuth(0);
        }

        float delta = adjustedLeg.getAzimuth() - leg.getAzimuth();

        Coord3D end = Space3DUtils.toCartesian(start, adjustedLeg);
        Line<Coord3D> line = new Line<>(start, end);
        space.addLeg(leg, line);
        if (leg.hasDestination()) {
            update(space, leg.getDestination(), end, delta);
        }
    }


    protected void updateSplay(Space<Coord3D> space, Leg leg, Coord3D start, float rotation) {
        Leg adjustedLeg = leg.rotate(rotation);
        Coord3D end = Space3DUtils.toCartesian(start, adjustedLeg);
        Line<Coord3D> line = new Line<>(start, end);
        space.addLeg(leg, line);
    }
}

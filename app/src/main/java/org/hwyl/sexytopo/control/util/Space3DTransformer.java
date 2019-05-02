package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;


public class Space3DTransformer {

    // These were originally static methods but because we want to override one it has
    // to be OO. Thanks Java for your stupid inability to override static methods :/

    public Space<Coord3D> transformTo3D(Survey survey) {
        return transformTo3D(survey.getOrigin());
    }


    public Space<Coord3D> transformTo3D(Station root) {
        Space<Coord3D> space = new Space<>();
        update(space, root, Coord3D.ORIGIN);
        return space;
    }


    protected void update(Space<Coord3D> space, Station station, Coord3D coord3D) {
        space.addStation(station, coord3D);
        for (Leg leg : station.getOnwardLegs()) {
            update(space, leg, coord3D);
        }
    }


    protected void update(Space<Coord3D> space, Leg leg, Coord3D start) {
        Coord3D end = transform(start, leg);
        Line<Coord3D> line = new Line<>(start, end);
        space.addLeg(leg, line);
        if (leg.hasDestination()) {
            update(space, leg.getDestination(), end);
        }
    }


    public Coord3D transform(Coord3D start, Leg leg) {
        return Space3DUtils.toCartesian(start, leg);
    }

}

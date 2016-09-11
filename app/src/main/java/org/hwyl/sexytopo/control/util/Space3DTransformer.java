package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

/**
 * Created by rls on 26/07/14.
 */
public class Space3DTransformer {

    // These were originally static methods but because we want to override one it has
    // to be OO. Thanks Java for your stupid inability to override static methods :/

    public Space transformTo3D(Survey survey) {
        return transformTo3D(survey.getOrigin());
    }


    public Space transformTo3D(Station root) {
        Space space = new Space();
        update(space, root, Coord3D.ORIGIN);
        return space;
    }


    private void update(Space<Coord3D> space, Station station, Coord3D coord3D) {
        space.addStation(station, coord3D);
        for (Leg leg : station.getOnwardLegs()) {
            update(space, leg, coord3D);
        }
    }


    private void update(Space<Coord3D> space, Leg leg, Coord3D start) {
        Coord3D end = transform(start, leg);
        Line<Coord3D> line = new Line<>(start, end);
        space.addLeg(leg, line);
        if (leg.hasDestination()) {
            update(space, leg.getDestination(), end);
        }
    }


    public Coord3D transform(Coord3D start, Leg leg) {
        double r = leg.getDistance();
        double phi = leg.getAzimuth();
        double theta = leg.getInclination();

        phi = Math.toRadians(phi);
        theta = Math.toRadians(theta);

        double y = r * Math.cos(theta) * Math.cos(phi);
        double x = r * Math.cos(theta) * Math.sin(phi);
        double z = r * Math.sin(theta);

        x += start.getX();
        y += start.getY();
        z += start.getZ();

        return new Coord3D(x, y, z);
    }

}

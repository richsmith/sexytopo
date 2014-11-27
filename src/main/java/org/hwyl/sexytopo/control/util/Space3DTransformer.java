package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.Leg;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.Station;
import org.hwyl.sexytopo.model.Survey;

/**
 * Created by rls on 26/07/14.
 */
public class Space3DTransformer {


    public static Space transformTo3D(Survey survey) {
        Space space = new Space();
        update(space, survey.getOrigin(), Coord3D.ORIGIN);
        return space;
    }

    private static void update(Space<Coord3D> space, Station station, Coord3D coord3D) {
        space.addStation(station, coord3D);
        for (Leg leg : station.getOnwardLegs()) {
            update(space, leg, coord3D);
        }
    }

    private static void update(Space<Coord3D> space, Leg leg, Coord3D start) {
        Coord3D end = transform(start, leg);
        Line<Coord3D> line = new Line<>(start, end);
        space.addLeg(leg, line);
        if (leg.hasDestination()) {
            update(space, leg.getDestination(), end);
        }
    }

    public static Coord3D transform(Coord3D start, Leg leg) {
        double r = leg.getDistance();
        double phi = leg.getBearing();
        double theta = leg.getInclination();

        phi = Math.toRadians(phi);
        theta = Math.toRadians(theta);

        double y = r * Math.cos(theta) * Math.cos(phi);
        double x = r * Math.cos(theta) * Math.sin(phi);
        double z = r * Math.sin(theta);

        x += start.getX();
        y += start.getY();
        z += start.getZ();

        return new Coord3D((int)x, (int)y, (int)z);
    }

}

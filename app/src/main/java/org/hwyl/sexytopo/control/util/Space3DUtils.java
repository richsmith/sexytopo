package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.survey.Leg;


public class Space3DUtils {

    public static Coord3D toCartesian(Coord3D start, Leg leg) {
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

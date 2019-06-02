package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.survey.Leg;


public class Space3DUtils {

    public static Coord3D toCartesian(Coord3D start, Leg leg) {
        double r = leg.getDistance();
        double phi = Math.toRadians(leg.getAzimuth());
        double theta = Math.toRadians(leg.getInclination());

        double y = r * Math.cos(theta) * Math.cos(phi);
        double x = r * Math.cos(theta) * Math.sin(phi);
        double z = r * Math.sin(theta);

        x += start.x;
        y += start.y;
        z += start.z;

        return new Coord3D(x, y, z);
    }

}

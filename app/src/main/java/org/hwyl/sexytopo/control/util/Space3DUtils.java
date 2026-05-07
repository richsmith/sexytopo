package org.hwyl.sexytopo.control.util;

import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.survey.Leg;

public class Space3DUtils {

    public static Coord3D toCartesian(Coord3D start, Leg leg) {
        float r = leg.getDistance();
        float phi = (float) Math.toRadians(leg.getAzimuth());
        float theta = (float) Math.toRadians(leg.getInclination());

        float y = (float) (r * Math.cos(theta) * Math.cos(phi));
        float x = (float) (r * Math.cos(theta) * Math.sin(phi));
        float z = (float) (r * Math.sin(theta));

        x += start.x;
        y += start.y;
        z += start.z;

        return new Coord3D(x, y, z);
    }

    /**
     * Converts a leg to its endpoint treating it as purely vertical: x and y are unchanged from the
     * start, and z shifts by r*sin(inclination). This preserves the true altitude change without
     * any horizontal displacement on screen.
     */
    public static Coord3D toCartesianVertical(Coord3D start, Leg leg) {
        float dz = leg.getDistance() * (float) Math.sin(Math.toRadians(leg.getInclination()));
        return new Coord3D(start.x, start.y, start.z + dz);
    }
}

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

    public static float getDistance(Coord3D a, Coord3D b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        float dz = a.z - b.z;
        return (float) Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
    }

    public static Leg toLeg(Coord3D vector) {
        float x = vector.x, y = vector.y, z = vector.z;

        float distance = (float) Math.sqrt((x * x) + (y * y) + (z * z));

        float azimuth = (float) Math.toDegrees(Math.atan2(x, y));
        if (azimuth < 0) {
            azimuth += 360;
        }

        float horizontal = (float) Math.sqrt((x * x) + (y * y));
        float inclination = (float) Math.toDegrees(Math.atan2(z, horizontal));

        return new Leg(distance, azimuth, inclination);
    }
}

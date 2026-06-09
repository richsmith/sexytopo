package org.hwyl.sexytopo.control.util.amalgamation;

import java.util.List;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.control.util.Space3DUtils;
import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.survey.Leg;

/** Geometry helpers shared between the leg amalgamation strategies. */
final class Amalgamation {

    private Amalgamation() {}

    /** The cartesian endpoints of the readings, each taken from a common origin. */
    static Coord3D[] endpointsOf(List<Leg> legs) {
        Coord3D[] endpoints = new Coord3D[legs.size()];
        for (int i = 0; i < legs.size(); i++) {
            endpoints[i] = Space3DUtils.toCartesian(Coord3D.ORIGIN, legs.get(i));
        }
        return endpoints;
    }

    /** Averages each component (distance, azimuth, inclination) of the readings independently. */
    static Leg averageComponents(List<Leg> legs) {
        int count = legs.size();
        float distance = 0.0f, inclination = 0.0f;
        float[] azimuths = new float[count];
        for (int i = 0; i < count; i++) {
            Leg leg = legs.get(i);
            distance += leg.getDistance();
            inclination += leg.getInclination();
            azimuths[i] = leg.getAzimuth();
        }
        distance /= count;
        inclination /= count;
        return new Leg(distance, Space2DUtils.averageAzimuths(azimuths), inclination);
    }

    /**
     * Averages the readings as vectors by taking the centroid of their cartesian endpoints and
     * converting back to a distance, azimuth and inclination.
     */
    static Leg averageVectors(List<Leg> legs) {
        float x = 0, y = 0, z = 0;
        for (Leg leg : legs) {
            Coord3D endpoint = Space3DUtils.toCartesian(Coord3D.ORIGIN, leg);
            x += endpoint.x;
            y += endpoint.y;
            z += endpoint.z;
        }
        int count = legs.size();
        return Space3DUtils.toLeg(new Coord3D(x / count, y / count, z / count));
    }
}

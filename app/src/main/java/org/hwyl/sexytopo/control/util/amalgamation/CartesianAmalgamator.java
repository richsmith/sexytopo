package org.hwyl.sexytopo.control.util.amalgamation;

import java.util.List;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.Space3DUtils;
import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.survey.Leg;

/**
 * Compares the cartesian endpoints of the readings: they are compatible if every pair of endpoints
 * lies within a fixed distance of each other. Readings are averaged as vectors (the centroid of
 * their endpoints).
 *
 * <p>This behaves sensibly at any inclination because it measures the actual spatial spread of the
 * readings rather than their angular spread. The tolerance is an absolute distance, so it bounds
 * the readings to a sphere of that radius regardless of the leg's direction.
 */
final class CartesianAmalgamator {

    private CartesianAmalgamator() {}

    static boolean areReadingsCompatible(List<Leg> legs) {
        float maxEndpointDelta = GeneralPreferences.getMaxEndpointDelta();
        Coord3D[] endpoints = Amalgamation.endpointsOf(legs);
        for (int i = 0; i < endpoints.length; i++) {
            for (int j = i + 1; j < endpoints.length; j++) {
                if (Space3DUtils.getDistance(endpoints[i], endpoints[j]) > maxEndpointDelta) {
                    return false;
                }
            }
        }
        return true;
    }

    static Leg average(List<Leg> legs) {
        return Amalgamation.averageVectors(legs);
    }
}

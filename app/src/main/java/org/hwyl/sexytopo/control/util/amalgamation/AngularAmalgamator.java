package org.hwyl.sexytopo.control.util.amalgamation;

import java.util.List;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.model.survey.Leg;

/**
 * Compares distance, azimuth and inclination separately, each against its own tolerance, and
 * averages each component independently. This is the historical SexyTopo behaviour and remains the
 * default.
 *
 * <p>It works well for gently-sloping legs but breaks down for steep ones: near the vertical a tiny
 * variation in the endpoint produces a large swing in azimuth even though the readings are in
 * practice identical, so genuinely-repeated steep readings can be rejected. The spatial strategies
 * avoid this.
 */
final class AngularAmalgamator {

    private AngularAmalgamator() {}

    static boolean areReadingsCompatible(List<Leg> legs) {

        float minDistance = Float.POSITIVE_INFINITY, maxDistance = Float.NEGATIVE_INFINITY;
        float minAzimuth = Float.POSITIVE_INFINITY, maxAzimuth = Float.NEGATIVE_INFINITY;
        float minInclination = Float.POSITIVE_INFINITY, maxInclination = Float.NEGATIVE_INFINITY;
        float offsetAzimuth = 540 - legs.get(0).getAzimuth();

        for (Leg leg : legs) {
            minDistance = Math.min(leg.getDistance(), minDistance);
            maxDistance = Math.max(leg.getDistance(), maxDistance);
            float shiftedAzimuth = (leg.getAzimuth() + offsetAzimuth) % 360;
            minAzimuth = Math.min(shiftedAzimuth, minAzimuth);
            maxAzimuth = Math.max(shiftedAzimuth, maxAzimuth);
            minInclination = Math.min(leg.getInclination(), minInclination);
            maxInclination = Math.max(leg.getInclination(), maxInclination);
        }

        float distanceDiff = maxDistance - minDistance;
        float azimuthDiff = maxAzimuth - minAzimuth;
        float inclinationDiff = maxInclination - minInclination;

        float maxDistanceDelta = GeneralPreferences.getMaxDistanceDelta();
        float maxAngleDelta = GeneralPreferences.getMaxAngleDelta();

        return distanceDiff <= maxDistanceDelta
                && azimuthDiff <= maxAngleDelta
                && inclinationDiff <= maxAngleDelta;
    }

    static Leg average(List<Leg> legs) {
        return Amalgamation.averageComponents(legs);
    }
}

package org.hwyl.sexytopo.control.util.amalgamation;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.DoubleStream;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.model.survey.Leg;

/**
 * Compares the horizontal angle, vertical angle and distance of the readings separately, each
 * against its own tolerance, which is how BCRA grade 5 specifies survey accuracy. Readings are
 * averaged as vectors (the centroid of their endpoints).
 *
 * <p>Unlike the angular method, the azimuth spread is scaled by the cosine of the inclination
 * before it is compared. That gives how far the readings actually swing sideways: on a level leg it
 * is the azimuth spread itself, but towards the vertical a large azimuth spread moves the readings
 * very little, so genuinely-repeated steep readings are no longer rejected.
 */
final class SlopeCorrectedAmalgamator {

    private SlopeCorrectedAmalgamator() {}

    static boolean areReadingsCompatible(List<Leg> legs) {
        float maxDistanceDelta = GeneralPreferences.getMaxDistanceDelta();
        float maxAngleDelta = GeneralPreferences.getMaxAngleDelta();

        double distanceRange = rangeOf(legs.stream().mapToDouble(Leg::getDistance));
        double inclinationRange = rangeOf(legs.stream().mapToDouble(Leg::getInclination));

        // Measure azimuths relative to the first reading so readings either side of north don't
        // appear to be 360 degrees apart
        float reference = legs.get(0).getAzimuth();
        double azimuthRange =
                rangeOf(
                        legs.stream()
                                .mapToDouble(leg -> (leg.getAzimuth() - reference + 540) % 360));
        double meanInclination = legs.stream().mapToDouble(Leg::getInclination).average().orElse(0);
        double sidewaysRange = azimuthRange * Math.cos(Math.toRadians(meanInclination));

        return distanceRange <= maxDistanceDelta
                && inclinationRange <= maxAngleDelta
                && sidewaysRange <= maxAngleDelta;
    }

    static Leg average(List<Leg> legs) {
        return Amalgamation.averageVectors(legs);
    }

    private static double rangeOf(DoubleStream values) {
        DoubleSummaryStatistics statistics = values.summaryStatistics();
        return statistics.getMax() - statistics.getMin();
    }
}

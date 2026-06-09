package org.hwyl.sexytopo.control.util.amalgamation;

import java.util.List;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.Space3DUtils;
import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.survey.Leg;

/**
 * Compares the readings using the relative pairwise method from TopoDroid: for each pair of
 * readings the gap between their endpoints is expressed as a fraction of each reading's length and
 * the two fractions are summed; the readings are compatible if this stays below a threshold for
 * every pair. Readings are averaged as vectors (the centroid of their endpoints).
 *
 * <p>The tolerance is relative rather than absolute, so longer legs are allowed a proportionally
 * larger gap. Like the cartesian method it behaves sensibly at any inclination.
 */
final class PairwiseAmalgamator {

    private PairwiseAmalgamator() {}

    static boolean areReadingsCompatible(List<Leg> legs) {
        float maxRelativeError = GeneralPreferences.getMaxPairwiseError();
        Coord3D[] endpoints = Amalgamation.endpointsOf(legs);
        for (int i = 0; i < endpoints.length; i++) {
            for (int j = i + 1; j < endpoints.length; j++) {
                float gap = Space3DUtils.getDistance(endpoints[i], endpoints[j]);
                float lengthI = legs.get(i).getDistance();
                float lengthJ = legs.get(j).getDistance();
                if (lengthI == 0 || lengthJ == 0) {
                    // Degenerate zero-length reading; fall back to an absolute comparison
                    if (gap > maxRelativeError) {
                        return false;
                    }
                    continue;
                }
                if ((gap / lengthI) + (gap / lengthJ) > maxRelativeError) {
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

package org.hwyl.sexytopo.control.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.hwyl.sexytopo.model.graph.Coord2D;

/**
 * Normalises the point order of wall-kind sketch lines so the passage interior is on a consistent
 * side of the line, whichever direction the user happened to draw it in. This is done when the
 * stroke is finished, so the stored point order is canonical and rendering and every exporter can
 * take it at face value.
 *
 * <p>The interior side is inferred from the survey centreline: for each segment of the line, the
 * side the nearest station is on, weighted by segment length. That handles wiggly walls and the odd
 * stray point without being fooled; a wall roughly equidistant between two parallel passages can
 * still guess wrong, which is recoverable in Therion with -reverse on.
 */
public final class LineOrienter {

    private LineOrienter() {}

    /**
     * Reverses points in place if needed so the inferred passage interior is on the canonical side.
     * Returns true if the points were reversed. Points are in survey space (y flipped on export);
     * the canonical side is chosen so the exported line matches Therion's wall orientation
     * convention.
     */
    public static boolean orientToInterior(
            List<Coord2D> points, Collection<Coord2D> stationPositions) {

        if (points.size() < 2 || stationPositions.isEmpty()) {
            return false;
        }

        float weightedSideSum = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            Coord2D from = points.get(i);
            Coord2D to = points.get(i + 1);
            float midX = (from.x + to.x) / 2;
            float midY = (from.y + to.y) / 2;

            Coord2D station = findNearest(stationPositions, midX, midY);

            // cross product of segment direction with the vector to the nearest station:
            // the sign says which side of the segment the station is on, the segment length
            // weights the vote
            float cross =
                    (to.x - from.x) * (station.y - midY) - (to.y - from.y) * (station.x - midX);
            weightedSideSum += Math.signum(cross) * Space2DUtils.getDistance(from, to);
        }

        // survey space is y-down relative to the exported (Therion) frame, so a positive cross
        // here puts the station on the *left* of the drawing direction after the export y-flip
        // — which is the side Therion expects the passage interior on
        if (weightedSideSum < 0) {
            Collections.reverse(points);
            return true;
        }
        return false;
    }

    private static Coord2D findNearest(Collection<Coord2D> candidates, float x, float y) {
        Coord2D nearest = null;
        float bestSquared = Float.MAX_VALUE;
        for (Coord2D candidate : candidates) {
            float dx = candidate.x - x;
            float dy = candidate.y - y;
            float distanceSquared = dx * dx + dy * dy;
            if (distanceSquared < bestSquared) {
                bestSquared = distanceSquared;
                nearest = candidate;
            }
        }
        return nearest;
    }
}

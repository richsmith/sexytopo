package org.hwyl.sexytopo.control.geometry;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.geometry.Line;

/**
 * Normalises the point order of wall-kind sketch lines so the passage interior is on a consistent
 * side of the line, whichever direction the user happened to draw it in. This is done when the
 * stroke is finished, so the stored point order is canonical and rendering and every exporter can
 * take it at face value.
 *
 * <p>The interior side is inferred from the survey centreline: for each segment of the line, the
 * side the nearest leg is on, weighted by segment length. That handles wiggly walls and the odd
 * stray point without being fooled. Legs rather than stations, because the nearest station can be a
 * long way off down a long leg, further than a station in a parallel passage beyond the wall. A
 * wall roughly equidistant between two parallel passages can still guess wrong, which is
 * recoverable in Therion with -reverse on.
 */
public final class LineOrienter {

    private LineOrienter() {}

    /**
     * Reverses points in place if needed so the inferred passage interior is on the canonical side.
     * Returns true if the points were reversed. Points are in survey space (y flipped on export);
     * the canonical side is chosen so the exported line matches Therion's wall orientation
     * convention.
     */
    public static boolean orientToInterior(List<Coord2D> points, Collection<Line<Coord2D>> legs) {

        if (points.size() < 2 || legs.isEmpty()) {
            return false;
        }

        float weightedSideSum = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            Coord2D from = points.get(i);
            Coord2D to = points.get(i + 1);
            float midX = (from.x + to.x) / 2;
            float midY = (from.y + to.y) / 2;

            Coord2D nearest = findNearestPointOnLegs(legs, midX, midY);

            // cross product of segment direction with the vector to the nearest leg: the sign
            // says which side of the segment the leg is on, the segment length weights the vote
            float cross =
                    (to.x - from.x) * (nearest.y - midY) - (to.y - from.y) * (nearest.x - midX);
            weightedSideSum += Math.signum(cross) * Space2DUtils.getDistance(from, to);
        }

        // survey space is y-down relative to the exported (Therion) frame, so a positive cross
        // here puts the leg on the *left* of the drawing direction after the export y-flip
        // — which is the side Therion expects the passage interior on
        if (weightedSideSum < 0) {
            Collections.reverse(points);
            return true;
        }
        return false;
    }

    private static Coord2D findNearestPointOnLegs(
            Collection<Line<Coord2D>> legs, float x, float y) {
        Coord2D target = new Coord2D(x, y);
        return legs.stream()
                .map(leg -> nearestPointOn(leg, target))
                .min(Comparator.comparingDouble(point -> Space2DUtils.getDistance(point, target)))
                .orElse(null); // never empty: orientToInterior checks there are legs
    }

    private static Coord2D nearestPointOn(Line<Coord2D> leg, Coord2D target) {
        Coord2D start = leg.getStart();
        Coord2D end = leg.getEnd();
        float dx = end.x - start.x;
        float dy = end.y - start.y;
        float lengthSquared = dx * dx + dy * dy;
        float fraction =
                lengthSquared == 0
                        ? 0
                        : ((target.x - start.x) * dx + (target.y - start.y) * dy) / lengthSquared;
        fraction = Math.max(0, Math.min(1, fraction));
        return new Coord2D(start.x + fraction * dx, start.y + fraction * dy);
    }
}

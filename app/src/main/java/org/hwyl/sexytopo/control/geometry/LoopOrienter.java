package org.hwyl.sexytopo.control.geometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.hwyl.sexytopo.model.geometry.Coord2D;

/**
 * Normalises the point order of a line drawn round something, so the right side faces it whichever
 * direction the user happened to draw it in: a pit or chimney edge round its hole has its ticks
 * pointing into the hole, and a boulder outline has the rock inside. Like LineOrienter for walls,
 * this is done when the stroke is finished.
 *
 * <p>Unlike a wall, such a line's side can't in general be inferred, so a line that doesn't enclose
 * anything is left as drawn.
 */
public final class LoopOrienter {

    private LoopOrienter() {}

    /**
     * Reverses points in place if needed so a line drawn round something has its inside on the side
     * given by insideOnRight (relative to the drawing direction, as seen on screen). The line
     * encloses something either by itself, its ends meeting within closeDistance of each other, or
     * together with a wall that both its ends come within closeDistance of. If it encloses nothing
     * it's left as drawn. Returns true if the points were reversed.
     */
    public static boolean orientInsideOfLoop(
            List<Coord2D> points,
            boolean insideOnRight,
            Collection<List<Coord2D>> walls,
            float closeDistance) {

        if (points.size() < 3) {
            return false;
        }

        List<Coord2D> loop = findLoop(points, walls, closeDistance);
        if (loop == null) {
            return false;
        }

        // a clockwise loop (positive area) has its inside on the right of the drawing direction
        double signedArea = PolygonUtils.signedArea(loop);
        if (signedArea != 0 && (signedArea > 0) != insideOnRight) {
            Collections.reverse(points);
            return true;
        }
        return false;
    }

    /**
     * The closed outline the line makes, by itself or with the stretch of wall between its ends, or
     * null if it doesn't make one.
     */
    private static List<Coord2D> findLoop(
            List<Coord2D> points, Collection<List<Coord2D>> walls, float closeDistance) {

        Coord2D start = points.get(0);
        Coord2D end = points.get(points.size() - 1);
        if (Space2DUtils.getDistance(start, end) <= closeDistance) {
            return points;
        }

        for (List<Coord2D> wall : walls) {
            WallPosition fromEnd = WallPosition.nearest(wall, end);
            WallPosition fromStart = WallPosition.nearest(wall, start);
            if (fromEnd == null
                    || fromStart == null
                    || fromEnd.distance > closeDistance
                    || fromStart.distance > closeDistance) {
                continue;
            }
            // carry on from the line's end along the wall back to its start
            List<Coord2D> loop = new ArrayList<>(points);
            loop.add(fromEnd.point);
            if (fromEnd.position < fromStart.position) {
                for (int i = fromEnd.segment + 1; i <= fromStart.segment; i++) {
                    loop.add(wall.get(i));
                }
            } else {
                for (int i = fromEnd.segment; i > fromStart.segment; i--) {
                    loop.add(wall.get(i));
                }
            }
            loop.add(fromStart.point);
            return loop;
        }
        return null;
    }

    /** The point on a wall nearest to some other point, and how far along the wall it is. */
    private static final class WallPosition {
        final Coord2D point;
        final int segment;
        final float position; // segment index plus the fraction of the way along that segment
        final float distance;

        private WallPosition(Coord2D point, int segment, float fraction, float distance) {
            this.point = point;
            this.segment = segment;
            this.position = segment + fraction;
            this.distance = distance;
        }

        static WallPosition nearest(List<Coord2D> wall, Coord2D target) {
            WallPosition best = null;
            for (int i = 0; i < wall.size() - 1; i++) {
                Coord2D a = wall.get(i);
                Coord2D b = wall.get(i + 1);
                float dx = b.x - a.x;
                float dy = b.y - a.y;
                float lengthSquared = dx * dx + dy * dy;
                float fraction =
                        lengthSquared == 0
                                ? 0
                                : ((target.x - a.x) * dx + (target.y - a.y) * dy) / lengthSquared;
                fraction = Math.max(0, Math.min(1, fraction));
                Coord2D point = new Coord2D(a.x + fraction * dx, a.y + fraction * dy);
                float distance = Space2DUtils.getDistance(point, target);
                if (best == null || distance < best.distance) {
                    best = new WallPosition(point, i, fraction, distance);
                }
            }
            return best;
        }
    }
}

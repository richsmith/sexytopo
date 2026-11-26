package org.hwyl.sexytopo.control.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hwyl.sexytopo.model.geometry.Coord2D;

/**
 * Bends a line by pulling a point on it: the point goes all the way with the pull, and the line
 * either side follows less and less the further along it is from there, curving smoothly back to
 * its old course a reach away along the line.
 */
public final class LineReshaper {

    // how many points are added along each side of a bend, at most
    private static final int POINTS_PER_REACH = 24;

    private LineReshaper() {}

    /**
     * The line's points with the point on it nearest grab pulled by the offset, as a new list. The
     * line's ends stay put, so it stays joined to whatever it was joined to, unless the grab is
     * within endTolerance of one, which then takes the end itself. A closed loop (its last point
     * the same as its first) has no ends, so the bend can run round past its start, and it stays
     * closed. Points are added where the line bends, so even a long straight stretch curves; the
     * bent stretch is then simplified as a drawn line is, and the rest of the line is left exactly
     * as it was.
     */
    public static List<Coord2D> pull(
            List<Coord2D> line,
            boolean closed,
            Coord2D grab,
            Coord2D offset,
            float reach,
            float endTolerance) {
        int count = line.size();
        float[] along = getDistancesAlong(line);
        float length = along[count - 1];
        if (length == 0) {
            List<Coord2D> moved = new ArrayList<>();
            line.forEach(point -> moved.add(point.plus(offset)));
            return moved;
        }

        // where on the line the grab is
        int grabSegment = 0;
        float grabFraction = 0;
        float nearest = Float.MAX_VALUE;
        for (int i = 0; i < count - 1; i++) {
            float fraction = getFractionNearest(grab, line.get(i), line.get(i + 1));
            float distance =
                    Space2DUtils.getDistance(
                            grab, pointAlong(line.get(i), line.get(i + 1), fraction));
            if (distance < nearest) {
                nearest = distance;
                grabSegment = i;
                grabFraction = fraction;
            }
        }
        float grabAlong =
                along[grabSegment] + grabFraction * (along[grabSegment + 1] - along[grabSegment]);

        float reachBefore;
        float reachAfter;
        if (closed) {
            reachBefore = reachAfter = Math.min(reach, length / 2);
        } else {
            float fromEnd = length - grabAlong;
            if (Math.min(grabAlong, fromEnd) <= endTolerance) {
                grabAlong = grabAlong <= fromEnd ? 0 : length;
            }
            reachBefore = Math.min(reach, grabAlong);
            reachAfter = Math.min(reach, length - grabAlong);
        }
        Bend bend = new Bend(grabAlong, reachBefore, reachAfter, closed ? length : 0);

        // the line with points added along the stretch that bends, and the grab point itself
        float spacing = Math.max(reachBefore, reachAfter) / POINTS_PER_REACH;
        List<Coord2D> points = new ArrayList<>();
        List<Float> distances = new ArrayList<>();
        List<Boolean> isAdded = new ArrayList<>();
        points.add(line.get(0));
        distances.add(0f);
        isAdded.add(false);
        for (int i = 0; i < count - 1; i++) {
            Coord2D start = line.get(i);
            Coord2D end = line.get(i + 1);
            float segmentLength = along[i + 1] - along[i];
            List<Float> fractions = new ArrayList<>();
            if (spacing > 0 && bend.overlaps(along[i], along[i + 1])) {
                int pieces = (int) Math.ceil(segmentLength / spacing);
                for (int piece = 1; piece < pieces; piece++) {
                    fractions.add((float) piece / pieces);
                }
            }
            if (i == grabSegment && grabFraction > 0 && grabFraction < 1) {
                fractions.add(grabFraction);
                Collections.sort(fractions);
            }
            for (float fraction : fractions) {
                points.add(pointAlong(start, end, fraction));
                distances.add(along[i] + fraction * segmentLength);
                isAdded.add(true);
            }
            points.add(end);
            distances.add(along[i + 1]);
            isAdded.add(false);
        }

        boolean[] isChanged = new boolean[points.size()];
        for (int i = 0; i < points.size(); i++) {
            float weight = bend.getWeight(distances.get(i));
            if (weight > 0) {
                points.set(i, points.get(i).plus(offset.scale(weight)));
            }
            isChanged[i] = weight > 0 || isAdded.get(i);
        }
        if (closed) {
            points.set(points.size() - 1, points.get(0));
        }

        return simplifyChanged(points, isChanged);
    }

    /**
     * The points with each run of changed ones simplified, between the unchanged points either
     * side, which are kept as they are.
     */
    private static List<Coord2D> simplifyChanged(List<Coord2D> points, boolean[] isChanged) {
        List<Coord2D> result = new ArrayList<>();
        int i = 0;
        while (i < points.size()) {
            if (!isChanged[i]) {
                result.add(points.get(i));
                i++;
                continue;
            }
            int runEnd = i;
            while (runEnd + 1 < points.size() && isChanged[runEnd + 1]) {
                runEnd++;
            }
            // from the unchanged point before (already added), to the one after
            int from = Math.max(i - 1, 0);
            int to = Math.min(runEnd + 1, points.size() - 1);
            List<Coord2D> simplified =
                    Space2DUtils.simplify(
                            points.subList(from, to + 1), Space2DUtils.SIMPLIFICATION_EPSILON);
            result.addAll(simplified.subList(from < i ? 1 : 0, simplified.size()));
            i = to + 1;
        }
        return result;
    }

    /** How far along the line each of its points is from its start. */
    private static float[] getDistancesAlong(List<Coord2D> line) {
        float[] along = new float[line.size()];
        for (int i = 1; i < line.size(); i++) {
            along[i] = along[i - 1] + Space2DUtils.getDistance(line.get(i - 1), line.get(i));
        }
        return along;
    }

    /** How far along the segment from start to end its point nearest the given one is, 0..1. */
    private static float getFractionNearest(Coord2D point, Coord2D start, Coord2D end) {
        Coord2D direction = end.minus(start);
        float lengthSquared = direction.x * direction.x + direction.y * direction.y;
        if (lengthSquared == 0) {
            return 0;
        }
        Coord2D fromStart = point.minus(start);
        float fraction = (fromStart.x * direction.x + fromStart.y * direction.y) / lengthSquared;
        return Math.max(0, Math.min(1, fraction));
    }

    private static Coord2D pointAlong(Coord2D start, Coord2D end, float fraction) {
        return start.plus(end.minus(start).scale(fraction));
    }

    /**
     * How much of a pull each point along a line follows: all of it at the grab, easing off to none
     * a reach away either side, with no sudden change of direction at either.
     */
    private static final class Bend {
        private final float grabAlong;
        private final float reachBefore;
        private final float reachAfter;
        // the length of a closed loop, which the bend can run round; 0 for an open line
        private final float loopLength;

        Bend(float grabAlong, float reachBefore, float reachAfter, float loopLength) {
            this.grabAlong = grabAlong;
            this.reachBefore = reachBefore;
            this.reachAfter = reachAfter;
            this.loopLength = loopLength;
        }

        float getWeight(float distanceAlong) {
            float offset = getOffsetAlong(distanceAlong);
            if (offset == 0) {
                return 1;
            }
            float reach = offset < 0 ? reachBefore : reachAfter;
            float t = 1 - Math.abs(offset) / reach;
            return t <= 0 ? 0 : t * t * (3 - 2 * t);
        }

        /** Whether any of the stretch of line between the two distances along it bends. */
        boolean overlaps(float from, float to) {
            float low = grabAlong - reachBefore;
            float high = grabAlong + reachAfter;
            if (loopLength == 0) {
                return from < high && to > low;
            }
            for (float shift = -loopLength; shift <= loopLength; shift += loopLength) {
                if (from < high + shift && to > low + shift) {
                    return true;
                }
            }
            return false;
        }

        /** How far along the line a distance is from the grab, the short way round a loop. */
        private float getOffsetAlong(float distanceAlong) {
            float offset = distanceAlong - grabAlong;
            if (loopLength > 0) {
                offset -= loopLength * Math.round(offset / loopLength);
            }
            return offset;
        }
    }
}

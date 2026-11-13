package org.hwyl.sexytopo.control.util;

import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hwyl.sexytopo.model.geometry.Coord2D;

/**
 * Boolean operations on filled regions, implemented on top of android.graphics.Path so the hard
 * geometry cases are handled by the platform. Results are recovered by sampling the output contours
 * with PathMeasure and then simplifying, so exact vertex positions are not preserved — fine for
 * hand-sketched shapes.
 *
 * <p>A region is represented as a Region: an outline contour plus any hole contours. Operations
 * take and return regions, so a hole is preserved through merges and erases rather than being
 * silently filled in. Both inputs and outputs use the even-odd fill rule.
 */
public class PolygonUtils {

    // Sampling resolution when converting a Path contour back to a polygon, in survey metres.
    private static final float MIN_SAMPLE_STEP = 0.01f;
    private static final float MAX_SAMPLE_STEP = 0.5f;
    private static final int TARGET_SAMPLES_PER_CONTOUR = 200;

    // how far along the stroke, from either end, to look for the crossing that closes it, as a
    // fraction of the stroke's length. Kept well short of a quarter: a figure of eight started at
    // one end crosses itself a quarter of the way along, and must not lose a lobe.
    private static final double OVERSHOOT_SEARCH_FRACTION = 0.15;

    // a hook is only looked for this far back from each end, as a multiple of the gap between the
    // ends, and only trimmed if the ends came closer than this fraction of the gap
    private static final double HOOK_SEARCH_GAPS = 2;
    private static final double HOOK_CLOSEST_APPROACH_RATIO = 0.5;

    // width of the channel cut along each line when clipping to it, in survey metres: too narrow
    // to show, but wide enough for the platform ops to part the pieces cleanly
    private static final float CUT_WIDTH = 0.02f;

    /**
     * A filled region: an outline contour, plus zero or more holes lying within it. This is the
     * geometric counterpart of AreaDetail, without the sketch-level concerns of colour and type.
     */
    public static final class Region {

        private final List<Coord2D> outline;
        private final List<List<Coord2D>> holes;

        public Region(List<Coord2D> outline) {
            this(outline, Collections.emptyList());
        }

        public Region(List<Coord2D> outline, List<List<Coord2D>> holes) {
            this.outline = outline;
            this.holes = holes;
        }

        public List<Coord2D> getOutline() {
            return outline;
        }

        public List<List<Coord2D>> getHoles() {
            return holes;
        }

        public List<List<Coord2D>> getContours() {
            List<List<Coord2D>> contours = new ArrayList<>();
            contours.add(outline);
            contours.addAll(holes);
            return contours;
        }

        /** The filled area: that enclosed by the outline, less that of the holes. */
        public double getArea() {
            return area(outline) - holes.stream().mapToDouble(PolygonUtils::area).sum();
        }
    }

    /** Whether two regions overlap (share any filled area). Holes count as not filled. */
    public static boolean overlap(Region a, Region b) {
        Path intersection = new Path();
        boolean succeeded = intersection.op(toPath(a), toPath(b), Path.Op.INTERSECT);
        return succeeded && !intersection.isEmpty();
    }

    /**
     * Union of a set of regions. Overlapping inputs are merged; disjoint inputs come back as
     * separate regions. Merging can create holes — a ring of water built from four overlapping bars
     * encloses a courtyard — and those are returned as holes of the merged region. Returns null if
     * the platform op fails.
     */
    public static List<Region> union(List<Region> regions) {
        Path result = new Path();
        for (Region region : regions) {
            if (!result.op(toPath(region), Path.Op.UNION)) {
                return null;
            }
        }
        return extractRegions(result);
    }

    /**
     * Subtract a disc from a region. Depending on where the disc lands this can shrink the region,
     * punch a hole in its interior, split it into several regions, or (if the disc swallows it)
     * remove it entirely (returning an empty list). Returns null if the platform op fails.
     */
    public static List<Region> subtract(Region region, Coord2D centre, float radius) {
        Path disc = new Path();
        disc.addCircle(centre.x, centre.y, radius, Path.Direction.CW);
        Path result = toPath(region);
        if (!result.op(disc, Path.Op.DIFFERENCE)) {
            return null;
        }
        return extractRegions(result);
    }

    /**
     * Trim the overshoot from a stroke sketched as the outline of an area.
     *
     * <p>To be sure of closing an outline, people tend to carry on past where they started, so the
     * end of the stroke crosses its beginning. Closed as drawn, the stretch beyond the crossing
     * (and any lead-in before it) would enclose a little flap outside the shape, which survives as
     * a jagged spike or notch where the ends meet. Instead, the stroke is cut at the crossing and
     * joined there.
     *
     * <p>Only crossings between the stroke's opening and closing stretches count, so a deliberate
     * figure of eight further round is left alone. If there are several (a scribbled join), the one
     * that keeps the most of the stroke is used. A stroke whose ends don't cross is returned
     * unchanged, to be closed with a straight line as usual.
     */
    public static List<Coord2D> trimOvershoot(List<Coord2D> stroke) {
        int size = stroke.size();
        if (size < 4) {
            return stroke;
        }

        double[] distanceAlong = new double[size];
        for (int i = 1; i < size; i++) {
            Coord2D a = stroke.get(i - 1);
            Coord2D b = stroke.get(i);
            distanceAlong[i] = distanceAlong[i - 1] + Math.hypot(b.x - a.x, b.y - a.y);
        }
        double length = distanceAlong[size - 1];
        double window = length * OVERSHOOT_SEARCH_FRACTION;

        // segment i runs from point i to point i + 1
        int bestHead = -1;
        int bestTail = -1;
        Coord2D bestCrossing = null;
        double bestKept = -1;
        for (int head = 0; head < size - 1 && distanceAlong[head] <= window; head++) {
            for (int tail = size - 2; tail > head + 1; tail--) {
                if (distanceAlong[tail + 1] < length - window) {
                    break;
                }
                Coord2D crossing =
                        segmentIntersection(
                                stroke.get(head),
                                stroke.get(head + 1),
                                stroke.get(tail),
                                stroke.get(tail + 1));
                if (crossing == null) {
                    continue;
                }
                double kept = distanceAlong[tail] - distanceAlong[head + 1];
                if (kept > bestKept) {
                    bestKept = kept;
                    bestHead = head;
                    bestTail = tail;
                    bestCrossing = crossing;
                }
            }
        }

        if (bestCrossing == null) {
            return stroke;
        }

        List<Coord2D> trimmed = new ArrayList<>();
        trimmed.add(bestCrossing);
        trimmed.addAll(stroke.subList(bestHead + 1, bestTail + 1));
        return trimmed;
    }

    /**
     * Trim hooks from the ends of a stroke sketched as the outline of an area.
     *
     * <p>A stroke can stop short of its start, but with its ends curling away from each other as
     * the finger comes off the screen. Closed as drawn, the hooks leave a kink either side of the
     * closing line. Instead, the stroke is cut where its ends came closest, and closed with a
     * straight line from there.
     *
     * <p>This is guarded to avoid misreading a narrow area: the hooks are only looked for a short
     * way back from each end (relative to the gap between the ends, and never beyond the stroke's
     * opening and closing stretches), and only trimmed if the ends came much closer together than
     * they finished. In a narrow channel, the two sides are about as far apart as the ends, so the
     * channel's tip is left alone. A stroke whose ends cross should have had its overshoot trimmed
     * first (see trimOvershoot); its ends then meet, and there is nothing more to do.
     */
    public static List<Coord2D> trimHooks(List<Coord2D> stroke) {
        int size = stroke.size();
        if (size < 4) {
            return stroke;
        }

        Coord2D first = stroke.get(0);
        Coord2D last = stroke.get(size - 1);
        double gap = Math.hypot(last.x - first.x, last.y - first.y);
        if (gap == 0) {
            return stroke;
        }

        double[] distanceAlong = new double[size];
        for (int i = 1; i < size; i++) {
            Coord2D a = stroke.get(i - 1);
            Coord2D b = stroke.get(i);
            distanceAlong[i] = distanceAlong[i - 1] + Math.hypot(b.x - a.x, b.y - a.y);
        }
        double length = distanceAlong[size - 1];
        double window = Math.min(length * OVERSHOOT_SEARCH_FRACTION, gap * HOOK_SEARCH_GAPS);

        int bestHead = 0;
        int bestTail = size - 1;
        double bestDistance = gap;
        for (int head = 0; head < size && distanceAlong[head] <= window; head++) {
            for (int tail = size - 1;
                    tail > head && distanceAlong[tail] >= length - window;
                    tail--) {
                Coord2D a = stroke.get(head);
                Coord2D b = stroke.get(tail);
                double distance = Math.hypot(b.x - a.x, b.y - a.y);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestHead = head;
                    bestTail = tail;
                }
            }
        }

        if (bestDistance >= gap * HOOK_CLOSEST_APPROACH_RATIO) {
            return stroke;
        }
        return new ArrayList<>(stroke.subList(bestHead, bestTail + 1));
    }

    /** Where segments ab and cd cross, or null if they don't. */
    private static Coord2D segmentIntersection(Coord2D a, Coord2D b, Coord2D c, Coord2D d) {
        double abX = b.x - a.x;
        double abY = b.y - a.y;
        double cdX = d.x - c.x;
        double cdY = d.y - c.y;
        double denominator = abX * cdY - abY * cdX;
        if (denominator == 0) {
            return null; // parallel
        }
        double acX = c.x - a.x;
        double acY = c.y - a.y;
        double t = (acX * cdY - acY * cdX) / denominator;
        double u = (acX * abY - acY * abX) / denominator;
        if (t < 0 || t > 1 || u < 0 || u > 1) {
            return null;
        }
        return new Coord2D((float) (a.x + t * abX), (float) (a.y + t * abY));
    }

    /**
     * Resolve a freshly sketched outline into well-formed regions, discarding any interior loops.
     *
     * <p>A single stroke can cross itself — usually by accident, when the user overshoots the point
     * where the outline closes. Interpreting those crossings as holes would litter areas with
     * artefacts the user never intended, so the winding fill rule is used and only outermost
     * contours are kept: a self-crossing tail is absorbed into the boundary rather than punching a
     * hole. Deliberate holes are made by erasing, or by merging areas that enclose a gap.
     *
     * <p>Usually returns a single region, but a stroke that crosses itself can enclose two separate
     * lobes, so the result is a list. Returns null if the platform op fails.
     */
    public static List<Region> normalise(List<Coord2D> outline) {
        Path path = new Path();
        path.setFillType(Path.FillType.WINDING);
        addContour(path, outline);

        // union with an empty path to make the platform resolve self-intersections into
        // non-crossing contours; PathMeasure would otherwise just retrace the original stroke
        Path resolved = new Path();
        if (!resolved.op(path, new Path(), Path.Op.UNION)) {
            return null;
        }

        List<Region> regions = extractRegions(resolved);
        return regions.stream()
                .map(region -> new Region(region.getOutline()))
                .collect(Collectors.toList());
    }

    /**
     * Clip a region to the interior side of a set of oriented lines (e.g. walls), which must have
     * the interior on their (-dy, dx) side, as LineOrienter arranges for walls.
     *
     * <p>The region is cut along the lines, each extended a little past its ends so that one drawn
     * just short of the region's edge still cuts across it. Each piece is then kept if it lies
     * mostly on the interior side of the lines nearest it. So a stroke that strays across a wall
     * loses the part beyond it, one spanning a passage keeps the part between the walls, and one
     * drawn over a pillar gets a hole. A line that doesn't cut the region apart (a stub of wall
     * poking into it) leaves it alone.
     *
     * <p>Returns the region unchanged if nothing is cut off, and also if everything would be: then
     * the lines can't be oriented the way they appear to be. Returns null if a platform op fails.
     */
    public static List<Region> clipToInteriorOf(
            Region region, List<List<Coord2D>> orientedLines, float extension) {

        List<List<Coord2D>> lines =
                orientedLines.stream()
                        .map(PolygonUtils::withoutRepeatedPoints)
                        .filter(line -> line.size() >= 2)
                        .collect(Collectors.toList());

        List<Region> pieces = cutAlong(region, lines, extension);
        if (pieces == null) {
            return null;
        }

        List<Region> kept =
                pieces.stream()
                        .filter(piece -> getInteriorVote(piece, lines) >= 0)
                        .collect(Collectors.toList());
        if (kept.isEmpty() || kept.size() == pieces.size()) {
            return Collections.singletonList(region);
        }

        return closeCuts(kept, region);
    }

    /**
     * Clip a line to the interior side of a set of oriented lines (e.g. walls), as clipToInteriorOf
     * does for a region. The line is cut wherever it crosses one of them, each extended a little
     * past its ends as there, and each piece is kept if it lies mostly on the interior side of the
     * lines nearest it. So a pit edge drawn a little past a wall loses the overshoot.
     *
     * <p>Returns the line unchanged, as its only piece, if nothing is cut off, and also if
     * everything would be.
     */
    public static List<List<Coord2D>> clipLineToInteriorOf(
            List<Coord2D> line, List<List<Coord2D>> orientedLines, float extension) {

        List<List<Coord2D>> lines =
                orientedLines.stream()
                        .map(PolygonUtils::withoutRepeatedPoints)
                        .filter(oriented -> oriented.size() >= 2)
                        .collect(Collectors.toList());
        List<List<Coord2D>> cutters =
                lines.stream()
                        .map(oriented -> extendEnds(oriented, extension))
                        .collect(Collectors.toList());

        List<List<Coord2D>> pieces =
                cutAt(line, cutters).stream()
                        .map(PolygonUtils::withoutRepeatedPoints)
                        .filter(piece -> piece.size() >= 2)
                        .collect(Collectors.toList());
        List<List<Coord2D>> kept =
                pieces.stream()
                        .filter(piece -> getInteriorVote(piece, false, lines) >= 0)
                        .collect(Collectors.toList());
        if (kept.isEmpty() || kept.size() == pieces.size()) {
            return Collections.singletonList(line);
        }
        return kept;
    }

    /** The line split into pieces at every point where it crosses one of the cutters. */
    private static List<List<Coord2D>> cutAt(List<Coord2D> line, List<List<Coord2D>> cutters) {
        List<List<Coord2D>> pieces = new ArrayList<>();
        List<Coord2D> piece = new ArrayList<>();
        piece.add(line.get(0));
        for (int i = 0; i < line.size() - 1; i++) {
            Coord2D from = line.get(i);
            Coord2D to = line.get(i + 1);
            for (float t : getCrossings(from, to, cutters)) {
                Coord2D crossing = from.plus(to.minus(from).scale(t));
                piece.add(crossing);
                pieces.add(piece);
                piece = new ArrayList<>();
                piece.add(crossing);
            }
            piece.add(to);
        }
        pieces.add(piece);
        return pieces;
    }

    /**
     * Where the segment from-to crosses the cutters, as fractions of the way along it, in order.
     */
    private static List<Float> getCrossings(Coord2D from, Coord2D to, List<List<Coord2D>> cutters) {
        List<Float> crossings = new ArrayList<>();
        Coord2D direction = to.minus(from);
        for (List<Coord2D> cutter : cutters) {
            for (int i = 0; i < cutter.size() - 1; i++) {
                Coord2D start = cutter.get(i);
                Coord2D cutterDirection = cutter.get(i + 1).minus(start);
                float denominator = cross(direction, cutterDirection);
                if (denominator == 0) {
                    continue; // parallel
                }
                Coord2D offset = start.minus(from);
                float t = cross(offset, cutterDirection) / denominator;
                float u = cross(offset, direction) / denominator;
                if (t > 0 && t < 1 && u >= 0 && u <= 1) {
                    crossings.add(t);
                }
            }
        }
        Collections.sort(crossings);
        return crossings;
    }

    private static float cross(Coord2D a, Coord2D b) {
        return a.x * b.y - a.y * b.x;
    }

    /** The pieces left after cutting a narrow channel along each line. */
    private static List<Region> cutAlong(
            Region region, List<List<Coord2D>> lines, float extension) {

        Path centrelines = new Path();
        for (List<Coord2D> line : lines) {
            addPolyline(centrelines, extendEnds(line, extension));
        }
        Path cuts = new Path();
        getStrokePaint(CUT_WIDTH).getFillPath(centrelines, cuts);

        Path result = toPath(region);
        if (!result.op(cuts, Path.Op.DIFFERENCE)) {
            return null;
        }
        return extractRegions(result);
    }

    /**
     * Join the kept pieces back up, closing the cuts within and between them: grow them by the cut
     * width, then trim back to the region they came from. Where a piece was dropped, the edge ends
     * up a hair beyond the line, which doesn't show.
     */
    private static List<Region> closeCuts(List<Region> kept, Region original) {

        Path joined = new Path();
        for (Region piece : kept) {
            if (!joined.op(toPath(piece), Path.Op.UNION)) {
                return null;
            }
        }

        Path rim = new Path();
        getStrokePaint(2 * CUT_WIDTH).getFillPath(joined, rim);
        if (!joined.op(rim, Path.Op.UNION) || !joined.op(toPath(original), Path.Op.INTERSECT)) {
            return null;
        }
        return extractRegions(joined);
    }

    private static Paint getStrokePaint(float width) {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        paint.setStrokeJoin(Paint.Join.ROUND);
        return paint;
    }

    /**
     * How the region lies relative to the oriented lines: positive if mostly on their interior
     * side, negative if mostly on the exterior. Each edge of its contours votes, weighted by its
     * length, for the side it's on of whichever line is nearest it.
     *
     * <p>Package-private rather than private so it can be unit tested directly.
     */
    static double getInteriorVote(Region region, List<List<Coord2D>> orientedLines) {
        return region.getContours().stream()
                .mapToDouble(contour -> getInteriorVote(contour, true, orientedLines))
                .sum();
    }

    /** As for a region, but for the edges of a single contour, or of an open line. */
    private static double getInteriorVote(
            List<Coord2D> points, boolean isClosed, List<List<Coord2D>> orientedLines) {
        int size = points.size();
        int edgeCount = isClosed ? size : size - 1;
        double vote = 0;
        for (int i = 0; i < edgeCount; i++) {
            Coord2D from = points.get(i);
            Coord2D to = points.get((i + 1) % size);
            Coord2D midpoint = from.plus(to).scale(0.5f);
            vote +=
                    Math.signum(getSide(midpoint, orientedLines))
                            * Space2DUtils.getDistance(from, to);
        }
        return vote;
    }

    /**
     * Which side of the nearest line the point is on: positive for the interior (-dy, dx) side,
     * negative for the exterior. Where the nearest place is a bend in the line, the side is judged
     * against both segments meeting there, so a point just beyond a corner isn't misjudged.
     */
    private static float getSide(Coord2D point, List<List<Coord2D>> orientedLines) {

        List<Coord2D> nearestLine = null;
        int nearestSegment = 0;
        float nearestT = 0;
        float bestSquared = Float.MAX_VALUE;

        for (List<Coord2D> line : orientedLines) {
            for (int i = 0; i < line.size() - 1; i++) {
                Coord2D start = line.get(i);
                Coord2D direction = line.get(i + 1).minus(start);
                Coord2D offset = point.minus(start);
                float lengthSquared = direction.x * direction.x + direction.y * direction.y;
                float t = (offset.x * direction.x + offset.y * direction.y) / lengthSquared;
                t = Math.max(0, Math.min(1, t));
                Coord2D fromNearest = offset.minus(direction.scale(t));
                float distanceSquared =
                        fromNearest.x * fromNearest.x + fromNearest.y * fromNearest.y;
                if (distanceSquared < bestSquared) {
                    bestSquared = distanceSquared;
                    nearestLine = line;
                    nearestSegment = i;
                    nearestT = t;
                }
            }
        }

        if (nearestLine == null) {
            return 0;
        }

        Coord2D normal = getInteriorNormal(nearestLine, nearestSegment);
        if (nearestT == 0 && nearestSegment > 0) {
            normal = normal.plus(getInteriorNormal(nearestLine, nearestSegment - 1));
        } else if (nearestT == 1 && nearestSegment < nearestLine.size() - 2) {
            normal = normal.plus(getInteriorNormal(nearestLine, nearestSegment + 1));
        }

        Coord2D start = nearestLine.get(nearestSegment);
        Coord2D nearest =
                start.plus(nearestLine.get(nearestSegment + 1).minus(start).scale(nearestT));
        Coord2D fromNearest = point.minus(nearest);
        return fromNearest.x * normal.x + fromNearest.y * normal.y;
    }

    /** The unit normal of a line's segment, pointing to the interior (-dy, dx) side. */
    private static Coord2D getInteriorNormal(List<Coord2D> line, int segment) {
        Coord2D direction = line.get(segment + 1).minus(line.get(segment));
        return new Coord2D(-direction.y, direction.x).normalise();
    }

    /**
     * The line with each end pushed out by the given distance, carrying on the way it was going.
     */
    private static List<Coord2D> extendEnds(List<Coord2D> line, float extension) {
        List<Coord2D> reversed = new ArrayList<>(line);
        Collections.reverse(reversed);

        List<Coord2D> extended = new ArrayList<>();
        extended.add(extendEnd(reversed, extension));
        extended.addAll(line);
        extended.add(extendEnd(line, extension));
        return extended;
    }

    /**
     * A point beyond the last point of the line. The direction is taken from a point at least the
     * extension distance back, so a little flick at the end of a stroke doesn't swing it.
     */
    private static Coord2D extendEnd(List<Coord2D> line, float extension) {
        Coord2D end = line.get(line.size() - 1);
        Coord2D back = line.get(0);
        for (int i = line.size() - 2; i >= 0; i--) {
            back = line.get(i);
            if (Space2DUtils.getDistance(end, back) >= extension) {
                break;
            }
        }
        return end.plus(end.minus(back).normalise().scale(extension));
    }

    private static List<Coord2D> withoutRepeatedPoints(List<Coord2D> line) {
        List<Coord2D> distinct = new ArrayList<>();
        for (Coord2D point : line) {
            if (distinct.isEmpty() || !distinct.get(distinct.size() - 1).equals(point)) {
                distinct.add(point);
            }
        }
        return distinct;
    }

    private static void addPolyline(Path path, List<Coord2D> line) {
        path.moveTo(line.get(0).x, line.get(0).y);
        for (Coord2D point : line.subList(1, line.size())) {
            path.lineTo(point.x, point.y);
        }
    }

    private static Path toPath(Region region) {
        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        for (List<Coord2D> contour : region.getContours()) {
            addContour(path, contour);
        }
        return path;
    }

    private static void addContour(Path path, List<Coord2D> contour) {
        boolean first = true;
        for (Coord2D point : contour) {
            if (first) {
                path.moveTo(point.x, point.y);
                first = false;
            } else {
                path.lineTo(point.x, point.y);
            }
        }
        path.close();
    }

    private static List<Region> extractRegions(Path path) {

        List<List<Coord2D>> contours = new ArrayList<>();

        PathMeasure pathMeasure = new PathMeasure(path, false);
        do {
            List<Coord2D> contour = sampleContour(pathMeasure);
            if (contour != null) {
                contours.add(contour);
            }
        } while (pathMeasure.nextContour());

        return groupIntoRegions(contours);
    }

    /**
     * Sort a flat list of contours into regions, by how deeply each is nested inside the others. A
     * contour inside no other is an outline; one inside an outline is a hole in it; one inside a
     * hole is an island, the outline of a region of its own; and so on. Each hole is assigned to
     * the outline one level out that contains it, which is unambiguous because contours produced by
     * a boolean op never cross.
     *
     * <p>Package-private rather than private so it can be unit tested directly. The operations that
     * call it go through android.graphics.Path, whose Robolectric shadow does not implement real
     * boolean geometry or multi-contour PathMeasure iteration, so they can only be exercised on a
     * device or emulator.
     */
    static List<Region> groupIntoRegions(List<List<Coord2D>> contours) {

        Map<List<Coord2D>, Long> depths =
                contours.stream()
                        .collect(
                                Collectors.toMap(
                                        contour -> contour,
                                        contour -> countContaining(contour, contours),
                                        (a, b) -> a,
                                        IdentityHashMap::new));

        return contours.stream()
                .filter(contour -> depths.get(contour) % 2 == 0)
                .map(
                        outline ->
                                new Region(
                                        outline,
                                        contours.stream()
                                                .filter(
                                                        hole ->
                                                                depths.get(hole)
                                                                        == depths.get(outline) + 1)
                                                .filter(hole -> contains(outline, hole.get(0)))
                                                .collect(Collectors.toList())))
                .collect(Collectors.toList());
    }

    private static long countContaining(List<Coord2D> contour, List<List<Coord2D>> contours) {
        Coord2D representative = contour.get(0);
        return contours.stream()
                .filter(other -> other != contour && contains(other, representative))
                .count();
    }

    /** Sample the current PathMeasure contour into a polygon, or null if it is degenerate. */
    private static List<Coord2D> sampleContour(PathMeasure pathMeasure) {

        float length = pathMeasure.getLength();
        if (length <= 0) {
            return null;
        }

        float step = length / TARGET_SAMPLES_PER_CONTOUR;
        step = Math.max(MIN_SAMPLE_STEP, Math.min(MAX_SAMPLE_STEP, step));

        List<Coord2D> sampled = new ArrayList<>();
        float[] position = new float[2];
        for (float distance = 0; distance < length; distance += step) {
            if (pathMeasure.getPosTan(distance, position, null)) {
                sampled.add(new Coord2D(position[0], position[1]));
            }
        }

        if (sampled.size() < 3) {
            return null;
        }

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (Coord2D point : sampled) {
            minX = Math.min(minX, point.x);
            maxX = Math.max(maxX, point.x);
            minY = Math.min(minY, point.y);
            maxY = Math.max(maxY, point.y);
        }
        float epsilon = Space2DUtils.simplificationEpsilon(maxX - minX, maxY - minY);
        List<Coord2D> polygon = Space2DUtils.simplify(sampled, epsilon);

        return polygon.size() >= 3 ? polygon : null;
    }

    /** The area enclosed by a closed contour (shoelace formula), whichever way round it runs. */
    public static double area(List<Coord2D> polygon) {
        double twiceSignedArea = 0;
        int size = polygon.size();
        for (int i = 0, j = size - 1; i < size; j = i++) {
            Coord2D a = polygon.get(j);
            Coord2D b = polygon.get(i);
            twiceSignedArea += (double) a.x * b.y - (double) b.x * a.y;
        }
        return Math.abs(twiceSignedArea) / 2;
    }

    /** Standard ray-casting point-in-polygon test. */
    public static boolean contains(List<Coord2D> polygon, Coord2D point) {
        boolean inside = false;
        int size = polygon.size();
        for (int i = 0, j = size - 1; i < size; j = i++) {
            Coord2D a = polygon.get(i);
            Coord2D b = polygon.get(j);
            if ((a.y > point.y) != (b.y > point.y)
                    && point.x < (b.x - a.x) * (point.y - a.y) / (b.y - a.y) + a.x) {
                inside = !inside;
            }
        }
        return inside;
    }
}

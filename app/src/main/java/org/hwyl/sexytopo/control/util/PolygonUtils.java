package org.hwyl.sexytopo.control.util;

import android.graphics.Path;
import android.graphics.PathMeasure;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.hwyl.sexytopo.model.graph.Coord2D;

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
     * Sort a flat list of contours into regions. A contour lying inside another is a hole; anything
     * else is the outline of its own region. Each hole is assigned to the first outline containing
     * it, which is unambiguous because regions produced by a boolean op never overlap.
     *
     * <p>Nesting deeper than one level (an island within a hole) is not modelled: such a contour is
     * treated as a hole of whichever outline contains it, and the even-odd fill rule then draws it
     * filled, which is the right picture in any case.
     *
     * <p>Package-private rather than private so it can be unit tested directly. The operations that
     * call it go through android.graphics.Path, whose Robolectric shadow does not implement real
     * boolean geometry or multi-contour PathMeasure iteration, so they can only be exercised on a
     * device or emulator.
     */
    static List<Region> groupIntoRegions(List<List<Coord2D>> contours) {

        List<List<Coord2D>> outlines = new ArrayList<>();
        List<List<Coord2D>> holes = new ArrayList<>();

        for (List<Coord2D> contour : contours) {
            if (isInsideAnyOther(contour, contours)) {
                holes.add(contour);
            } else {
                outlines.add(contour);
            }
        }

        List<Region> regions = new ArrayList<>();
        for (List<Coord2D> outline : outlines) {
            List<List<Coord2D>> ownHoles =
                    holes.stream()
                            .filter(hole -> contains(outline, hole.get(0)))
                            .collect(Collectors.toList());
            regions.add(new Region(outline, ownHoles));
        }
        return regions;
    }

    private static boolean isInsideAnyOther(List<Coord2D> contour, List<List<Coord2D>> contours) {
        Coord2D representative = contour.get(0);
        return contours.stream()
                .anyMatch(other -> other != contour && contains(other, representative));
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

package org.hwyl.sexytopo.control.util;

import android.graphics.Path;
import android.graphics.PathMeasure;
import java.util.ArrayList;
import java.util.List;
import org.hwyl.sexytopo.model.graph.Coord2D;

/**
 * Boolean operations on simple polygons (lists of vertices with an implicit closing segment),
 * implemented on top of android.graphics.Path so the hard geometry cases are handled by the
 * platform. Results are recovered by sampling the output contours with PathMeasure and then
 * simplifying, so exact vertex positions are not preserved — fine for hand-sketched shapes.
 *
 * <p>Holes in the output (e.g. a ring formed by merging two crescents) cannot be represented by the
 * polygon-list model, so hole contours are dropped, which fills them in.
 */
public class PolygonUtils {

    // Sampling resolution when converting a Path contour back to a polygon, in survey metres.
    private static final float MIN_SAMPLE_STEP = 0.01f;
    private static final float MAX_SAMPLE_STEP = 0.5f;
    private static final int TARGET_SAMPLES_PER_CONTOUR = 200;

    /** Whether two polygons overlap (share any interior area). */
    public static boolean overlap(List<Coord2D> a, List<Coord2D> b) {
        Path intersection = new Path();
        boolean succeeded = intersection.op(toPath(a), toPath(b), Path.Op.INTERSECT);
        return succeeded && !intersection.isEmpty();
    }

    /**
     * Union of a set of polygons. Overlapping inputs are merged; disjoint inputs come back as
     * separate polygons. Returns null if the platform op fails.
     */
    public static List<List<Coord2D>> union(List<List<Coord2D>> polygons) {
        Path result = new Path();
        for (List<Coord2D> polygon : polygons) {
            if (!result.op(toPath(polygon), Path.Op.UNION)) {
                return null;
            }
        }
        return extractPolygons(result);
    }

    /**
     * Subtract a disc from a polygon. Depending on where the disc lands this can shrink the
     * polygon, split it into several polygons, or (if the disc swallows it) remove it entirely
     * (returning an empty list). Returns null if the platform op fails.
     */
    public static List<List<Coord2D>> subtract(
            List<Coord2D> polygon, Coord2D centre, float radius) {
        Path disc = new Path();
        disc.addCircle(centre.x, centre.y, radius, Path.Direction.CW);
        Path result = toPath(polygon);
        if (!result.op(disc, Path.Op.DIFFERENCE)) {
            return null;
        }
        return extractPolygons(result);
    }

    private static Path toPath(List<Coord2D> polygon) {
        Path path = new Path();
        boolean first = true;
        for (Coord2D point : polygon) {
            if (first) {
                path.moveTo(point.x, point.y);
                first = false;
            } else {
                path.lineTo(point.x, point.y);
            }
        }
        path.close();
        return path;
    }

    private static List<List<Coord2D>> extractPolygons(Path path) {

        List<List<Coord2D>> polygons = new ArrayList<>();

        PathMeasure pathMeasure = new PathMeasure(path, false);
        do {
            List<Coord2D> polygon = sampleContour(pathMeasure);
            if (polygon != null) {
                polygons.add(polygon);
            }
        } while (pathMeasure.nextContour());

        dropHoles(polygons);
        return polygons;
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

    /**
     * Remove any contour that lies inside another (i.e. a hole), since the polygon model can't
     * represent holes. Islands are unaffected: after a boolean op they never nest.
     */
    private static void dropHoles(List<List<Coord2D>> polygons) {
        for (int i = polygons.size() - 1; i >= 0; i--) {
            Coord2D representative = polygons.get(i).get(0);
            for (int j = 0; j < polygons.size(); j++) {
                if (i != j && contains(polygons.get(j), representative)) {
                    polygons.remove(i);
                    break;
                }
            }
        }
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

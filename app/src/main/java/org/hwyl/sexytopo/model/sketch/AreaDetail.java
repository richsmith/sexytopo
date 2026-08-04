package org.hwyl.sexytopo.model.sketch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.hwyl.sexytopo.control.util.PolygonUtils;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.graph.Coord2D;

/**
 * A filled region of the sketch, e.g. a pool of water. The region is bounded by an outline: a list
 * of vertices with an implicit closing segment from the last vertex back to the first. It may also
 * have holes — unfilled islands within it, e.g. a dry patch in the middle of a pool, or the
 * courtyard of a ring of water. Each hole is a closed contour in the same form as the outline.
 *
 * <p>Holes are not nested: a hole never contains a further filled region. That can't arise from the
 * ways areas are created (drawing, merging, erasing) without deliberate effort, and if it somehow
 * does, the even-odd fill rule renders the innermost contour filled, which is the sensible picture
 * anyway.
 */
public final class AreaDetail extends SketchDetail {

    private final List<Coord2D> outline;
    private final List<List<Coord2D>> holes;
    private final AreaType areaType;

    public AreaDetail(List<Coord2D> outline, AreaType areaType, Colour colour) {
        this(outline, Collections.emptyList(), areaType, colour);
    }

    public AreaDetail(
            List<Coord2D> outline, List<List<Coord2D>> holes, AreaType areaType, Colour colour) {
        super(colour);
        this.outline = outline;
        this.holes = holes;
        this.areaType = areaType;
        // only the outline contributes to the bounding box; holes are inside it by definition
        for (Coord2D point : outline) {
            updateBoundingBox(point);
        }
    }

    public List<Coord2D> getOutline() {
        return outline;
    }

    public List<List<Coord2D>> getHoles() {
        return holes;
    }

    public boolean hasHoles() {
        return !holes.isEmpty();
    }

    /** The outline followed by any holes, i.e. every closed contour that bounds the region. */
    public List<List<Coord2D>> getContours() {
        List<List<Coord2D>> contours = new ArrayList<>();
        contours.add(outline);
        contours.addAll(holes);
        return contours;
    }

    public AreaType getAreaType() {
        return areaType;
    }

    /** This area's geometry, for handing to the boolean polygon operations. */
    public PolygonUtils.Region toRegion() {
        return new PolygonUtils.Region(outline, holes);
    }

    /** An area with this one's type and colour, but the geometry of the given region. */
    public AreaDetail withRegion(PolygonUtils.Region region) {
        return new AreaDetail(region.getOutline(), region.getHoles(), areaType, getColour());
    }

    /**
     * Distance from the region's boundary (not its interior); a point well inside the area is
     * considered far from it, which stops e.g. the eraser grabbing an area when the user taps in
     * the middle of it. Hole edges count as boundary, so an area can be grabbed by the rim of one
     * of its holes.
     */
    @Override
    public float getDistanceFrom(Coord2D point) {
        return (float)
                getContours().stream()
                        .mapToDouble(contour -> getDistanceFromContour(contour, point))
                        .min()
                        .orElse(Float.MAX_VALUE);
    }

    private static float getDistanceFromContour(List<Coord2D> contour, Coord2D point) {
        float minDistance = Float.MAX_VALUE;
        int size = contour.size();
        for (int i = 0; i < size; i++) {
            Coord2D start = contour.get(i);
            Coord2D end = contour.get((i + 1) % size);
            minDistance =
                    Math.min(minDistance, Space2DUtils.getDistanceFromLine(point, start, end));
        }
        return minDistance;
    }

    @Override
    public AreaDetail translate(Coord2D translation) {
        return transform(point -> point.plus(translation));
    }

    @Override
    public AreaDetail scale(float scale) {
        return transform(point -> point.scale(scale));
    }

    private AreaDetail transform(UnaryOperator<Coord2D> operator) {
        List<Coord2D> newOutline = transformContour(outline, operator);
        List<List<Coord2D>> newHoles =
                holes.stream()
                        .map(hole -> transformContour(hole, operator))
                        .collect(Collectors.toList());
        return new AreaDetail(newOutline, newHoles, areaType, getColour());
    }

    private static List<Coord2D> transformContour(
            List<Coord2D> contour, UnaryOperator<Coord2D> operator) {
        return contour.stream().map(operator).collect(Collectors.toList());
    }
}

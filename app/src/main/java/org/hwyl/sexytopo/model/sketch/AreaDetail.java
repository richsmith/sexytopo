package org.hwyl.sexytopo.model.sketch;

import java.util.ArrayList;
import java.util.List;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.graph.Coord2D;

/**
 * A filled region of the sketch, e.g. a pool of water. The boundary is a simple polygon: a list of
 * vertices with an implicit closing segment from the last vertex back to the first.
 */
public final class AreaDetail extends SketchDetail {

    private final List<Coord2D> polygon;
    private final AreaType areaType;

    public AreaDetail(List<Coord2D> polygon, AreaType areaType, Colour colour) {
        super(colour);
        this.polygon = polygon;
        this.areaType = areaType;
        for (Coord2D point : polygon) {
            updateBoundingBox(point);
        }
    }

    public List<Coord2D> getPolygon() {
        return polygon;
    }

    public AreaType getAreaType() {
        return areaType;
    }

    /**
     * Distance from the polygon boundary (not the interior); a point well inside the area is
     * considered far from it, which stops e.g. the eraser grabbing an area when the user taps in
     * the middle of it.
     */
    @Override
    public float getDistanceFrom(Coord2D point) {
        float minDistance = Float.MAX_VALUE;
        int size = polygon.size();
        for (int i = 0; i < size; i++) {
            Coord2D start = polygon.get(i);
            Coord2D end = polygon.get((i + 1) % size);
            minDistance =
                    Math.min(minDistance, Space2DUtils.getDistanceFromLine(point, start, end));
        }
        return minDistance;
    }

    @Override
    public AreaDetail translate(Coord2D translation) {
        List<Coord2D> newPolygon = new ArrayList<>();
        for (Coord2D point : polygon) {
            newPolygon.add(point.plus(translation));
        }
        return new AreaDetail(newPolygon, areaType, getColour());
    }

    @Override
    public AreaDetail scale(float scale) {
        List<Coord2D> newPolygon = new ArrayList<>();
        for (Coord2D point : polygon) {
            newPolygon.add(point.scale(scale));
        }
        return new AreaDetail(newPolygon, areaType, getColour());
    }
}

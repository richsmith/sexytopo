package org.hwyl.sexytopo.control.io.thirdparty.therion;

import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Space;


public class SketchDimensions {

    final public float minX, maxX, minY, maxY;

    private SketchDimensions(float minX, float maxX, float minY, float maxY) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
    }

    public float getWidth() {
        return Math.abs(maxX - minX);
    }

    public float getHeight() {
        return Math.abs(maxY - minY);
    }

    public static SketchDimensions getDimensions(Space<Coord2D> space) {
        Float minX = null, maxX = null, minY = null, maxY = null;
        for (Line<Coord2D> line : space.getLegMap().values()) {
            for (Coord2D point : new Coord2D[]{line.getStart(), line.getEnd()}) {
                if (minX == null || point.x < minX) {
                    minX = point.x;
                }
                if (maxX == null || point.x > maxX) {
                    maxX = point.x;
                }
                if (minY == null || point.y < minY) {
                    minY = point.y;
                }
                if (maxY == null || point.y > maxY) {
                    maxY = point.y;
                }
            }
        }

        if (minX == null) {
            minX = 0.0f;
        }
        if (maxX == null) {
            maxX = 20.0f;
        }
        if (minY == null) {
            minY = 0.0f;
        }
        if (maxY == null) {
            maxY = 20.0f;
        }

        return new SketchDimensions(minX, maxX, minY, maxY);

    }

}

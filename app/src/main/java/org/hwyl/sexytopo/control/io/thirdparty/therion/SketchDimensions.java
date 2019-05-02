package org.hwyl.sexytopo.control.io.thirdparty.therion;

import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Space;


public class SketchDimensions {

    final public double minX, maxX, minY, maxY;

    private SketchDimensions(double minX, double maxX, double minY, double maxY) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
    }

    public double getWidth() {
        return Math.abs(maxX - minX);
    }

    public double getHeight() {
        return Math.abs(maxY - minY);
    }

    public static SketchDimensions getDimensions(Space<Coord2D> space) {
        Double minX = null, maxX = null, minY = null, maxY = null;
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
            minX = 0.0;
        }
        if (maxX == null) {
            maxX = 20.0;
        }
        if (minY == null) {
            minY = 0.0;
        }
        if (maxY == null) {
            maxY = 20.0;
        }

        return new SketchDimensions(minX, maxX, minY, maxY);

    }
}

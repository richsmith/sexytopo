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
                if (minX == null || point.getX() < minX) {
                    minX = point.getX();
                }
                if (maxX == null || point.getX() > maxX) {
                    maxX = point.getX();
                }
                if (minY == null || point.getY() < minY) {
                    minY = point.getY();
                }
                if (maxY == null || point.getY() > maxY) {
                    maxY = point.getY();
                }
            }
        }
        return new SketchDimensions(minX, maxX, minY, maxY);

    }
}

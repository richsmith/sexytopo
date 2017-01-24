package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.model.graph.Coord2D;


public abstract class SketchDetail {

    private final Colour colour;

    private double left = Double.POSITIVE_INFINITY;
    private double right = Double.NEGATIVE_INFINITY;
    private double top = Double.POSITIVE_INFINITY;
    private double bottom = Double.NEGATIVE_INFINITY;

    protected SketchDetail(Colour colour) {
        this.colour = colour;
    }

    public Colour getColour() {
        return colour;
    }

    public abstract double getDistanceFrom(Coord2D point);

    public abstract SketchDetail translate(Coord2D point);

    public boolean intersectsRectangle(Coord2D rectangleTopLeft, Coord2D rectangleBottomRight) {

        double left1 = rectangleTopLeft.getX();
        double top1 = rectangleTopLeft.getY();
        double right1 = rectangleBottomRight.getX();
        double bottom1 = rectangleBottomRight.getY();

        return (right >= left1 && left <= right1) && (top <= bottom1 && bottom >= top1);
    }

    protected void updateBoundingBox(Coord2D point) {
        left = Math.min(left, point.getX());
        right = Math.max(right, point.getX());
        top = Math.min(top, point.getY());
        bottom = Math.max(bottom, point.getY());

    }
}

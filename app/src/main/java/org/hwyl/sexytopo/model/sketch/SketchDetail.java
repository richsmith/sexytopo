package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.model.graph.Coord2D;


public abstract class SketchDetail {

    private final Colour colour;

    private double left;
    private double right;
    private double top;
    private double bottom;

    protected SketchDetail(Colour colour) {
        this.colour = colour;
        resetBoundingBox();
    }

    public Colour getColour() {
        return colour;
    }

    public abstract double getDistanceFrom(Coord2D point);

    public abstract SketchDetail translate(Coord2D point);

    public boolean intersectsRectangle(Coord2D rectangleTopLeft, Coord2D rectangleBottomRight) {

        double left1 = rectangleTopLeft.x;
        double top1 = rectangleTopLeft.y;
        double right1 = rectangleBottomRight.x;
        double bottom1 = rectangleBottomRight.y;

        return (right >= left1 && left <= right1) && (top <= bottom1 && bottom >= top1);
    }

    protected void updateBoundingBox(Coord2D point) {
        left = Math.min(left, point.x);
        right = Math.max(right, point.x);
        top = Math.min(top, point.y);
        bottom = Math.max(bottom, point.y);

    }


    protected void resetBoundingBox() {
        left = Double.POSITIVE_INFINITY;
        right = Double.NEGATIVE_INFINITY;
        top = Double.POSITIVE_INFINITY;
        bottom = Double.NEGATIVE_INFINITY;
    }


    public double getWidth() {
        if (left == Double.POSITIVE_INFINITY || right == Double.NEGATIVE_INFINITY) {
            return 0;
        } else {
            return right - left;
        }
    }


    public double getHeight() {
        if (top == Double.POSITIVE_INFINITY || bottom == Double.NEGATIVE_INFINITY) {
            return 0;
        } else {
            return bottom - top;
        }
    }


    public Coord2D getTopLeft() {
        if (top == Double.POSITIVE_INFINITY || left == Double.POSITIVE_INFINITY) {
            return new Coord2D(0, 0);
        } else {
            return new Coord2D(left, top);
        }
    }


    public Coord2D getBottomRight() {
        if (bottom == Double.NEGATIVE_INFINITY || right == Double.NEGATIVE_INFINITY) {
            return new Coord2D(0, 0);
        } else {
            return new Coord2D(right, bottom);
        }
    }
}

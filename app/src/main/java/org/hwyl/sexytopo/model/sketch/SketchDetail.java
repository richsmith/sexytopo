package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.model.graph.Coord2D;


public abstract class SketchDetail {

    private final Colour colour;

    private float left;
    private float right;
    private float top;
    private float bottom;

    protected SketchDetail(Colour colour) {
        this.colour = colour;
        resetBoundingBox();
    }

    public Colour getColour() {
        return colour;
    }

    public Colour getDrawColour(boolean isDarkModeActive) {
        if (isDarkModeActive && colour == Colour.BLACK) {
            return Colour.WHITE;
        } else {
            return colour;
        }
    }

    public abstract float getDistanceFrom(Coord2D point);

    public abstract SketchDetail translate(Coord2D point);

    public boolean intersectsRectangle(Coord2D rectangleTopLeft, Coord2D rectangleBottomRight) {

        float left1 = rectangleTopLeft.x;
        float top1 = rectangleTopLeft.y;
        float right1 = rectangleBottomRight.x;
        float bottom1 = rectangleBottomRight.y;

        return (right >= left1 && left <= right1) && (top <= bottom1 && bottom >= top1);
    }

    protected void updateBoundingBox(Coord2D point) {
        left = Math.min(left, point.x);
        right = Math.max(right, point.x);
        top = Math.min(top, point.y);
        bottom = Math.max(bottom, point.y);

    }


    protected void resetBoundingBox() {
        left = Float.POSITIVE_INFINITY;
        right = Float.NEGATIVE_INFINITY;
        top = Float.POSITIVE_INFINITY;
        bottom = Float.NEGATIVE_INFINITY;
    }


    public float getWidth() {
        if (left == Float.POSITIVE_INFINITY || right == Float.NEGATIVE_INFINITY) {
            return 0;
        } else {
            return right - left;
        }
    }


    public float getHeight() {
        if (top == Float.POSITIVE_INFINITY || bottom == Float.NEGATIVE_INFINITY) {
            return 0;
        } else {
            return bottom - top;
        }
    }


    public Coord2D getTopLeft() {
        if (top == Float.POSITIVE_INFINITY || left == Float.POSITIVE_INFINITY) {
            return new Coord2D(0, 0);
        } else {
            return new Coord2D(left, top);
        }
    }


    public Coord2D getBottomRight() {
        if (bottom == Float.NEGATIVE_INFINITY || right == Float.NEGATIVE_INFINITY) {
            return new Coord2D(0, 0);
        } else {
            return new Coord2D(right, bottom);
        }
    }
}

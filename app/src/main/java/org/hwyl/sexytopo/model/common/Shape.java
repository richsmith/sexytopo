package org.hwyl.sexytopo.model.common;

import org.hwyl.sexytopo.model.graph.Coord2D;

/**
 * Cartesisan shapes for use in SexyTopo.
 * SexyTopo uses screen coords: y axis increases downwards.
 */
public class Shape {


    protected float left;
    protected float right;
    protected float top;
    protected float bottom;

    public Shape() {
        resetBoundingBox();
    }

    public void updateBoundingBox(Coord2D point) {
        left = Math.min(left, point.x);
        right = Math.max(right, point.x);
        top = Math.min(top, point.y);
        bottom = Math.max(bottom, point.y);
    }

    public void updateBoundingBox(Shape shape) {
        updateBoundingBox(shape.getTopLeft());
        updateBoundingBox(shape.getBottomRight());
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
            return Math.abs(right - left);
        }
    }


    public float getHeight() {
        if (top == Float.POSITIVE_INFINITY || bottom == Float.NEGATIVE_INFINITY) {
            return 0;
        } else {
            return Math.abs(bottom - top);
        }
    }


    public Coord2D getTopLeft() {
        return new Coord2D(getLeft(), getTop());
    }


    public Coord2D getBottomRight() {
        return new Coord2D(getRight(), getBottom());
    }

    public float getLeft() {
        if (left == Float.POSITIVE_INFINITY) {
            return 0;
        } else {
            return left;
        }
    }

    public float getRight() {
        if (right == Float.NEGATIVE_INFINITY) {
            return 0;
        } else {
            return right;
        }
    }

    public float getTop () {
        if (top == Float.POSITIVE_INFINITY) {
            return 0;
        } else {
            return top;
        }
    }

    public float getBottom() {
        if (bottom == Float.NEGATIVE_INFINITY) {
            return 0;
        } else {
            return bottom;
        }
    }


    public boolean intersectsRectangle(Coord2D rectangleTopLeft, Coord2D rectangleBottomRight) {

        float left1 = rectangleTopLeft.x;
        float top1 = rectangleTopLeft.y;
        float right1 = rectangleBottomRight.x;
        float bottom1 = rectangleBottomRight.y;

        return (right >= left1 && left <= right1) && (top <= bottom1 && bottom >= top1);
    }

    public void flipVertically() {
        // This is a bit hacky! Provided for flipping the Y axis for e.g. Therion export.
        // Do this after all other processing has been done, because it will break things.
        // Not sure if it's worth handling this properly...
        top = -top;
        bottom = -bottom;
    }

}

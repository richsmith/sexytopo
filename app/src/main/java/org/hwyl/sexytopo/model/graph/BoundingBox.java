package org.hwyl.sexytopo.model.graph;

import org.hwyl.sexytopo.control.util.TextTools;

public class BoundingBox {

    // uses SexyTopo coordinates (i.e. y increases downwards)

    private float left;
    private float right;
    private float top;
    private float bottom;

    public BoundingBox() {
        this(
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            Float.POSITIVE_INFINITY);
    }

    public BoundingBox(float left, float right, float top, float bottom) {
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }

    public Coord2D getTopLeft() {
        if (top == Float.POSITIVE_INFINITY || left == Float.POSITIVE_INFINITY) {
            return new Coord2D(0, 0);
        } else {
            return new Coord2D(left, top);
        }
    }

    public float getLeft() {
        if (left == Float.POSITIVE_INFINITY) {
            return 0;
        } else {
            return left;
        }
    }

    public float getBottom() {
        if (bottom == Float.NEGATIVE_INFINITY) {
            return 0;
        } else {
            return bottom;
        }
    }

    public float getTop() {
        if (top == Float.POSITIVE_INFINITY) {
            return 0;
        } else {
            return top;
        }
    }

    public float getRight() {
        if (right == Float.NEGATIVE_INFINITY) {
            return 0;
        } else {
            return right;
        }
    }

    public Coord2D getBottomRight() {
        if (bottom == Float.NEGATIVE_INFINITY || right == Float.NEGATIVE_INFINITY) {
            return new Coord2D(0, 0);
        } else {
            return new Coord2D(right, bottom);
        }
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

    public void update(Coord2D point) {
        left = Math.min(left, point.x);
        right = Math.max(right, point.x);
        top = Math.max(top, point.y);
        bottom = Math.min(bottom, point.y);
    }





    public BoundingBox scale(float scale) {
        return new BoundingBox(
            left * scale,
            right * scale,
            top * scale,
            bottom * scale);
    }

    public String toString() {
        return "L:" + TextTools.formatTo2dp(left) + " R:" + TextTools.formatTo2dp(right) + " T:" + TextTools.formatTo2dp(top) + " B:" + TextTools.formatTo2dp(bottom);
    }
}

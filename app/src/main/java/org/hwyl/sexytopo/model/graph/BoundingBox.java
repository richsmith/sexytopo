package org.hwyl.sexytopo.model.graph;

public class BoundingBox {

    private float left;
    private float right;
    private float top;
    private float bottom;

    public BoundingBox() {
        this(
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY);
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
        top = Math.min(top, point.y);
        bottom = Math.max(bottom, point.y);
    }

    public BoundingBox union(BoundingBox boundingBox) {
        return new BoundingBox(
                Math.min(left, boundingBox.left),
                Math.max(right, boundingBox.right),
                Math.min(top, boundingBox.top),
                Math.max(bottom, boundingBox.bottom)
        );
    }
}

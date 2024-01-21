package org.hwyl.sexytopo.model.common;

public class Frame extends Shape {

    public Frame() {
        super();
    }

    public Frame(float left, float right, float top, float bottom) {
        super();
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }

    public static Frame from(Shape shape) {
        return new Frame(
            shape.left,
            shape.right,
            shape.top,
            shape.bottom);
    }

    public Frame expandToNearest(int n) {
        return new Frame(
            roundDownTo(left, n),
            roundUpTo(right, n),
            roundDownTo(top, n),
            roundUpTo(bottom, n));
    }

    public Frame addPadding(int border) {
        return new Frame(
            left - border,
            right + border,
            top - border,
            bottom + border);
    }

    public Frame union(Frame other) {
        return new Frame(
            Math.min(left, other.left),
            Math.max(right, other.right),
            Math.min(top, other.top),
            Math.max(bottom, other.bottom));
    }

    public Frame scale(float factor) {
        return new Frame(
            left * factor,
            right * factor,
            top * factor,
            bottom * factor);
    }

    private static int roundUpTo(double value, int n) {
        return (int)Math.ceil(value / n) * n;
    }

    private static int roundDownTo(double value, int n) {
        return (int)Math.floor(value / n) * n;
    }
}

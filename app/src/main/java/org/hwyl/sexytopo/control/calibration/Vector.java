package org.hwyl.sexytopo.control.calibration;

public class Vector {
    public float x, y, z;

    public static Vector getZero() {
        return new Vector(0, 0, 0);
    }

    public Vector(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector(Vector vector) {
        this.x = vector.x;
        this.y = vector.y;
        this.z = vector.z;
    }

    public Vector minus(Vector other) {
        return minus(this, other);
    }

    public Vector plus(Vector other) {
        return plus(this, other);
    }

    public Vector times(float a) {
        return times(this, a);
    }

    public float times(Vector other) {
        return times(this, other);
    }

    public Vector crossProduct(Vector other) {
        return crossProduct(this, other);
    }

    public Matrix outerProduct(Vector a) { // outer product
        return outerProduct(this, a);
    }

    public static Vector plus(Vector a, Vector b) {
        return new Vector(a.x + b.x, a.y + b.y, a.z + b.z);
    }

    public static Vector minus(Vector a, Vector b) {
        return new Vector(a.x - b.x, a.y - b.y, a.z - b.z);
    }

    public static Vector times(Vector a, float b) {
        return new Vector(a.x * b, a.y * b, a.z * b);
    }

    public static float times(Vector a, Vector b) { // dot product
        return a.x * b.x + a.y * b.y + a.z * b.z;
    }

    public Vector normalised() {
        return Normalized(this);
    }

    public static Vector crossProduct(Vector a, Vector b) { // cross product
        return new Vector(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x);
    }

    public static Matrix outerProduct(Vector a, Vector b) { // outer product
        return new Matrix(times(b, a.x), times(b, a.y), times(b, a.z));
    }

    public static float Abs(Vector a) {
        return (float)Math.sqrt(a.x * a.x + a.y * a.y + a.z * a.z);
    }

    public Vector TurnX(float a) { // turn around X axis
        float s = (float)Math.sin(a);
        float c = (float)Math.cos(a);
        return new Vector(x, c * y - s * z, c * z + s * y);
    }

    public Vector TurnY(float a) { // turn around Y axis
        float s = (float)Math.sin(a);
        float c = (float)Math.cos(a);
        return new Vector(c * x + s * z, y, c * z - s * x);
    }

    public Vector TurnZ(float a) { // turn around Z axis
        float s = (float)Math.sin(a);
        float c = (float)Math.cos(a);
        return new Vector(c * x - s * y, c * y + s * x, z);
    }

    public static Vector Normalized(Vector a) {
        return times(a, 1 / Abs(a));
    }

    public static float MaxDiff(Vector a, Vector b) {
        return Math.max(Math.abs(a.x - b.x), Math.max(Math.abs(a.y - b.y), Math.abs(a.z - b.z)));
    }
}

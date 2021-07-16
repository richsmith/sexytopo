package org.hwyl.sexytopo.control.calibration;

public class Matrix {

    public final Vector x, y, z;

    public static Matrix getZero() {
        return new Matrix(
            Vector.getZero(),
            Vector.getZero(),
            Vector.getZero()
        );
    }

    public static Matrix getOne() {
        return new Matrix(
            new Vector(1, 0, 0),
            new Vector(0, 1, 0),
            new Vector(0, 0, 1));
    }

    public Matrix(Vector x, Vector y, Vector z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Matrix(Matrix matrix) {
        this.x = new Vector(matrix.x);
        this.y = new Vector(matrix.y);
        this.z = new Vector(matrix.z);
    }

    public Matrix times(float a) {
        return times(this, a);
    }

    public Vector times(Vector a) {
        return times(this, a);
    }

    public Matrix plus(Matrix a) {
        return plus(this, a);
    }

    public Matrix minus(Matrix a) {
        return minus(this, a);
    }

    public Matrix times(Matrix a) {
        return times(this, a);
    }

    public static Matrix plus(Matrix a, Matrix b) {
        return new Matrix(a.x.plus(b.x), a.y.plus(b.y), a.z.plus(b.z));
    }

    public static Matrix minus(Matrix a, Matrix b) {
        return new Matrix(a.x.minus(b.x), a.y.minus(b.y), a.z.minus(b.z));
    }

    public static Matrix times(Matrix a, float b) {
        return new Matrix(a.x.times(b), a.y.times(b), a.z.times(b));
    }

    public static Vector times(Matrix a, Vector b) {
        return new Vector(a.x.times(b), a.y.times(b), a.z.times(b));
    }

    public static Matrix times(Matrix a, Matrix b) {
        b = Transposed(b);
        return new Matrix(b.times(a.x), b.times(a.y), b.times(a.z));
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public static Matrix Transposed(Matrix m) {
        m = new Matrix(m);
        float t;
        t = m.x.y; m.x.y = m.y.x; m.y.x = t;
        t = m.x.z; m.x.z = m.z.x; m.z.x = t;
        t = m.y.z; m.y.z = m.z.y; m.z.y = t;
        return m;
    }

    public static Matrix Inverse(Matrix m) {
        m = Transposed(m);
        Matrix ad = new Matrix(m.y.crossProduct(m.z), m.z.crossProduct(m.x), m.x.crossProduct(m.y)); // adjugate
        return ad.times(1 / m.x.times(ad.x)); // adjugate * 1/determinant
    }

    public static float MaxDiff(Matrix a, Matrix b) {
        return Math.max(Vector.MaxDiff(a.x, b.x), Math.max(Vector.MaxDiff(a.y, b.y), Vector.MaxDiff(a.z, b.z)));
    }

}

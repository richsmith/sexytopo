package org.hwyl.sexytopo.model.graph;


public class Coord3D extends Coord {


    public static final Coord3D ORIGIN = new Coord3D(0, 0, 0);

    private final double x, y, z;

    public Coord3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) {
            return false;
        } else {
            Coord3D coord3D = (Coord3D)object;

            return (coord3D.x == x) && (coord3D.y == y) && (coord3D.z == z);
        }
    }

    @Override
    public int hashCode() {
        double result = x;
        result = 31 * result + y;
        result = 31 * result + z;
        return (int)result;
    }


    @Override
    public String toString() {
        return String.format("(%f, %f, %f)", x, y, z);
    }
}
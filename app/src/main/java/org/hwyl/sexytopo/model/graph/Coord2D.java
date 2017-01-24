package org.hwyl.sexytopo.model.graph;


public class Coord2D extends Coord {

    public static final Coord2D ORIGIN = new Coord2D(0, 0);

    private final double x, y;

    public Coord2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Coord2D add(double x, double y) {
        return new Coord2D(getX() + x, getY() + y);
    }

    public Coord2D plus(Coord2D coord2D) {
        return new Coord2D(this.x + coord2D.x, this.y + coord2D.y);
    }

    public Coord2D minus(Coord2D coord2D) {
        return new Coord2D(this.x - coord2D.x, this.y - coord2D.y);
    }

    public Coord2D scale(double scale) {
        return new Coord2D(x * scale, y * scale);
    }

    public String toString() {
        return String.format("(%.2f, %.2f)", x, y);
    }

    public Coord2D flipVertically() {
        return new Coord2D(x, -y);
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) {
            return false;
        } else {
            Coord2D coord2D = (Coord2D)object;

            return (coord2D.x == x) && (coord2D.y == y);
        }
    }

    @Override
    public int hashCode() {
        double result = x;
        result = 31 * result + y;
        return (int)result;
    }
}

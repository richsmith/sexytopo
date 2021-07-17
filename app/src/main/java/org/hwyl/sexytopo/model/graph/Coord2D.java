package org.hwyl.sexytopo.model.graph;


import android.annotation.SuppressLint;

import org.hwyl.sexytopo.control.util.NumberTools;


@SuppressWarnings("unused")
public final class Coord2D extends Coord {

    public static final Coord2D ORIGIN = new Coord2D(0, 0);

    public final float x, y;

    public Coord2D(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Coord2D add(float x, float y) {
        return new Coord2D(this.x + x, this.y + y);
    }

    public Coord2D plus(Coord2D coord2D) {
        return new Coord2D(this.x + coord2D.x, this.y + coord2D.y);
    }

    public Coord2D minus(Coord2D coord2D) {
        return new Coord2D(this.x - coord2D.x, this.y - coord2D.y);
    }

    public Coord2D multiply(Coord2D coord2D) {
        return new Coord2D(this.x * coord2D.x, this.y * coord2D.y);
    }

    public Coord2D scale(float scale) {
        return new Coord2D(x * scale, y * scale);
    }

    public float mag() {
        return (float) Math.sqrt(this.x * this.x + this.y * this.y);
    }

    public Coord2D normalise() {
        float mag = this.mag();
        if (mag > 0)
            return this.scale(1/mag);
        return this;
    }

    @SuppressLint("DefaultLocale")
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
            return NumberTools.isWithinDelta(coord2D.x, x) &&
                    NumberTools.isWithinDelta(coord2D.y, y);
        }
    }

    @Override
    public int hashCode() {
        float result = x;
        result = 31 * result + y;
        return (int)result;
    }
}

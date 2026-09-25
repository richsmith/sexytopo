package org.hwyl.sexytopo.model.geometry;

public abstract class Coord<C extends Coord<C>> {

    public abstract C scale(float scale);
}

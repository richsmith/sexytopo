package org.hwyl.sexytopo.model.graph;

public abstract class Coord<C extends Coord<C>> {

    public abstract C scale(float scale);
}

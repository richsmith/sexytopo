package org.hwyl.sexytopo.model.graph;

/**
 * Created by rls on 27/07/14.
 */
public class Line<T extends Coord> {
    private final T start;
    private final T end;

    public Line(T start, T end) {
        this.start = start;
        this.end = end;
    }

    public T getStart() {
        return start;
    }

    public T getEnd() {
        return end;
    }

    public String toString() {
        return start + " -> " + end;
    }
}
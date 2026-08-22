package org.hwyl.sexytopo.model.graph;

public final class Line<T extends Coord<T>> {
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

    public Line<T> scale(float scale) {
        return new Line<>(start.scale(scale), end.scale(scale));
    }
}

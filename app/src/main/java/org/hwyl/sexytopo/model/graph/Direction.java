package org.hwyl.sexytopo.model.graph;

public enum Direction {
    LEFT,
    RIGHT,
    VERTICAL;

    public Direction opposite() {
        if (this == LEFT) return RIGHT;
        if (this == RIGHT) return LEFT;
        return VERTICAL;
    }
}

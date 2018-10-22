package org.hwyl.sexytopo.model.graph;

public enum Direction {
    LEFT, RIGHT;

    public Direction opposite() {
        return (this == LEFT)? RIGHT : LEFT;
    }
}
package org.hwyl.sexytopo.model.graph

enum class Direction {
    LEFT, RIGHT;

    fun opposite(): Direction {
        return if (this == LEFT) RIGHT else LEFT;
    }
}
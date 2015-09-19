package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.model.graph.Coord2D;

/**
 * Created by rls on 01/06/15.
 */
public abstract class SketchDetail {

    private final Colour colour;

    protected SketchDetail(Colour colour) {
        this.colour = colour;
    }

    public Colour getColour() {
        return colour;
    }

    public abstract double getDistanceFrom(Coord2D point);
}

package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.model.common.Shape;
import org.hwyl.sexytopo.model.graph.Coord2D;


public abstract class SketchDetail extends Shape {

    private final Colour colour;


    protected SketchDetail(Colour colour) {
        super();
        this.colour = colour;
    }

    public Colour getColour() {
        return colour;
    }

    public Colour getDrawColour(boolean isDarkModeActive) {
        if (isDarkModeActive && colour == Colour.BLACK) {
            return Colour.WHITE;
        } else {
            return colour;
        }
    }

    public String getXviString() {
        return null;
    }

    public abstract float getDistanceFrom(Coord2D point);

    public abstract SketchDetail translate(Coord2D point);

    public abstract SketchDetail scale(float scale);


}

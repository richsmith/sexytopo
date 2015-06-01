package org.hwyl.sexytopo.model.sketch;

/**
 * Created by rls on 01/06/15.
 */
public abstract class SketchDetail {

    private final int colour;

    protected SketchDetail(int colour) {
        this.colour = colour;
    }

    public int getColour() {
        return colour;
    }
}

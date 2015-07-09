package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.model.graph.Coord2D;

/**
 * Created by rls on 01/06/15.
 */
public class TextDetail extends SketchDetail {

    private final Coord2D location;
    private final String text;

    public TextDetail(Coord2D location, String text, Colour colour) {
        super(colour);
        this.location = location;
        this.text = text;
    }

    public Coord2D getLocation() {
        return location;
    }

    public String getText() {
        return text;
    }

}

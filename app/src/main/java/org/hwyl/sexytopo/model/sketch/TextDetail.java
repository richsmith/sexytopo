package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.model.graph.Coord2D;


public class TextDetail extends SinglePositionDetail {

    private final String text;

    public TextDetail(Coord2D location, String text, Colour colour) {
        super(colour, location);
        this.text = text;
    }

    public String getText() {
        return text;
    }


    @Override
    public TextDetail translate(Coord2D point) {
        return new TextDetail(getPosition().plus(point), getText(), getColour());
    }
}

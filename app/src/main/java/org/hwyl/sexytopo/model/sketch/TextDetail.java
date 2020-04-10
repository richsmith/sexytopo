package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.model.graph.Coord2D;


public class TextDetail extends SinglePositionDetail implements AutoScalableDetail {

    private final String text;
    private final float size;

    public TextDetail(Coord2D location, String text, Colour colour, float size) {
        super(colour, location);
        this.text = text;
        this.size = size;
    }

    public String getText() {
        return text;
    }

    public float getSize() {
        return size;
    }

    @Override
    public TextDetail translate(Coord2D point) {
        return new TextDetail(getPosition().plus(point), getText(), getColour(), getSize());
    }
}

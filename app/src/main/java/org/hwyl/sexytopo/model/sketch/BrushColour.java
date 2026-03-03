package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.R;


public enum BrushColour {

    BLACK(R.id.buttonBlack, Colour.BLACK),
    BROWN(R.id.buttonBrown, Colour.BROWN),
    GREY(R.id.buttonGrey, Colour.GREY),
    RED(R.id.buttonRed, Colour.RED),
    ORANGE(R.id.buttonOrange, Colour.ORANGE),
    GREEN(R.id.buttonGreen, Colour.GREEN),
    BLUE(R.id.buttonBlue, Colour.BLUE),
    PURPLE(R.id.buttonPurple, Colour.PURPLE);

    private final int id;
    private final Colour colour;

    private static final BrushColour DEFAULT = BLACK;

    BrushColour(int id, Colour colour) {
        this.id = id;
        this.colour = colour;
    }

    public static BrushColour fromString(String name) {
        return name == null? DEFAULT : BrushColour.valueOf(name);
    }

    public int getId() {
        return id;
    }

    public Colour getColour() {
        return colour;
    }

}
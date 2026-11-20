package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.R;

public enum BrushColour {
    BLACK(R.id.buttonBlack, Colour.BLACK, R.string.sketch_toolbar_colour_main),
    BROWN(R.id.buttonBrown, Colour.BROWN, R.string.sketch_toolbar_colour_brown),
    GREY(R.id.buttonGrey, Colour.GREY, R.string.sketch_toolbar_colour_grey),
    RED(R.id.buttonRed, Colour.RED, R.string.sketch_toolbar_colour_red),
    ORANGE(R.id.buttonOrange, Colour.ORANGE, R.string.sketch_toolbar_colour_orange),
    GREEN(R.id.buttonGreen, Colour.GREEN, R.string.sketch_toolbar_colour_green),
    BLUE(R.id.buttonBlue, Colour.BLUE, R.string.sketch_toolbar_colour_blue),
    PURPLE(R.id.buttonPurple, Colour.PURPLE, R.string.sketch_toolbar_colour_purple);

    private final int id;
    private final Colour colour;
    private final int stringId;

    private static final BrushColour DEFAULT = BLACK;

    BrushColour(int id, Colour colour, int stringId) {
        this.id = id;
        this.colour = colour;
        this.stringId = stringId;
    }

    public static BrushColour fromString(String name) {
        return name == null ? DEFAULT : BrushColour.valueOf(name);
    }

    public int getId() {
        return id;
    }

    public Colour getColour() {
        return colour;
    }

    public int getStringId() {
        return stringId;
    }
}

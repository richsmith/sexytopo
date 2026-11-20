package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.control.util.SketchPreferences.Toggle;

/**
 * A colour some kinds of feature are always drawn in, whatever the selected brush colour, while its
 * quick-menu mode is on: blue for water and brown for mud.
 */
public enum AutoColour {
    NONE(null, null),
    WATER(Colour.WATER, Toggle.BLUE_WATER),
    MUD(Colour.MUD, Toggle.BROWN_MUD);

    private final Colour colour;
    private final Toggle toggle;

    AutoColour(Colour colour, Toggle toggle) {
        this.colour = colour;
        this.toggle = toggle;
    }

    public Colour getColour() {
        return colour;
    }

    public boolean isOn() {
        return toggle != null && toggle.isOn();
    }

    /** The colour to draw a feature in: this one if its mode is on, otherwise the given colour. */
    public Colour applyTo(Colour colour) {
        return isOn() ? this.colour : colour;
    }
}

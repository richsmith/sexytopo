package org.hwyl.sexytopo.model.sketch;

import android.graphics.Color;

/**
 * Created by rls on 08/07/15.
 */
public enum Colour {

    NONE(0),
    BLACK(Color.BLACK),
    BROWN(0xFFA52A2A),
    ORANGE(0xFFFFA500),
    GREEN(0xFF00DD00),
    BLUE(Color.BLUE),
    PURPLE(Color.MAGENTA);

    public final int intValue;

    private Colour(int intValue) {
        this.intValue = intValue;
    }
}

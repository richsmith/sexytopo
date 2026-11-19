package org.hwyl.sexytopo.model.sketch;

import static org.hwyl.sexytopo.model.sketch.SketchTool.Trait.DRAWS_PATH;
import static org.hwyl.sexytopo.model.sketch.SketchTool.Trait.MODAL;
import static org.hwyl.sexytopo.model.sketch.SketchTool.Trait.OFFERS_LINE_MENU;
import static org.hwyl.sexytopo.model.sketch.SketchTool.Trait.USES_COLOUR;

import java.util.EnumSet;
import java.util.Set;
import org.hwyl.sexytopo.R;

public enum SketchTool {
    MOVE(R.id.buttonMove, OFFERS_LINE_MENU),
    LINE(R.id.buttonLine, USES_COLOUR, DRAWS_PATH, OFFERS_LINE_MENU),
    AREA(R.id.buttonArea, USES_COLOUR, DRAWS_PATH, OFFERS_LINE_MENU),
    ERASE(R.id.buttonErase),
    SYMBOL(R.id.buttonSymbol, USES_COLOUR),
    TEXT(387348957, USES_COLOUR),
    SELECT(R.id.buttonSelect, OFFERS_LINE_MENU),
    POSITION_CROSS_SECTION(-1),
    ROTATE_CROSS_SECTION(-1),
    MOVE_CROSS_SECTION(-1, MODAL),
    PINCH_TO_ZOOM(-1, MODAL),
    MODAL_MOVE(-1, MODAL);

    enum Trait {
        /** Draws in the brush colour. */
        USES_COLOUR,
        /** Only lasts as long as the gesture that started it. */
        MODAL,
        /** Starts an in-progress line or area as soon as the finger lands. */
        DRAWS_PATH,
        /**
         * A long press on a line opens its context menu. Not offered by tools that act as soon as
         * the finger lands (erasing, placing a symbol or text), since by then a long press has
         * already done something, nor while placing or adjusting a cross-section.
         */
        OFFERS_LINE_MENU
    }

    private final int id;
    private final Set<Trait> traits;

    private static final SketchTool DEFAULT = MOVE;

    SketchTool(int id, Trait... traits) {
        this.id = id;
        this.traits =
                traits.length == 0 ? EnumSet.noneOf(Trait.class) : EnumSet.of(traits[0], traits);
    }

    public static SketchTool fromString(String name) {
        if (name == null) {
            return DEFAULT;
        }
        // The line tool was saved as DRAW before it was renamed
        if (name.equals("DRAW")) {
            return LINE;
        }
        return SketchTool.valueOf(name);
    }

    public int getId() {
        return id;
    }

    public boolean usesColour() {
        return traits.contains(USES_COLOUR);
    }

    public boolean isModal() {
        return traits.contains(MODAL);
    }

    public boolean drawsPath() {
        return traits.contains(DRAWS_PATH);
    }

    public boolean offersLineMenu() {
        return traits.contains(OFFERS_LINE_MENU);
    }
}

package org.hwyl.sexytopo.model.sketch;

import static org.hwyl.sexytopo.model.sketch.SketchTool.Trait.DRAWS_PATH;
import static org.hwyl.sexytopo.model.sketch.SketchTool.Trait.MODAL;
import static org.hwyl.sexytopo.model.sketch.SketchTool.Trait.OFFERS_DETAIL_MENU;
import static org.hwyl.sexytopo.model.sketch.SketchTool.Trait.ONE_USE;
import static org.hwyl.sexytopo.model.sketch.SketchTool.Trait.OPENS_LINKED_SURVEYS;
import static org.hwyl.sexytopo.model.sketch.SketchTool.Trait.USES_COLOUR;

import java.util.EnumSet;
import java.util.Set;
import org.hwyl.sexytopo.R;

public enum SketchTool {
    MOVE(R.id.buttonMove, OFFERS_DETAIL_MENU, OPENS_LINKED_SURVEYS),
    LINE(R.id.buttonLine, USES_COLOUR, DRAWS_PATH, OFFERS_DETAIL_MENU),
    AREA(R.id.buttonArea, USES_COLOUR, DRAWS_PATH, OFFERS_DETAIL_MENU),
    ERASE(R.id.buttonErase),
    SYMBOL(R.id.buttonSymbol, USES_COLOUR, OFFERS_DETAIL_MENU),
    TEXT(387348957, USES_COLOUR, OFFERS_DETAIL_MENU),
    // not currently on the toolbar
    SELECT(-1, OFFERS_DETAIL_MENU, OPENS_LINKED_SURVEYS),
    POSITION_CROSS_SECTION(-1, ONE_USE),
    ROTATE_CROSS_SECTION(-1, ONE_USE),
    MOVE_CROSS_SECTION(-1, MODAL),
    MOVE_DETAIL(-1, ONE_USE),
    RESIZE_DETAIL(-1, ONE_USE),
    PINCH_TO_ZOOM(-1, MODAL),
    MODAL_MOVE(-1, MODAL);

    enum Trait {
        /** Draws in the brush colour. */
        USES_COLOUR,
        /** Only lasts as long as the gesture that started it. */
        MODAL,
        /** Does one thing, then goes back to the tool in use before. */
        ONE_USE,
        /** Starts an in-progress line or area as soon as the finger lands. */
        DRAWS_PATH,
        /** A long press on a line or area opens its context menu. */
        OFFERS_DETAIL_MENU,
        /** A tap on a linked survey in this mode triggers an offer to open it */
        OPENS_LINKED_SURVEYS
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

    public boolean isOneUse() {
        return traits.contains(ONE_USE);
    }

    public boolean drawsPath() {
        return traits.contains(DRAWS_PATH);
    }

    public boolean offersDetailMenu() {
        return traits.contains(OFFERS_DETAIL_MENU);
    }

    public boolean opensLinkedSurveys() {
        return traits.contains(OPENS_LINKED_SURVEYS);
    }
}

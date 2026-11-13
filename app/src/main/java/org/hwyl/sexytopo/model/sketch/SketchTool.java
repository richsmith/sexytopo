package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.R;

public enum SketchTool {
    MOVE(R.id.buttonMove, false, false),
    LINE(R.id.buttonLine, true, false),
    AREA(R.id.buttonArea, true, false),
    ERASE(R.id.buttonErase, false, false),
    SYMBOL(R.id.buttonSymbol, true, false),
    TEXT(387348957, true, false),
    SELECT(R.id.buttonSelect, false, false),
    POSITION_CROSS_SECTION(-1, false, false),
    ROTATE_CROSS_SECTION(-1, false, false),
    MOVE_CROSS_SECTION(-1, false, true),
    PINCH_TO_ZOOM(-1, false, true),
    MODAL_MOVE(-1, false, true);

    private final int id;
    private final boolean usesColour;
    private final boolean isModal;

    private static final SketchTool DEFAULT = MOVE;

    SketchTool(int id, boolean usesColour, boolean isModal) {
        this.id = id;
        this.usesColour = usesColour;
        this.isModal = isModal;
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
        return usesColour;
    }

    public boolean isModal() {
        return isModal;
    }
}

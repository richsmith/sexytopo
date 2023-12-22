package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.R;


public enum SketchTool {
    MOVE(R.id.buttonMove, false, false),
    DRAW(R.id.buttonDraw, true, false),
    ERASE(R.id.buttonErase, false, false),
    SYMBOL(R.id.buttonSymbol, true, false),
    TEXT(387348957, true, false),
    SELECT(R.id.buttonSelect, false, false),
    POSITION_CROSS_SECTION(R.id.graph_station_new_cross_section, false, false),
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
        return name == null? DEFAULT : SketchTool.valueOf(name);
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
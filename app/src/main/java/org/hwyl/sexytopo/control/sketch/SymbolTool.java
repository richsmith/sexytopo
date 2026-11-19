package org.hwyl.sexytopo.control.sketch;

import android.view.MotionEvent;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.SexyTopo;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.sketch.Symbol;

/**
 * Places the chosen symbol where the user lifts. A directional symbol points the way the finger was
 * dragged. The text "symbol" hands over to the text tool.
 */
class SymbolTool implements ToolHandler {

    private final Viewport viewport;
    private final SketchState state;
    private final ToolHost host;
    private final TextTool textTool;

    private Symbol symbol = Symbol.getDefault();

    // where the press started, to place the symbol and, for a directional one, to point it from
    private Coord2D actionDownPointOnView = Coord2D.ORIGIN;

    SymbolTool(Viewport viewport, SketchState state, ToolHost host, TextTool textTool) {
        this.viewport = viewport;
        this.state = state;
        this.host = host;
        this.textTool = textTool;
    }

    void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }

    @Override
    public boolean onTouch(MotionEvent event) {

        if (symbol == Symbol.TEXT) {
            return textTool.onTouch(event);
        }

        final Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        final Coord2D touchPointOnSurvey = viewport.toSurvey(touchPointOnView);
        float startingSizeDp = GeneralPreferences.getSymbolStartingSizeDp();
        float startingSizePixels = SexyTopo.dpToPixels(startingSizeDp);
        float size = startingSizePixels / viewport.getScale();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Only record the start point; the symbol is placed on ACTION_UP so a long-press
                // that opens a station menu doesn't also leave a symbol behind.
                actionDownPointOnView = touchPointOnView;
                return true;

            case MotionEvent.ACTION_UP:
                if (symbol.isDirectional()) {
                    float angle =
                            Space2DUtils.getAngleBetween(actionDownPointOnView, touchPointOnView);
                    angle = Space2DUtils.adjustAngle(angle, -90);
                    Coord2D firstTouch = viewport.toSurvey(actionDownPointOnView);
                    state.getSketch()
                            .addSymbolDetail(firstTouch, symbol, state.getColour(), size, angle);
                    float distance =
                            Space2DUtils.getDistance(actionDownPointOnView, touchPointOnView);
                    if (distance < 5) {
                        host.showToast(R.string.sketch_symbol_orientation_education);
                    }
                } else {
                    state.getSketch()
                            .addSymbolDetail(
                                    touchPointOnSurvey, symbol, state.getColour(), size, 0);
                }
                host.invalidate();
                return true;

            default:
                return false;
        }
    }
}

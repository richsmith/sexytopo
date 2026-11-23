package org.hwyl.sexytopo.control.sketch;

import static org.hwyl.sexytopo.control.sketch.ScreenUnits.dpToPixels;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.sketch.SketchTool;
import org.hwyl.sexytopo.model.sketch.SymbolDetail;

/**
 * Moves a symbol, chosen from its context menu, by dragging. The drag can start anywhere, so the
 * finger needn't cover the symbol: it moves by as much as the finger does. Until then the symbol is
 * highlighted, so it's clear which one will move. Lifting the finger puts it down, as a single
 * undoable step, and goes back to the tool in use before.
 */
class MoveSymbolTool implements ToolHandler {

    // how far the highlight's corners stand off from the symbol
    private static final float HIGHLIGHT_MARGIN_DP = 4;

    private final Viewport viewport;
    private final SketchState state;
    private final SketchRenderer sketchRenderer;
    private final ToolHost host;

    private final Paint highlightPaint = new Paint();

    private SymbolDetail beingMoved = null;
    private SketchTool toolToReturnTo = SketchTool.MOVE;

    // where the drag started, and how far it's gone, in survey coordinates
    private Coord2D dragStartOnSurvey = null;
    private Coord2D dragOffset = Coord2D.ORIGIN;

    MoveSymbolTool(
            Viewport viewport, SketchState state, SketchRenderer sketchRenderer, ToolHost host) {
        this.viewport = viewport;
        this.state = state;
        this.sketchRenderer = sketchRenderer;
        this.host = host;
    }

    /** Set the highlight up from the theme. */
    void initialise(Context context) {
        SketchPaints.setUpHighlightPaint(context, highlightPaint);
    }

    void start(SymbolDetail symbolDetail) {
        beingMoved = symbolDetail;
        dragStartOnSurvey = null;
        dragOffset = Coord2D.ORIGIN;
        // A pinch or pan part way through comes back to this tool, so remember the one to go back
        // to here rather than relying on the previous tool
        toolToReturnTo = state.getSketchTool();
        state.setSketchTool(SketchTool.MOVE_SYMBOL);
        // drawn here instead while it's waiting to be moved
        sketchRenderer.setHiddenSymbol(symbolDetail);
        host.showToast(R.string.sketch_move_symbol_instruction);
        host.invalidate();
    }

    @Override
    public boolean onTouch(MotionEvent event) {
        if (!isActive()) {
            finish();
            return false;
        }

        Coord2D touchPointOnSurvey = viewport.toSurvey(new Coord2D(event.getX(), event.getY()));

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                dragStartOnSurvey = touchPointOnSurvey;
                dragOffset = Coord2D.ORIGIN;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (dragStartOnSurvey != null) {
                    dragOffset = touchPointOnSurvey.minus(dragStartOnSurvey);
                    host.invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (dragStartOnSurvey != null) {
                    Coord2D offset = touchPointOnSurvey.minus(dragStartOnSurvey);
                    if (offset.x != 0 || offset.y != 0) {
                        state.getSketch().moveSymbolDetail(beingMoved, offset);
                    }
                }
                finish();
                return true;

            case MotionEvent.ACTION_CANCEL:
                finish();
                return true;

            default:
                return false;
        }
    }

    /**
     * Draw the symbol being moved where it will go, highlighted. Drawn over everything else, as
     * it's what's being worked on.
     */
    void draw(Canvas canvas) {
        if (beingMoved == null) {
            return;
        }
        if (!isActive()) {
            // given up on without a touch, so put the symbol back in its sketch
            finish();
            return;
        }
        SymbolDetail preview = beingMoved.translate(dragOffset);
        Coord2D centre = viewport.toView(preview.getPosition());
        float size = preview.getSize() * viewport.getScale() + 2 * dpToPixels(HIGHLIGHT_MARGIN_DP);
        sketchRenderer.drawSymbol(canvas, preview);
        CanvasShapes.drawHighlightBrackets(canvas, centre.x, centre.y, size, highlightPaint);
    }

    /**
     * Whether a symbol is waiting to be moved. It may have gone from the sketch meanwhile (undone,
     * say), or the user may have picked another tool instead.
     */
    private boolean isActive() {
        return beingMoved != null
                && state.getSketchTool() == SketchTool.MOVE_SYMBOL
                && state.getSketch().getSymbolDetails().contains(beingMoved);
    }

    private void finish() {
        beingMoved = null;
        dragStartOnSurvey = null;
        dragOffset = Coord2D.ORIGIN;
        sketchRenderer.setHiddenSymbol(null);
        if (state.getSketchTool() == SketchTool.MOVE_SYMBOL) {
            state.setSketchTool(toolToReturnTo);
        }
        host.invalidate();
    }
}

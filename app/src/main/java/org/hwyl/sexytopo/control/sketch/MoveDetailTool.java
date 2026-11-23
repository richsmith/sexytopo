package org.hwyl.sexytopo.control.sketch;

import static org.hwyl.sexytopo.control.sketch.ScreenUnits.dpToPixels;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.sketch.SinglePositionDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.SketchTool;

/**
 * Moves a symbol or piece of text, chosen from its context menu, by dragging. The drag can start
 * anywhere, so the finger needn't cover it: it moves by as much as the finger does. Until then it's
 * highlighted, so it's clear what will move. Lifting the finger puts it down, as a single undoable
 * step, and goes back to the tool in use before.
 */
class MoveDetailTool implements ToolHandler {

    // how far the highlight's corners stand off from what's being moved
    private static final float HIGHLIGHT_MARGIN_DP = 4;

    private final Viewport viewport;
    private final SketchState state;
    private final SketchRenderer sketchRenderer;
    private final ToolHost host;

    private final Paint highlightPaint = new Paint();

    private SinglePositionDetail beingMoved = null;
    private SketchTool toolToReturnTo = SketchTool.MOVE;

    // where the drag started, and how far it's gone, in survey coordinates
    private Coord2D dragStartOnSurvey = null;
    private Coord2D dragOffset = Coord2D.ORIGIN;

    MoveDetailTool(
            Viewport viewport, SketchState state, SketchRenderer sketchRenderer, ToolHost host) {
        this.viewport = viewport;
        this.state = state;
        this.sketchRenderer = sketchRenderer;
        this.host = host;
        sketchRenderer.setHiddenDetail(this::getDetailBeingMoved);
    }

    /** Set the highlight up from the theme. */
    void initialise(Context context) {
        SketchPaints.setUpHighlightPaint(context, highlightPaint);
    }

    void start(SinglePositionDetail detail) {
        beingMoved = detail;
        dragStartOnSurvey = null;
        dragOffset = Coord2D.ORIGIN;
        // A pinch or pan part way through comes back to this tool, so remember the one to go back
        // to here rather than relying on the previous tool
        toolToReturnTo = state.getSketchTool();
        state.setSketchTool(SketchTool.MOVE_DETAIL);
        host.showToast(R.string.sketch_move_detail_instruction);
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
                        state.getSketch().moveDetail(beingMoved, offset);
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

    /** Draw what's being moved where it will go, highlighted. */
    @Override
    public void drawFeedback(Canvas canvas) {
        if (!isActive()) {
            return;
        }
        SinglePositionDetail preview = (SinglePositionDetail) beingMoved.translate(dragOffset);
        sketchRenderer.drawDetail(canvas, preview);
        RectF box = sketchRenderer.getBoundsOnView(preview);
        float margin = dpToPixels(HIGHLIGHT_MARGIN_DP);
        box.inset(-margin, -margin);
        CanvasShapes.drawHighlightBrackets(canvas, box, highlightPaint);
    }

    private SinglePositionDetail getDetailBeingMoved() {
        return isActive() ? beingMoved : null;
    }

    /**
     * Whether something is waiting to be moved. It may have gone from the sketch meanwhile (undone,
     * say), or the user may have picked another tool instead.
     */
    private boolean isActive() {
        if (beingMoved == null || state.getUnderlyingSketchTool() != SketchTool.MOVE_DETAIL) {
            return false;
        }
        Sketch sketch = state.getSketch();
        return sketch.getSymbolDetails().contains(beingMoved)
                || sketch.getTextDetails().contains(beingMoved);
    }

    private void finish() {
        beingMoved = null;
        dragStartOnSurvey = null;
        dragOffset = Coord2D.ORIGIN;
        if (state.getSketchTool() == SketchTool.MOVE_DETAIL) {
            state.setSketchTool(toolToReturnTo);
        }
        host.invalidate();
    }
}

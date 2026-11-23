package org.hwyl.sexytopo.control.sketch;

import static org.hwyl.sexytopo.control.sketch.ScreenUnits.dpToPixels;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.sketch.SinglePositionDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.SketchTool;

/**
 * Changes a symbol or piece of text, chosen from its context menu, by a drag starting anywhere.
 * It's highlighted and shown as it will be; the drag ending makes the change, as a single undoable
 * step, and goes back to the tool in use before.
 */
abstract class DragDetailTool implements ToolHandler {

    // how far the highlight's corners stand off from what's being changed
    private static final float HIGHLIGHT_MARGIN_DP = 4;

    protected final Viewport viewport;
    protected final SketchRenderer sketchRenderer;
    private final SketchState state;
    private final ToolHost host;
    private final SketchTool sketchTool;
    private final int instructionId;

    private final Paint highlightPaint = new Paint();

    private SinglePositionDetail beingDragged = null;

    // where the drag started and where it's got to, on the view
    private Coord2D dragStart = null;
    private Coord2D dragEnd = null;

    DragDetailTool(
            Viewport viewport,
            SketchState state,
            SketchRenderer sketchRenderer,
            ToolHost host,
            SketchTool sketchTool,
            int instructionId) {
        this.viewport = viewport;
        this.state = state;
        this.sketchRenderer = sketchRenderer;
        this.host = host;
        this.sketchTool = sketchTool;
        this.instructionId = instructionId;
    }

    /**
     * The detail as a drag between two points on the view would leave it, or the detail itself if
     * the drag changes nothing.
     */
    abstract SinglePositionDetail drag(SinglePositionDetail detail, Coord2D from, Coord2D to);

    /** Make the change a finished drag stands for, as a single undoable step. */
    abstract void commit(Sketch sketch, SinglePositionDetail detail, Coord2D from, Coord2D to);

    /** Set the highlight up from the theme. */
    void initialise(Context context) {
        SketchPaints.setUpHighlightPaint(context, highlightPaint);
    }

    void start(SinglePositionDetail detail) {
        beingDragged = detail;
        dragStart = null;
        dragEnd = null;
        state.startTemporaryTool(sketchTool);
        host.showToast(instructionId);
        host.invalidate();
    }

    @Override
    public boolean onTouch(MotionEvent event) {
        if (!isActive()) {
            finish();
            return false;
        }

        Coord2D touchPoint = new Coord2D(event.getX(), event.getY());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                dragStart = touchPoint;
                dragEnd = touchPoint;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (dragStart != null) {
                    dragEnd = touchPoint;
                    host.invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (dragStart != null) {
                    commit(state.getSketch(), beingDragged, dragStart, touchPoint);
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

    /** Draw what's being changed as it will be, highlighted. */
    @Override
    public void drawFeedback(Canvas canvas) {
        if (!isActive()) {
            return;
        }
        SinglePositionDetail preview =
                dragStart == null ? beingDragged : drag(beingDragged, dragStart, dragEnd);
        sketchRenderer.drawDetail(canvas, preview);
        RectF box = sketchRenderer.getBoundsOnView(preview);
        float margin = dpToPixels(HIGHLIGHT_MARGIN_DP);
        box.inset(-margin, -margin);
        CanvasShapes.drawHighlightBrackets(canvas, box, highlightPaint);
    }

    @Override
    public SinglePositionDetail getHiddenDetail() {
        return isActive() ? beingDragged : null;
    }

    /**
     * Whether something is waiting to be changed. It may have gone from the sketch meanwhile
     * (undone, say), or the user may have picked another tool instead.
     */
    private boolean isActive() {
        if (beingDragged == null || state.getUnderlyingSketchTool() != sketchTool) {
            return false;
        }
        Sketch sketch = state.getSketch();
        return sketch.getSymbolDetails().contains(beingDragged)
                || sketch.getTextDetails().contains(beingDragged);
    }

    private void finish() {
        beingDragged = null;
        dragStart = null;
        dragEnd = null;
        if (state.getSketchTool() == sketchTool) {
            state.endTemporaryTool();
        }
        host.invalidate();
    }
}

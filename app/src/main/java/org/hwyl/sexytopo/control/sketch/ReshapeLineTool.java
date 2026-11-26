package org.hwyl.sexytopo.control.sketch;

import android.graphics.Canvas;
import android.view.MotionEvent;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.SketchTool;

/**
 * Reshapes a line, chosen from its context menu, by pulling part of it: a drag starting on the line
 * takes the nearest point on it along with the drag, and the line either side bends smoothly to
 * follow. The further the pull, the more of the line bends, so a short tug makes a small bulge and
 * a long one a sweeping curve. The line is shown as it will be, and the drag ending makes the
 * change, as a single undoable step, and goes back to the tool in use before. A touch away from the
 * line leaves it as it was.
 */
class ReshapeLineTool implements ToolHandler {

    // how much of the line either side of the grab bends, as a multiple of how far it's pulled
    private static final float REACH_PER_PULL = 2.5f;
    // the least the line bends either side of the grab, so a small nudge isn't a sharp kink
    private static final float MINIMUM_REACH_DP = 40;
    // how near an end of the line a grab has to be to pull the end itself
    private static final float END_GRAB_DP = 12;

    private final Viewport viewport;
    private final SketchState state;
    private final SketchRenderer sketchRenderer;
    private final ToolHost host;

    private PathDetail beingReshaped = null;

    // where the drag started and where it's got to, on the view
    private Coord2D dragStart = null;
    private Coord2D dragEnd = null;

    ReshapeLineTool(
            Viewport viewport, SketchState state, SketchRenderer sketchRenderer, ToolHost host) {
        this.viewport = viewport;
        this.state = state;
        this.sketchRenderer = sketchRenderer;
        this.host = host;
    }

    void start(PathDetail pathDetail) {
        beingReshaped = pathDetail;
        dragStart = null;
        dragEnd = null;
        state.startTemporaryTool(SketchTool.RESHAPE_LINE);
        host.showToast(R.string.sketch_reshape_line_instruction);
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
                float distance = beingReshaped.getDistanceFrom(viewport.toSurvey(touchPoint));
                if (distance > TouchTolerances.getSelectionRadius(viewport)) {
                    finish();
                    return true;
                }
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
                if (dragStart != null && !touchPoint.equals(dragStart)) {
                    Sketch sketch = state.getSketch();
                    sketch.reshapePathDetail(beingReshaped, reshape(dragStart, touchPoint));
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

    /** The line as a drag between two points on the view would leave it. */
    private PathDetail reshape(Coord2D from, Coord2D to) {
        Coord2D grab = viewport.toSurvey(from);
        Coord2D offset = viewport.toSurvey(to).minus(grab);
        float reach =
                Math.max(viewport.dpToSurvey(MINIMUM_REACH_DP), REACH_PER_PULL * offset.mag());
        return beingReshaped.pulled(grab, offset, reach, viewport.dpToSurvey(END_GRAB_DP));
    }

    /** Draw the line as it will be. */
    @Override
    public void drawFeedback(Canvas canvas) {
        if (!isActive()) {
            return;
        }
        PathDetail preview =
                dragStart == null || dragStart.equals(dragEnd)
                        ? beingReshaped
                        : reshape(dragStart, dragEnd);
        sketchRenderer.drawLine(canvas, preview);
    }

    @Override
    public PathDetail getHiddenDetail() {
        return isActive() ? beingReshaped : null;
    }

    /**
     * Whether a line is waiting to be reshaped. It may have gone from the sketch meanwhile (undone,
     * say), or the user may have picked another tool instead.
     */
    private boolean isActive() {
        return beingReshaped != null
                && state.getUnderlyingSketchTool() == SketchTool.RESHAPE_LINE
                && state.getSketch().getPathDetails().contains(beingReshaped);
    }

    private void finish() {
        beingReshaped = null;
        dragStart = null;
        dragEnd = null;
        if (state.getSketchTool() == SketchTool.RESHAPE_LINE) {
            state.endTemporaryTool();
        }
        host.invalidate();
    }
}

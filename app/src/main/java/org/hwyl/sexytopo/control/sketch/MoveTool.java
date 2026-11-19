package org.hwyl.sexytopo.control.sketch;

import android.view.MotionEvent;
import org.hwyl.sexytopo.model.geometry.Coord2D;

/** Pans the view by dragging. */
class MoveTool implements ToolHandler {

    private final Viewport viewport;
    private final ToolHost host;

    // where the drag started, and where the view was then
    private Coord2D actionDownPointOnView = Coord2D.ORIGIN;
    private Coord2D actionDownViewpointOffset = Coord2D.ORIGIN;

    MoveTool(Viewport viewport, ToolHost host) {
        this.viewport = viewport;
        this.host = host;
    }

    @Override
    public boolean onTouch(MotionEvent event) {

        Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                actionDownPointOnView = touchPointOnView;
                actionDownViewpointOffset = viewport.getOffset();
                break;
            case MotionEvent.ACTION_MOVE:
                Coord2D surveyDelta =
                        touchPointOnView
                                .minus(actionDownPointOnView)
                                .scale(1 / viewport.getScale());
                viewport.setOffset(actionDownViewpointOffset.minus(surveyDelta));
                host.invalidate();
            // fall through
            case MotionEvent.ACTION_UP:
                break;
            default:
                return false;
        }

        return true;
    }
}

package org.hwyl.sexytopo.control.sketch;

import android.view.MotionEvent;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.sketch.AreaType;
import org.hwyl.sexytopo.model.sketch.Sketch;

/** Draws the outline of an area of the chosen type, closed when the stroke ends. */
class AreaTool implements ToolHandler {

    private final Viewport viewport;
    private final SketchState state;
    private final ToolHost host;

    private AreaType areaType = AreaType.WATER;

    AreaTool(Viewport viewport, SketchState state, ToolHost host) {
        this.viewport = viewport;
        this.state = state;
        this.host = host;
    }

    void setAreaType(AreaType areaType) {
        this.areaType = areaType;
    }

    @Override
    public boolean onTouch(MotionEvent event) {

        Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        Coord2D surveyCoords = viewport.toSurvey(touchPointOnView);
        Sketch sketch = state.getSketch();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                sketch.startNewArea(surveyCoords, state.getColour(), areaType);
                break;

            case MotionEvent.ACTION_MOVE:
                if (sketch.getActivePath() == null) {
                    // shouldn't be null, but just in case...
                    sketch.startNewArea(surveyCoords, state.getColour(), areaType);
                } else {
                    sketch.getActivePath().lineTo(surveyCoords);
                }
                host.invalidate();
                break;

            case MotionEvent.ACTION_UP:
                sketch.finishArea(areaType, TouchTolerances.getMinimumArea(viewport));
                host.invalidate();
                break;

            default:
                return false;
        }

        return true;
    }
}

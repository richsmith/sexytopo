package org.hwyl.sexytopo.control.sketch;

import android.view.MotionEvent;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hwyl.sexytopo.control.util.LineOrienter;
import org.hwyl.sexytopo.control.util.LoopOrienter;
import org.hwyl.sexytopo.control.util.SketchPreferences;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.geometry.Line;
import org.hwyl.sexytopo.model.geometry.Space;
import org.hwyl.sexytopo.model.sketch.LineType;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;

/**
 * Draws a line of the chosen type. When the stroke ends, an outline whose ends nearly meet is
 * closed, the line is oriented if Auto-Orientation is on, and it's snapped to nearby line ends if
 * Snap to Lines is on.
 */
class LineTool implements ToolHandler {

    private final Viewport viewport;
    private final SketchState state;
    private final ToolHost host;

    private LineType lineType = LineType.SKETCH;

    // where the stroke started, to tell a tap (which leaves a dot) from a drag
    private Coord2D actionDownPointOnView = Coord2D.ORIGIN;

    LineTool(Viewport viewport, SketchState state, ToolHost host) {
        this.viewport = viewport;
        this.state = state;
        this.host = host;
    }

    void setLineType(LineType lineType) {
        this.lineType = lineType;
    }

    @Override
    public boolean onTouch(MotionEvent event) {

        Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        Coord2D surveyCoords = viewport.toSurvey(touchPointOnView);
        Sketch sketch = state.getSketch();

        boolean snapToLines = SketchPreferences.Toggle.SNAP_TO_LINES.isOn();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                actionDownPointOnView = touchPointOnView;
                sketch.startNewPath(surveyCoords, state.getColour(), lineType);
                break;

            case MotionEvent.ACTION_MOVE:
                if (sketch.getActivePath() == null) {
                    // shouldn't be null, but just in case...
                    sketch.startNewPath(surveyCoords, state.getColour(), lineType);
                } else {
                    sketch.getActivePath().lineTo(surveyCoords);
                }
                host.invalidate();
                break;

            case MotionEvent.ACTION_UP:
                boolean isDot = touchPointOnView.equals(actionDownPointOnView);
                if (isDot) {
                    sketch.getActivePath().lineTo(surveyCoords);
                }
                closeActivePathIfOutline();
                orientActivePathIfRequired();
                // snapped once the line's direction is settled, as that decides which ends of
                // other lines it can join
                if (snapToLines && !isDot) {
                    sketch.snapActivePath(TouchTolerances.getSnapDistance(viewport));
                    host.invalidate();
                }
                // A tap with a freehand line leaves a dot, but a typed line that small is a
                // mistake: it would be meaningless in an export
                float minimumSize =
                        lineType.isSemantic() ? TouchTolerances.getMinimumLineSize(viewport) : 0;
                sketch.finishPath(minimumSize);
                break;

            default:
                return false;
        }

        return true;
    }

    /**
     * Joins up an outline (a boulder) whose ends nearly meet, so it's a closed line, as Therion
     * takes a line whose first and last points coincide to be. Ends count as meeting when they're
     * as close as Snap to Lines would join.
     */
    private void closeActivePathIfOutline() {
        PathDetail activePath = state.getSketch().getActivePath();
        if (activePath == null || !activePath.getLineType().isOutline()) {
            return;
        }
        List<Coord2D> points = activePath.getPath();
        if (points.size() < 3) {
            return;
        }
        Coord2D start = points.get(0);
        Coord2D end = points.get(points.size() - 1);
        if (!start.equals(end)
                && Space2DUtils.getDistance(start, end)
                        <= TouchTolerances.getSnapDistance(viewport)) {
            activePath.lineTo(start);
        }
    }

    /**
     * With Auto-Orientation on, sets a finished line's point order so its interior is on a
     * consistent side whichever way the user drew it: the passage side of a wall (see
     * LineOrienter), or the inside of a loop for a ticked edge or outline (see LoopOrienter).
     */
    private void orientActivePathIfRequired() {
        PathDetail activePath = state.getSketch().getActivePath();
        if (activePath == null || !SketchPreferences.Toggle.AUTO_ORIENTATION.isOn()) {
            return;
        }
        LineType activeLineType = activePath.getLineType();
        if (activeLineType.isWall() && state.getProjection() != null) {
            LineOrienter.orientToInterior(activePath.getPath(), getLegLines());
        } else if (activeLineType.isOrientedByLoop()) {
            // a pit or chimney edge drawn round its hole, alone or with a wall, has its ticks
            // inside, and a boulder outline its rock; ends count as meeting when they're as close
            // as Snap to Lines would join
            List<List<Coord2D>> walls =
                    state.getSketch().getPathDetails().stream()
                            .filter(path -> path.getLineType().isWall())
                            .map(PathDetail::getPath)
                            .collect(Collectors.toList());
            LoopOrienter.orientInsideOfLoop(
                    activePath.getPath(),
                    activeLineType.hasLoopInsideOnRight(),
                    walls,
                    TouchTolerances.getSnapDistance(viewport));
        }
    }

    /**
     * The legs as drawn in this view, for orienting walls against. Splays are left out: they run to
     * the walls themselves, and may run on beyond them into a neighbouring passage.
     */
    private List<Line<Coord2D>> getLegLines() {
        Space<Coord2D> projection = state.getProjection();
        if (projection == null) {
            return Collections.emptyList();
        }
        return projection.getLegMap().entrySet().stream()
                .filter(entry -> entry.getKey().hasDestination())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }
}

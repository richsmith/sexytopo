package org.hwyl.sexytopo.control.sketch;

import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.geometry.Space;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.SketchDetail;
import org.hwyl.sexytopo.model.survey.Station;

/** Works out which station or line is under the finger. */
class SketchSelector {

    private final Viewport viewport;
    private final SketchState state;

    SketchSelector(Viewport viewport, SketchState state) {
        this.viewport = viewport;
        this.state = state;
    }

    /** The station nearest a point on the view, or null if none is near enough. */
    Station findStationAt(Coord2D touchPointOnView) {
        float selectionTolerance = TouchTolerances.getSelectionRadius(viewport);
        Coord2D touchPointOnSurvey = viewport.toSurvey(touchPointOnView);
        return findNearestStationWithinDelta(
                state.getProjection(), touchPointOnSurvey, selectionTolerance);
    }

    /**
     * The line the user pressed on, or null. Only the nearest detail counts, so pressing on a
     * symbol that sits on a line picks out the symbol rather than the line.
     */
    PathDetail findPathAt(Coord2D touchPointOnView) {
        float selectionTolerance = TouchTolerances.getSelectionRadius(viewport);
        Coord2D touchPointOnSurvey = viewport.toSurvey(touchPointOnView);
        SketchDetail nearest =
                state.getSketch()
                        .findNearestVisibleDetailWithin(
                                touchPointOnSurvey, selectionTolerance, viewport.getScale());
        return nearest instanceof PathDetail ? (PathDetail) nearest : null;
    }

    private static Station findNearestStationWithinDelta(
            Space<Coord2D> space, Coord2D target, float delta) {

        float shortest = Float.MAX_VALUE;
        Station best = null;

        for (Station station : space.getStationMap().keySet()) {
            Coord2D point = space.getStationMap().get(station);
            //noinspection ConstantConditions
            float distance = Space2DUtils.getDistance(point, target);

            if (distance > delta) {
                continue;
            }

            if (best == null || (distance < shortest)) {
                best = station;
                shortest = distance;
            }
        }

        return best;
    }
}

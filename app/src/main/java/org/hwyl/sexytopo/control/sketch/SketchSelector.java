package org.hwyl.sexytopo.control.sketch;

import java.util.Map;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.geometry.Space;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.SketchDetail;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

/** Works out which station, line or linked survey is under the finger. */
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

    /**
     * The linked survey drawn nearest a point on the view, or null if none is near enough. Its legs
     * and stations count, not its sketch. A linked survey only counts if it's nearer than the
     * survey being shown, so a tap on the station where they join, or where they overlap, is taken
     * as being on the survey being shown.
     */
    Survey findConnectedSurveyAt(
            Coord2D touchPointOnView, Map<Survey, Space<Coord2D>> surveysToProjections) {
        float selectionTolerance = TouchTolerances.getSelectionRadius(viewport);
        Coord2D touchPointOnSurvey = viewport.toSurvey(touchPointOnView);

        float shortest = getDistanceToSurveyData(state.getProjection(), touchPointOnSurvey);
        Survey best = null;

        for (Map.Entry<Survey, Space<Coord2D>> entry : surveysToProjections.entrySet()) {
            float distance = getDistanceToSurveyData(entry.getValue(), touchPointOnSurvey);
            if (distance < shortest) {
                best = entry.getKey();
                shortest = distance;
            }
        }

        return shortest <= selectionTolerance ? best : null;
    }

    /** How far a point is from the nearest leg or station drawn, or infinity if none are. */
    private static float getDistanceToSurveyData(Space<Coord2D> space, Coord2D point) {
        double toStations =
                space.getStationMap().values().stream()
                        .mapToDouble(station -> Space2DUtils.getDistance(station, point))
                        .min()
                        .orElse(Double.POSITIVE_INFINITY);
        double toLegs =
                space.getLegMap().values().stream()
                        .mapToDouble(
                                line ->
                                        Space2DUtils.getDistanceFromLine(
                                                point, line.getStart(), line.getEnd()))
                        .min()
                        .orElse(Double.POSITIVE_INFINITY);
        return (float) Math.min(toStations, toLegs);
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

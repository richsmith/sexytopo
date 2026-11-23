package org.hwyl.sexytopo.control.sketch;

import android.view.MotionEvent;
import java.util.List;
import java.util.stream.Collectors;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.PolygonUtils;
import org.hwyl.sexytopo.control.util.SketchPreferences;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.sketch.AreaDetail;
import org.hwyl.sexytopo.model.sketch.CrossSectionDetail;
import org.hwyl.sexytopo.model.sketch.GeometryType;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.SketchDetail;

/**
 * Erases what the finger touches or passes over. Everything erased in one drag is undone together.
 * With "delete line fragments" on, lines and areas are cut into rather than removed whole.
 *
 * <p>Each drag only erases one type of detail: areas, lines or points.
 */
class EraseTool implements ToolHandler {

    private final Viewport viewport;
    private final SketchState state;
    private final CrossSectionLayer crossSections;
    private final ToolHost host;

    // where the eraser last was during a drag, or null if it isn't being dragged
    private Coord2D lastErasePointOnSurvey;

    // the type of detail this drag is erasing, or null until it has hit something
    private GeometryType typeBeingErased;

    EraseTool(
            Viewport viewport, SketchState state, CrossSectionLayer crossSections, ToolHost host) {
        this.viewport = viewport;
        this.state = state;
        this.crossSections = crossSections;
        this.host = host;
    }

    @Override
    public boolean onTouch(MotionEvent event) {

        Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        Coord2D touchPointOnSurvey = viewport.toSurvey(touchPointOnView);

        boolean deleteLineFragments = GeneralPreferences.isDeletePathFragmentsModeOn();

        float deleteToleranceInMetres = TouchTolerances.getEraseRadius(viewport);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // everything erased in one drag is undone together
                state.getSketch().startGroupingDeletions();
                lastErasePointOnSurvey = touchPointOnSurvey;
                typeBeingErased = null;
                SketchDetail tapped = findDetailTapped(touchPointOnSurvey, deleteToleranceInMetres);
                if (tapped != null) {
                    typeBeingErased = tapped.getGeometryType();
                    eraseDetail(
                            tapped,
                            touchPointOnSurvey,
                            deleteToleranceInMetres,
                            deleteLineFragments);
                    host.invalidate();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (lastErasePointOnSurvey == null) {
                    break;
                }
                eraseAlong(
                        lastErasePointOnSurvey,
                        touchPointOnSurvey,
                        deleteToleranceInMetres,
                        deleteLineFragments);
                lastErasePointOnSurvey = touchPointOnSurvey;
                host.invalidate();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                cancel();
                break;

            default:
                return false;
        }

        return true;
    }

    /**
     * End any erasing drag, so the next one starts its own undo step and doesn't erase a line from
     * where this one left off.
     */
    void cancel() {
        state.getSketch().endGroupingDeletions();
        lastErasePointOnSurvey = null;
        typeBeingErased = null;
    }

    /**
     * Erase whatever the finger passed over between two touch events. Unlike a tap, this leaves
     * cross-sections alone, as they're too easy to hit by accident.
     */
    private void eraseAlong(
            Coord2D from, Coord2D to, float toleranceInMetres, boolean deleteLineFragments) {
        Sketch sketch = state.getSketch();
        // step along the gap, as on a quick drag the touch events can be far enough apart to jump
        // over a line
        float distance = to.minus(from).mag();
        int steps = Math.max(1, (int) Math.ceil(distance / (toleranceInMetres / 2)));
        for (int i = 1; i <= steps; i++) {
            Coord2D point = from.plus(to.minus(from).scale((float) i / steps));
            SketchDetail detail =
                    sketch.findNearestVisibleDetailWithin(
                            point,
                            toleranceInMetres,
                            viewport.getScale(),
                            this::isErasableInThisDrag);
            // crossing the middle of an area only cuts through it; it would be too easy to delete
            // a whole one by accident
            if (detail == null
                    && deleteLineFragments
                    && (typeBeingErased == null || typeBeingErased == GeometryType.AREA)) {
                detail = sketch.findAreaContaining(point);
            }
            if (detail != null) {
                typeBeingErased = detail.getGeometryType();
                eraseDetail(detail, point, toleranceInMetres, deleteLineFragments);
            }
        }
    }

    /** Whether a drag may erase this detail, given what it has already been erasing. */
    private boolean isErasableInThisDrag(SketchDetail detail) {
        if (detail instanceof CrossSectionDetail) {
            return false;
        }
        return typeBeingErased == null || typeBeingErased == detail.getGeometryType();
    }

    /**
     * Work out which sketch detail the user has just tapped, or null if they missed everything.
     * This only decides what was hit; what to do about it is up to the caller.
     *
     * <p>Details are tried in order of how specifically the tap identifies them. Whatever lies
     * nearest the tap wins first, so a line drawn across an area is still picked out when the user
     * aims at the line. Cross-sections come next: a press anywhere on the body counts, which is a
     * bigger target than a point and so shouldn't pre-empt something the user aimed at precisely.
     * Last comes an area under the finger, which covers tapping the middle of one — too far from
     * its boundary for the distance test to reach, and otherwise unhittable.
     */
    private SketchDetail findDetailTapped(Coord2D touchPointOnSurvey, float toleranceInMetres) {

        SketchDetail nearest =
                state.getSketch()
                        .findNearestVisibleDetailWithin(
                                touchPointOnSurvey, toleranceInMetres, viewport.getScale());
        if (nearest != null) {
            return nearest;
        }

        if (SketchPreferences.Toggle.SHOW_X_SECTIONS.isOn()) {
            CrossSectionDetail crossSection = crossSections.findBodyAt(touchPointOnSurvey);
            if (crossSection != null) {
                return crossSection;
            }
        }

        return state.getSketch().findAreaContaining(touchPointOnSurvey);
    }

    /**
     * Delete a tapped detail. With "delete line fragments" on, lines and areas are cut into rather
     * than removed whole; everything else is deleted outright.
     */
    private void eraseDetail(
            SketchDetail detail,
            Coord2D touchPointOnSurvey,
            float toleranceInMetres,
            boolean deleteLineFragments) {

        if (deleteLineFragments && detail instanceof PathDetail) {
            // you got part of the line; as when drawing, a typed line too small to see properly
            // is a speck, so a stub left at the edge of the eraser is dropped
            PathDetail pathDetail = (PathDetail) detail;
            float minimumSize =
                    pathDetail.getLineType().isSemantic()
                            ? TouchTolerances.getMinimumLineSize(viewport)
                            : 0;
            List<SketchDetail> fragments =
                    pathDetail
                            .getPathFragmentsOutsideRadius(touchPointOnSurvey, toleranceInMetres)
                            .stream()
                            .filter(fragment -> fragment.getMaxDimension() >= minimumSize)
                            .collect(Collectors.toList());
            state.getSketch().deleteDetail(detail, fragments);

        } else if (deleteLineFragments && detail instanceof AreaDetail) {
            // carve a notch out of the area; this can split it into several areas, or punch a
            // hole if the user is erasing in the middle of one
            eraseFromArea((AreaDetail) detail, touchPointOnSurvey, toleranceInMetres);

        } else {
            // bullseye!
            state.getSketch().deleteDetail(detail);
        }
    }

    /**
     * Erase a disc from an area as an undoable operation. Whatever survives comes back as
     * replacement areas: possibly one shrunken region, possibly one with a new hole if the user
     * erased into the middle of it, possibly several if the notch split it, possibly none if the
     * disc swallowed it. Falls back to deleting the whole area if the geometry op fails.
     */
    private void eraseFromArea(AreaDetail areaDetail, Coord2D touchPointOnSurvey, float radius) {
        List<PolygonUtils.Region> remaining =
                PolygonUtils.subtract(areaDetail.toRegion(), touchPointOnSurvey, radius);
        if (remaining == null) {
            state.getSketch().deleteDetail(areaDetail);
            return;
        }
        // a nibble can leave slivers too small to be worth keeping
        double minimumArea = TouchTolerances.getMinimumArea(viewport);
        List<SketchDetail> replacements =
                remaining.stream()
                        .filter(region -> region.getArea() >= minimumArea)
                        .map(region -> (SketchDetail) areaDetail.withRegion(region))
                        .collect(Collectors.toList());
        state.getSketch().deleteDetail(areaDetail, replacements);
    }
}

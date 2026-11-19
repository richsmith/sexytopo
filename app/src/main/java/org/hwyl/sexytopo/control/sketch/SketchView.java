package org.hwyl.sexytopo.control.sketch;

import static org.hwyl.sexytopo.control.sketch.SketchPaints.FADED_ALPHA;
import static org.hwyl.sexytopo.control.sketch.SketchPaints.SOLID_ALPHA;
import static org.hwyl.sexytopo.control.sketch.SketchPaints.dpToPixels;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RotateDrawable;
import android.os.Build;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SubMenu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SexyTopo;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.activity.SketchActivity;
import org.hwyl.sexytopo.control.components.DialogUtils;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.LineOrienter;
import org.hwyl.sexytopo.control.util.LoopOrienter;
import org.hwyl.sexytopo.control.util.PolygonUtils;
import org.hwyl.sexytopo.control.util.SketchPreferences;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.geometry.Line;
import org.hwyl.sexytopo.model.geometry.Projection2D;
import org.hwyl.sexytopo.model.geometry.Space;
import org.hwyl.sexytopo.model.sketch.AreaDetail;
import org.hwyl.sexytopo.model.sketch.AreaType;
import org.hwyl.sexytopo.model.sketch.BrushColour;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.sketch.CrossSectionDetail;
import org.hwyl.sexytopo.model.sketch.LineType;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.SketchDetail;
import org.hwyl.sexytopo.model.sketch.SketchTool;
import org.hwyl.sexytopo.model.sketch.Symbol;
import org.hwyl.sexytopo.model.sketch.SymbolDetail;
import org.hwyl.sexytopo.model.sketch.TextDetail;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.SurveyConnection;

@SuppressWarnings({"SameParameterValue", "UnnecessaryLocalVariable"})
public class SketchView extends View {

    private final ScaleGestureDetector scaleGestureDetector;
    private final GestureDetector longPressDetector;

    // Which part of the survey is on screen, and at what zoom
    protected final Viewport viewport = new Viewport();

    // What's being shown, and which tool is in use
    protected final SketchState state = new SketchState();

    // These variables are used in handling the dragging of the viewing window
    private Coord2D actionDownPointOnView = Coord2D.ORIGIN;
    // where the eraser last was during a drag, or null if it isn't being dragged
    private Coord2D lastErasePointOnSurvey;
    private Coord2D actionDownViewpointOffset = Coord2D.ORIGIN;

    private static final int BOX_SIZE = 10; // every grid box is 10 units square

    public static final int LEGEND_SIZE = 18;

    private static final float DELETE_DETAILS_WITHIN_N_DP = 10.0f;
    // typed lines spanning less than this on screen, and areas smaller than a square this size,
    // are discarded as specks
    private static final float MINIMUM_DETAIL_SIZE_DP = 10.0f;
    private static final float SELECTION_SENSITIVITY_DP = 25.0f;
    private static final float SNAP_TO_LINE_SENSITIVITY_DP = 10.0f;
    public static final float HOT_CORNER_DISTANCE_PROPORTION = 0.05f;

    private float deleteDetailsWithinPx;
    private float selectionSensitivityPx;
    private float snapToLineSensitivityPx;

    private SketchActivity activity;

    private Map<Survey, Space<Coord2D>> translatedConnectedSurveys = new HashMap<>();

    boolean surveyChanged;

    // cached preferences for performance
    private boolean isDarkModeActive = false;
    private boolean isTwoFingerModeActive = true;
    private boolean isHotCornersModeActive = true;

    // cached for performance
    private float surveyLength = 0;
    private float surveyHeight = 0;
    private Rect topLeftCorner;
    private Rect topRightCorner;
    private Rect bottomRightCorner;

    private float compassAzimuthDegrees = 0f;

    // Flag to prevent double menu opening during this touch sequence
    private boolean menuShownInThisTouch = false;

    private Symbol currentSymbol = Symbol.getDefault();

    private AreaType currentAreaType = AreaType.WATER;

    private Colour currentColour = Colour.BLACK;

    private LineType currentLineType = LineType.SKETCH;

    // ********** Drawing **********

    private final SketchPaints paints = new SketchPaints();

    private final CrossSectionLayer crossSections =
            new CrossSectionLayer(viewport, state, paints, new CrossSectionHost());

    // scratch object reused when drawing typed paths, to avoid per-frame allocation
    private final Path typedPathScratch = new Path();
    // scratch object reused to position the fill pattern, to avoid per-frame allocation
    private final Matrix areaFillMatrix = new Matrix();

    public SketchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        longPressDetector = new GestureDetector(context, new LongPressListener());

        // Line decoration and the shading on walls are drawn with path effects, which a
        // hardware-accelerated canvas only renders from Android 9 (API 28). Before that, draw in
        // software so they still show; it's slower, but a plain freehand sketch stays quick.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }
    }

    public void initialisePaint() {
        paints.initialise(getContext());
        crossSections.initialisePaints(getContext());

        isTwoFingerModeActive = GeneralPreferences.isTwoFingerModeActive();
        isHotCornersModeActive = GeneralPreferences.isHotCornersModeActive();

        deleteDetailsWithinPx = dpToPixels(DELETE_DETAILS_WITHIN_N_DP);
        selectionSensitivityPx = dpToPixels(SELECTION_SENSITIVITY_DP);
        snapToLineSensitivityPx = dpToPixels(SNAP_TO_LINE_SENSITIVITY_DP);
    }

    public void setActivity(SketchActivity sketchActivity) {
        this.activity = sketchActivity;
    }

    public void setSurvey(Survey survey) {
        if (survey != state.getSurvey()) {
            state.setSurvey(survey);
            surveyChanged = true;
        }
    }

    public void checkForChangedSurvey() {
        if (surveyChanged) {
            centreViewOnActiveStation();
            surveyChanged = false;
        }
    }

    public void setProjectionType(Projection2D projectionType) {
        state.setProjectionType(projectionType);
    }

    public void setProjection(Space<Coord2D> projection) {
        state.setProjection(projection);
    }

    public void setSketch(Sketch sketch) {
        state.setSketch(sketch);
    }

    public void setIsDarkModeActive(boolean isDarkModeActive) {
        this.isDarkModeActive = isDarkModeActive;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // Reset per-touch flags at start of new touch sequence
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            menuShownInThisTouch = false;
            crossSections.onTouchStart();
        }

        scaleGestureDetector.onTouchEvent(event);
        longPressDetector.onTouchEvent(event);

        if (scaleGestureDetector.isInProgress()) {
            // a pinch interrupts an erasing drag: don't erase a line from where it left off
            lastErasePointOnSurvey = null;
            state.getSketch().endGroupingDeletions();
            return true;
        }

        if (state.getSketchTool().isModal()
                && state.getSketchTool() != SketchTool.MOVE_CROSS_SECTION
                && event.getAction() == MotionEvent.ACTION_UP) {
            if (state.getPreviousSketchTool() != state.getSketchTool()) {
                state.revertToPreviousTool();
            } else {
                setSketchTool(SketchTool.MOVE);
            }
            invalidate();
            return true;
        }

        if (isModalMoveSelection(event)) {
            setSketchTool(SketchTool.MODAL_MOVE);
            // handled below
        }

        if (crossSections.isHandlePress(event)) {
            setSketchTool(SketchTool.MOVE_CROSS_SECTION);
        }

        if (crossSections.handleBodyTap(event)) {
            return true;
        }

        // Once a long-press has opened the station context menu, the rest of this touch is
        // consumed: it shouldn't also act as a sketch gesture (e.g. leaving a stray mark).
        if (menuShownInThisTouch) {
            return true;
        }

        switch (state.getSketchTool()) {
            case MOVE:
                return handleMove(event);
            case MODAL_MOVE:
                return handleMove(event);
            case LINE:
                return handleLine(event);
            case AREA:
                return handleDrawArea(event);
            case ERASE:
                return handleErase(event);
            case SYMBOL:
                return handleSymbol(event);
            case TEXT:
                return handleText(event);
            case SELECT:
                return handleSelect(event);
            case POSITION_CROSS_SECTION:
                return crossSections.positionTool.onTouch(event);
            case ROTATE_CROSS_SECTION:
                return crossSections.rotateTool.onTouch(event);
            case MOVE_CROSS_SECTION:
                return crossSections.moveTool.onTouch(event);
        }
        return false;
    }

    private boolean isModalMoveSelection(MotionEvent event) {

        if (state.getSketchTool() == SketchTool.MODAL_MOVE) {
            return false;
        }

        if (isTwoFingerModeActive && event.getPointerCount() >= 2) {
            return true;
        }

        if (isHotCornersModeActive && didEventHitHotCorner(event)) {
            return true;
        }

        return false;
    }

    private boolean didEventHitHotCorner(MotionEvent event) {
        if (!isHotCornersModeActive) {
            return false;
        }

        float x = event.getX();
        float y = event.getY();
        int height = getHeight();
        int width = getWidth();

        float corner_delta = Math.min(height, width) * HOT_CORNER_DISTANCE_PROPORTION;

        boolean hitLeftEdge = x < corner_delta;
        boolean hitRightEdge = x > width - corner_delta;
        boolean hitTopEdge = y < corner_delta;
        boolean hitBottomEdge = y > height - corner_delta;

        boolean hitCorner =
                (hitLeftEdge && (hitBottomEdge || hitTopEdge))
                        || (hitRightEdge && (hitBottomEdge || hitTopEdge));

        return hitCorner;
    }

    private boolean handleLine(MotionEvent event) {

        Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        Coord2D surveyCoords = viewport.toSurvey(touchPointOnView);

        boolean snapToLines = SketchPreferences.Toggle.SNAP_TO_LINES.isOn();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                actionDownPointOnView = touchPointOnView;
                state.getSketch().startNewPath(surveyCoords, currentColour, currentLineType);
                break;

            case MotionEvent.ACTION_MOVE:
                if (state.getSketch().getActivePath() == null) {
                    // shouldn't be null, but just in case...
                    state.getSketch().startNewPath(surveyCoords, currentColour, currentLineType);
                } else {
                    state.getSketch().getActivePath().lineTo(surveyCoords);
                }
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                boolean isDot = touchPointOnView.equals(actionDownPointOnView);
                if (isDot) {
                    state.getSketch().getActivePath().lineTo(surveyCoords);
                }
                closeActivePathIfOutline();
                orientActivePathIfRequired();
                // snapped once the line's direction is settled, as that decides which ends of
                // other lines it can join
                if (snapToLines && !isDot) {
                    state.getSketch().snapActivePath(getSnapDistance());
                    invalidate();
                }
                // A tap with a freehand line leaves a dot, but a typed line that small is a
                // mistake: it would be meaningless in an export
                float minimumSize = currentLineType.isSemantic() ? getMinimumLineSize() : 0;
                state.getSketch().finishPath(minimumSize);
                break;

            default:
                return false;
        }

        return true;
    }

    private boolean handleDrawArea(MotionEvent event) {

        Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        Coord2D surveyCoords = viewport.toSurvey(touchPointOnView);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                actionDownPointOnView = touchPointOnView;
                state.getSketch().startNewArea(surveyCoords, currentColour, currentAreaType);
                break;

            case MotionEvent.ACTION_MOVE:
                if (state.getSketch().getActivePath() == null) {
                    // shouldn't be null, but just in case...
                    state.getSketch().startNewArea(surveyCoords, currentColour, currentAreaType);
                } else {
                    state.getSketch().getActivePath().lineTo(surveyCoords);
                }
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                state.getSketch().finishArea(currentAreaType, getMinimumArea());
                invalidate();
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
        if (!start.equals(end) && Space2DUtils.getDistance(start, end) <= getSnapDistance()) {
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
        LineType lineType = activePath.getLineType();
        if (lineType.isWall() && state.getProjection() != null) {
            LineOrienter.orientToInterior(activePath.getPath(), getLegLines());
        } else if (lineType.isOrientedByLoop()) {
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
                    lineType.hasLoopInsideOnRight(),
                    walls,
                    getSnapDistance());
        }
    }

    private float getSnapDistance() {
        return snapToLineSensitivityPx / viewport.getScale();
    }

    private boolean handleMove(MotionEvent event) {

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
                invalidate();
            // fall through
            case MotionEvent.ACTION_UP:
                break;
            default:
                return false;
        }

        return true;
    }

    private boolean handleErase(MotionEvent event) {

        Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        Coord2D touchPointOnSurvey = viewport.toSurvey(touchPointOnView);

        boolean deleteLineFragments = GeneralPreferences.isDeletePathFragmentsModeOn();

        float deleteToleranceInMetres = deleteDetailsWithinPx / viewport.getScale();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // everything erased in one drag is undone together
                state.getSketch().startGroupingDeletions();
                lastErasePointOnSurvey = touchPointOnSurvey;
                SketchDetail tapped = findDetailTapped(touchPointOnSurvey, deleteToleranceInMetres);
                if (tapped != null) {
                    eraseDetail(
                            tapped,
                            touchPointOnSurvey,
                            deleteToleranceInMetres,
                            deleteLineFragments);
                    invalidate();
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
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                state.getSketch().endGroupingDeletions();
                lastErasePointOnSurvey = null;
                break;

            default:
                return false;
        }

        return true;
    }

    /**
     * Erase whatever the finger passed over between two touch events. Unlike a tap, this leaves
     * cross-sections alone, as they're too easy to hit by accident.
     */
    private void eraseAlong(
            Coord2D from, Coord2D to, float toleranceInMetres, boolean deleteLineFragments) {
        // step along the gap, as on a quick drag the touch events can be far enough apart to jump
        // over a line
        float distance = to.minus(from).mag();
        int steps = Math.max(1, (int) Math.ceil(distance / (toleranceInMetres / 2)));
        for (int i = 1; i <= steps; i++) {
            Coord2D point = from.plus(to.minus(from).scale((float) i / steps));
            SketchDetail detail =
                    state.getSketch()
                            .findNearestVisibleDetailWithin(
                                    point, toleranceInMetres, viewport.getScale());
            if (detail instanceof CrossSectionDetail) {
                continue;
            }
            // crossing the middle of an area only cuts through it; it would be too easy to delete
            // a whole one by accident
            if (detail == null && deleteLineFragments) {
                detail = state.getSketch().findAreaContaining(point);
            }
            if (detail != null) {
                eraseDetail(detail, point, toleranceInMetres, deleteLineFragments);
            }
        }
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
            float minimumSize = pathDetail.getLineType().isSemantic() ? getMinimumLineSize() : 0;
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
        double minimumArea = getMinimumArea();
        List<SketchDetail> replacements =
                remaining.stream()
                        .filter(region -> region.getArea() >= minimumArea)
                        .map(region -> (SketchDetail) areaDetail.withRegion(region))
                        .collect(Collectors.toList());
        state.getSketch().deleteDetail(areaDetail, replacements);
    }

    /**
     * The smallest area (in square metres) worth keeping: a square of fixed on-screen size,
     * converted at the current zoom. So the threshold follows what the user can actually see,
     * rather than being a fixed size in the cave.
     */
    private double getMinimumArea() {
        double sizeInMetres = getMinimumLineSize();
        return sizeInMetres * sizeInMetres;
    }

    /**
     * The smallest extent (in metres) a typed line must span to be kept, so a stray tap doesn't
     * leave a speck. Like getMinimumArea, it's a fixed on-screen size converted at the current
     * zoom.
     */
    private float getMinimumLineSize() {
        return dpToPixels(MINIMUM_DETAIL_SIZE_DP) / viewport.getScale();
    }

    private boolean handleSymbol(MotionEvent event) {

        if (currentSymbol == Symbol.TEXT) {
            return handleText(event);
        }

        final Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        final Coord2D touchPointOnSurvey = viewport.toSurvey(touchPointOnView);
        float startingSizeDp = GeneralPreferences.getSymbolStartingSizeDp();
        float startingSizePixels = SexyTopo.dpToPixels(startingSizeDp);
        float size = startingSizePixels / viewport.getScale();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Only record the start point; the symbol is placed on ACTION_UP so a long-press
                // that opens a station menu doesn't also leave a symbol behind.
                actionDownPointOnView = touchPointOnView;
                return true;

            case MotionEvent.ACTION_UP:
                if (currentSymbol.isDirectional()) {
                    float angle =
                            Space2DUtils.getAngleBetween(actionDownPointOnView, touchPointOnView);
                    angle = Space2DUtils.adjustAngle(angle, -90);
                    Coord2D firstTouch = viewport.toSurvey(actionDownPointOnView);
                    state.getSketch()
                            .addSymbolDetail(firstTouch, currentSymbol, currentColour, size, angle);
                    float distance =
                            Space2DUtils.getDistance(actionDownPointOnView, touchPointOnView);
                    if (distance < 5) {
                        activity.showSimpleToast(R.string.sketch_symbol_orientation_education);
                    }
                } else {
                    state.getSketch()
                            .addSymbolDetail(
                                    touchPointOnSurvey, currentSymbol, currentColour, size, 0);
                }
                invalidate();
                return true;

            default:
                return false;
        }
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    private boolean handleText(MotionEvent event) {

        final Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        final Coord2D touchPointOnSurvey = viewport.toSurvey(touchPointOnView);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                TextInputLayout inputLayout =
                        DialogUtils.createStandardTextInputLayout(
                                getContext(), R.string.sketch_text_hint);

                TextInputEditText input = DialogUtils.getEditText(inputLayout);

                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
                builder.setView(inputLayout)
                        .setPositiveButton(
                                R.string.ok,
                                (dialog, which) -> {
                                    Editable editable = input.getText();
                                    if (editable == null || editable.length() == 0) {
                                        return;
                                    }
                                    String text = editable.toString();
                                    int startingSizeSp = GeneralPreferences.getTextStartingSizeSp();
                                    float startingSizePixels =
                                            SketchPaints.spToPixels(getContext(), startingSizeSp);
                                    float size = startingSizePixels / viewport.getScale();
                                    state.getSketch()
                                            .addTextDetail(
                                                    touchPointOnSurvey, text, currentColour, size);
                                    invalidate();
                                })
                        .setNegativeButton(R.string.cancel, null);

                android.app.Dialog dialog = builder.create();

                // Automatically select text field
                dialog.setOnShowListener(
                        dialogInterface -> {
                            input.requestFocus();
                            InputMethodManager imm =
                                    (InputMethodManager)
                                            getContext()
                                                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
                        });

                DialogUtils.showKeyboardOnDisplay(dialog);
                dialog.show();
                return true;

            default:
                return false;
        }
    }

    private boolean handleSelect(MotionEvent event) {

        Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Station newSelectedStation = checkForStation(touchPointOnView);

                if (newSelectedStation == null) {
                    return false;

                } else if (newSelectedStation != state.getSurvey().getActiveStation()) {
                    state.getSurvey().setActiveStation(newSelectedStation);
                    broadcastSurveyUpdated();
                    invalidate();
                    return true;

                } else { // double selection opens context menu
                    if (!menuShownInThisTouch) {
                        showContextMenu(event, newSelectedStation);
                        menuShownInThisTouch = true;
                    }
                    return true;
                }

            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                break;
            default:
                return false;
        }

        return true;
    }

    private Station checkForStation(Coord2D touchPointOnView) {
        float selectionTolerance = selectionSensitivityPx / viewport.getScale();
        Coord2D touchPointOnSurvey = viewport.toSurvey(touchPointOnView);

        Station matchedStation =
                findNearestStationWithinDelta(
                        state.getProjection(), touchPointOnSurvey, selectionTolerance);
        return matchedStation; // this could be null if nothing is near
    }

    private void showContextMenu(MotionEvent event, final Station station) {
        // Determine view context based on projection type
        ViewContext viewContext = getViewContext();
        if (!viewContext.hasStationContextMenu()) {
            return;
        }

        // Use activity as listener (it implements ContextMenuManager.StationMenuListener)
        ContextMenuManager menuManager =
                new ContextMenuManager(getContext(), viewContext, activity);
        menuManager.showMenuForStation(
                this, station, state.getSurvey(), (int) event.getX(), (int) event.getY());
    }

    /**
     * The line the user pressed on, or null. Only the nearest detail counts, so pressing on a
     * symbol that sits on a line picks out the symbol rather than the line.
     */
    private PathDetail checkForPath(Coord2D touchPointOnView) {
        float selectionTolerance = selectionSensitivityPx / viewport.getScale();
        Coord2D touchPointOnSurvey = viewport.toSurvey(touchPointOnView);
        SketchDetail nearest =
                state.getSketch()
                        .findNearestVisibleDetailWithin(
                                touchPointOnSurvey, selectionTolerance, viewport.getScale());
        return nearest instanceof PathDetail ? (PathDetail) nearest : null;
    }

    /** Offers to flip a line or change its type. Both are single undoable steps. */
    private void showLineContextMenu(MotionEvent event, final PathDetail pathDetail) {
        PopupMenu popup =
                ContextMenuManager.createPopupMenuAt(
                        getContext(), this, (int) event.getX(), (int) event.getY());
        popup.inflate(R.menu.context_line);
        Menu menu = popup.getMenu();

        LineType currentType = pathDetail.getLineType();
        menu.findItem(R.id.line_title).setTitle(currentType.getStringId());
        // Orientation means nothing for a plain sketch line or a rock edge
        menu.findItem(R.id.action_flip_line).setEnabled(currentType.isOriented());

        Map<Integer, LineType> lineTypesByItemId = new HashMap<>();
        SubMenu lineTypeMenu = menu.findItem(R.id.menu_line_type).getSubMenu();
        for (LineType lineType : LineType.values()) {
            int itemId = lineType.getButtonViewId();
            lineTypeMenu
                    .add(R.id.menu_line_type, itemId, Menu.NONE, lineType.getStringId())
                    .setChecked(lineType == currentType);
            lineTypesByItemId.put(itemId, lineType);
        }
        lineTypeMenu.setGroupCheckable(R.id.menu_line_type, true, true);

        popup.setOnMenuItemClickListener(
                item -> {
                    if (item.getItemId() == R.id.action_flip_line) {
                        state.getSketch().flipPathDetail(pathDetail);
                        invalidate();
                        return true;
                    }
                    LineType lineType = lineTypesByItemId.get(item.getItemId());
                    if (lineType != null && lineType != currentType) {
                        retypeLine(pathDetail, lineType);
                        return true;
                    }
                    return false;
                });
        popup.show();
    }

    private void retypeLine(PathDetail pathDetail, LineType lineType) {
        state.getSketch().retypePathDetail(pathDetail, lineType);
        invalidate();
    }

    /**
     * The legs as drawn in this view, for orienting walls against. Splays are left out: they run to
     * the walls themselves, and may run on beyond them into a neighbouring passage.
     */
    private List<Line<Coord2D>> getLegLines() {
        if (state.getProjection() == null) {
            return Collections.emptyList();
        }
        return state.getProjection().getLegMap().entrySet().stream()
                .filter(entry -> entry.getKey().hasDestination())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    /**
     * @noinspection DuplicateBranchesInSwitch
     */
    protected ViewContext getViewContext() {
        if (state.getProjectionType() == null) {
            return ViewContext.PLAN;
        }
        switch (state.getProjectionType()) {
            case PLAN:
                return ViewContext.PLAN;
            case EXTENDED_ELEVATION:
                return ViewContext.EXTENDED_ELEVATION;
            case ELEVATION_NS:
            case ELEVATION_EW:
                return ViewContext.ELEVATION;
            case CROSS_SECTION:
                return ViewContext.CROSS_SECTION;
            default:
                return ViewContext.PLAN;
        }
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

    public void handleNewCrossSection(Station station) {
        handleNewCrossSection(station, CrossSection.Orientation.VERTICAL);
    }

    public void handleNewCrossSection(Station station, CrossSection.Orientation orientation) {
        crossSections.startPositioning(station, orientation);
    }

    public void handleRotateCrossSection(Station station) {
        crossSections.startRotating(station);
    }

    public void launchCrossSectionEditor(CrossSectionDetail detail) {
        crossSections.launchEditor(detail);
    }

    private void broadcastSurveyUpdated() {
        SurveyManager.getInstance(getContext().getApplicationContext()).broadcastSurveyUpdated();
    }

    protected void updateViewBounds() {
        viewport.updateBounds(getWidth(), getHeight());
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        updateViewBounds();

        drawGrid(canvas);
        drawConnectedSurveys(canvas, state.getProjection(), FADED_ALPHA);
        drawSurvey(canvas, state.getSurvey(), state.getProjection(), SOLID_ALPHA);
        drawLegend(canvas);
        drawCompass(canvas);
        drawHotCorners(canvas);
        drawDebuggingInfo(canvas);
    }

    protected void drawSurvey(Canvas canvas, Survey survey, Space<Coord2D> projection, int alpha) {
        drawSketch(canvas, activity.getSketch(survey), alpha);
        if (survey == state.getSurvey()) {
            // Linked surveys are drawn without their cross-sections, to cut down on clutter
            crossSections.draw(canvas, state.getSketch().getCrossSectionDetails(), alpha);
        }
        drawSurveyData(survey, canvas, projection, alpha);
    }

    private void drawConnectedSurveys(Canvas canvas, Space<Coord2D> projection, int alpha) {

        if (!SketchPreferences.Toggle.SHOW_CONNECTIONS.isOn()) {
            return;
        }

        if (doTranslatedConnectedSurveysNeedUpdating()) {
            try {
                this.translatedConnectedSurveys =
                        ConnectedSurveys.getTranslatedConnectedSurveys(
                                activity.getProjectionType(), state.getSurvey(), projection);
            } catch (Exception exception) {
                Log.e("Error getting translated connected surveys");
                Log.e(exception);
                return;
            }
        }

        for (Survey translatedConnectedSurvey : translatedConnectedSurveys.keySet()) {
            try {
                Space<Coord2D> connectedProjection =
                        translatedConnectedSurveys.get(translatedConnectedSurvey);
                drawSurvey(canvas, translatedConnectedSurvey, connectedProjection, alpha);
            } catch (Exception exception) {
                String name = translatedConnectedSurvey.getName();
                Log.e("Error drawing connected survey " + name);
                Log.e(exception);
                Log.e("Sorry, having to unlink connected survey " + name);
                translatedConnectedSurveys.remove(translatedConnectedSurvey);
            }
        }
    }

    private boolean doTranslatedConnectedSurveysNeedUpdating() {
        Set<Survey> flatSetOfConnectedSurveys = getFlatSetOfConnectedSurveys();
        Set<Survey> flatSetOfTranslatedConnectedSurveys = translatedConnectedSurveys.keySet();
        return !flatSetOfConnectedSurveys.equals(flatSetOfTranslatedConnectedSurveys);
    }

    private Set<Survey> getFlatSetOfConnectedSurveys() {
        Set<Survey> flatSet = new HashSet<>();
        for (Set<SurveyConnection> connectionSet :
                state.getSurvey().getConnectedSurveys().values()) {
            for (SurveyConnection connection : connectionSet) {
                flatSet.add(connection.otherSurvey);
            }
        }
        return flatSet;
    }

    protected void drawGrid(Canvas canvas) {
        float viewOffsetX = viewport.getOffsetX();
        float viewOffsetY = viewport.getOffsetY();
        float viewScale = viewport.getScale();

        if (!SketchPreferences.Toggle.SHOW_GRID.isOn()) {
            return;
        }

        int tickSizeInMetres = getMinorGridBoxSize();
        int numberTicksJustBeforeViewpointOffsetX = (int) (viewOffsetX / tickSizeInMetres);

        for (int n = numberTicksJustBeforeViewpointOffsetX; true; n++) {
            float xSurvey = n * tickSizeInMetres;
            int xView = (int) ((xSurvey - viewOffsetX) * viewScale);
            paints.gridPaint.setStrokeWidth(n % BOX_SIZE == 0 ? 3 : 1);
            canvas.drawLine(xView, 0, xView, getHeight(), paints.gridPaint);
            if (xView >= getWidth()) {
                break;
            }
        }

        int numberTicksJustBeforeViewpointOffsetY = (int) (viewOffsetY / tickSizeInMetres);

        for (int n = numberTicksJustBeforeViewpointOffsetY; true; n++) {
            float ySurvey = n * tickSizeInMetres;
            int yView = (int) ((ySurvey - viewOffsetY) * viewScale);
            paints.gridPaint.setStrokeWidth(n % BOX_SIZE == 0 ? 3 : 1);
            canvas.drawLine(0, yView, getWidth(), yView, paints.gridPaint);
            if (yView >= getHeight()) {
                break;
            }
        }
    }

    public int getMinorGridBoxSize() {

        if (viewport.getScale() > 15) {
            return 1;
        } else if (viewport.getScale() > 2) {
            return 10;
        } else {
            return 100;
        }
    }

    private void drawSurveyData(Survey survey, Canvas canvas, Space<Coord2D> space, int alpha) {
        drawLegs(canvas, space, alpha);
        drawStations(survey, canvas, space, alpha);
    }

    protected void drawLegs(Canvas canvas, Space<Coord2D> space, int baseAlpha) {
        drawLegs(canvas, space, baseAlpha, this::isLegInPlane);
    }

    /** Draws the legs, dashing those that isInPlane says point into or out of the page. */
    private void drawLegs(
            Canvas canvas, Space<Coord2D> space, int baseAlpha, Predicate<Leg> isInPlane) {

        boolean highlightLatestLeg = GeneralPreferences.isHighlightLatestLegModeOn();

        boolean showSplays = SketchPreferences.Toggle.SHOW_SPLAYS.isOn();
        boolean fadingNonActive = SketchPreferences.Toggle.FADE_NON_ACTIVE.isOn();

        Map<Leg, Line<Coord2D>> legMap = space.getLegMap();

        for (Leg leg : legMap.keySet()) {

            if (!showSplays && !leg.hasDestination()) {
                continue;
            }

            Line<Coord2D> line = legMap.get(leg);

            //noinspection ConstantConditions
            Coord2D start = viewport.toView(line.getStart());
            Coord2D end = viewport.toView(line.getEnd());

            if (!viewport.isLineOnCanvas(start, end)) {
                continue;
            }

            boolean fade =
                    baseAlpha == FADED_ALPHA || (fadingNonActive && !isAttachedToActive(leg));

            Paint paint;
            if (highlightLatestLeg && state.getSurvey().getMostRecentLeg() == leg) {
                paint = fade ? paints.fadedLatestLegPaint : paints.latestLegPaint;
            } else if (!leg.hasDestination()) {
                paint = fade ? paints.fadedSplayPaint : paints.splayPaint;
            } else {
                paint = fade ? paints.fadedLegPaint : paints.legPaint;
            }

            if (isInPlane.test(leg)) {
                canvas.drawLine(start.x, start.y, end.x, end.y, paint);
            } else {
                CanvasShapes.drawDashedLine(canvas, start, end, paints.dashedLineIntervalPx, paint);
            }
        }
    }

    /**
     * Whether a leg lies in the plane being drawn. Legs that don't are drawn dashed, to show that
     * they point into or out of the page.
     */
    protected boolean isLegInPlane(Leg leg) {
        return state.getProjectionType().isLegInPlane(leg);
    }

    private boolean isAttachedToActive(Leg leg) {
        return state.getSurvey().getActiveStation().getOnwardLegs().contains(leg);
    }

    private void drawStations(Survey survey, Canvas canvas, Space<Coord2D> space, int baseAlpha) {

        boolean fadingNonActive = SketchPreferences.Toggle.FADE_NON_ACTIVE.isOn();
        boolean showStationLabels = SketchPreferences.Toggle.SHOW_STATION_LABELS.isOn();

        if (fadingNonActive) {
            baseAlpha = FADED_ALPHA;
        }

        int alpha = baseAlpha;
        paints.stationPaint.setAlpha(alpha);

        for (Map.Entry<Station, Coord2D> entry : space.getStationMap().entrySet()) {
            Station station = entry.getKey();

            if (fadingNonActive && (station == survey.getActiveStation())) {
                alpha = SOLID_ALPHA;
                // setting alpha is measured as a relatively expensive call, so we change this as
                // little as possible
                paints.stationPaint.setAlpha(alpha);
            }

            Coord2D translatedStation = viewport.toView(entry.getValue());

            int x = (int) (translatedStation.x);
            int y = (int) (translatedStation.y);

            int stationCrossDiameterIntPx = Math.round(paints.stationCrossDiameterPx);
            CanvasShapes.drawStationCross(
                    canvas, paints.stationPaint, x, y, stationCrossDiameterIntPx, alpha);

            if (station == survey.getActiveStation()) {
                highlightActiveStation(canvas, x, y);
            }

            int spacing = stationCrossDiameterIntPx / 2;
            int nextX = x + stationCrossDiameterIntPx;

            if (showStationLabels) {
                String name = station.getName();
                if (station == survey.getOrigin()) {
                    name = name + " (" + survey.getName() + ")";
                }
                canvas.drawText(name, nextX, y + paints.stationLabelOffsetPx, paints.stationPaint);
                nextX += Math.round(paints.stationPaint.measureText(name)) + spacing;
            }

            List<Bitmap> icons = new ArrayList<>();
            if (station.hasComment()) {
                icons.add(paints.commentIcon);
            }
            if (survey.hasLinkedSurveys(station)) {
                icons.add(paints.linkIcon);
            }

            for (Bitmap icon : icons) {
                int yTop = y - stationCrossDiameterIntPx / 2;
                Rect rect =
                        new Rect(
                                nextX,
                                yTop,
                                nextX + stationCrossDiameterIntPx,
                                yTop + stationCrossDiameterIntPx);
                canvas.drawBitmap(icon, null, rect, paints.iconPaint);
                nextX += stationCrossDiameterIntPx + spacing;
            }

            crossSections.drawIndicator(canvas, station, x, y, alpha);

            if (fadingNonActive && (station == survey.getActiveStation())) {
                alpha = baseAlpha;
                paints.stationPaint.setAlpha(alpha);
            }
        }
    }

    private void highlightActiveStation(Canvas canvas, float x, float y) {

        float diameter = paints.stationCrossDiameterPx * 1.1f;
        float gap = diameter / 3f;
        float topY = y - (diameter / 2);
        float bottomY = y + (diameter / 2);
        float leftX = x - (diameter / 2);
        float rightX = x + (diameter / 2);

        float innerLeft = leftX + ((diameter - gap) / 2);
        float innerRight = innerLeft + gap;
        float innerTop = topY + ((diameter - gap) / 2);
        float innerBottom = innerTop + gap;

        // top-left corner
        Path topLeft = new Path();
        topLeft.moveTo(innerLeft, topY);
        topLeft.lineTo(leftX, topY);
        topLeft.lineTo(leftX, innerTop);
        canvas.drawPath(topLeft, paints.highlightPaint);

        // top-right corner
        Path topRight = new Path();
        topRight.moveTo(innerRight, topY);
        topRight.lineTo(rightX, topY);
        topRight.lineTo(rightX, innerTop);
        canvas.drawPath(topRight, paints.highlightPaint);

        // bottom-left corner
        Path bottomLeft = new Path();
        bottomLeft.moveTo(leftX, innerBottom);
        bottomLeft.lineTo(leftX, bottomY);
        bottomLeft.lineTo(innerLeft, bottomY);
        canvas.drawPath(bottomLeft, paints.highlightPaint);

        // bottom-right corner
        Path bottomRight = new Path();
        bottomRight.moveTo(rightX, innerBottom);
        bottomRight.lineTo(rightX, bottomY);
        bottomRight.lineTo(innerRight, bottomY);
        canvas.drawPath(bottomRight, paints.highlightPaint);
    }

    protected void drawSketch(Canvas canvas, Sketch sketch, int alpha) {
        float viewOffsetX = viewport.getOffsetX();
        float viewOffsetY = viewport.getOffsetY();
        float viewScale = viewport.getScale();

        if (!SketchPreferences.Toggle.SHOW_SKETCH.isOn()) {
            return;
        }

        drawAreas(canvas, sketch, alpha);

        Colour lastColour = Colour.BLACK;

        paints.drawPaint.setColor(lastColour.intValue);
        paints.drawPaint.setAlpha(alpha);

        boolean isDebugMode = activity.isDebugMode();

        for (PathDetail pathDetail : sketch.getPathDetails()) {

            if (!couldBeVisible(pathDetail)) {
                continue;
            }

            if (pathDetail.getLineType() != LineType.SKETCH) {
                drawTypedPath(canvas, pathDetail, alpha, isFinishedWall(sketch, pathDetail));
                continue;
            }

            // Avoiding constantly updating the paint colour saves approx. 10% of sketch draw time.
            // Ideally getPathDetails() would return the paths in colour order but HashSets
            // are unordered collections
            Colour drawColour = pathDetail.getDrawColour(isDarkModeActive);
            if (drawColour != lastColour) {
                lastColour = drawColour;
                paints.drawPaint.setColor(lastColour.intValue);
            }

            List<Coord2D> path = pathDetail.getPath();

            int lineIndex = 0;
            float fromX = -1, fromY = -1;
            float[] lines = new float[path.size() * 4];

            // This loop is the slowest part of the draw phase. Pulling out the calculations from
            // within Viewport.toView saves a not insignificant amount of time by not
            // constructing many thousands of Coord2D objects (approx. 10% of sketch draw time)
            for (Coord2D point : path) {
                if (fromX == -1) {
                    // from = viewport.toView(point);
                    fromX = (point.x - viewOffsetX) * viewScale;
                    fromY = (point.y - viewOffsetY) * viewScale;

                    if (isDebugMode) {
                        canvas.drawCircle(fromX, fromY, 3, paints.drawPaint);
                    }
                } else {
                    // Coord2D to = viewport.toView(point);
                    float toX = (point.x - viewOffsetX) * viewScale;
                    float toY = (point.y - viewOffsetY) * viewScale;

                    lines[lineIndex++] = fromX;
                    lines[lineIndex++] = fromY;
                    lines[lineIndex++] = toX;
                    lines[lineIndex++] = toY;

                    if (isDebugMode) {
                        canvas.drawCircle(toX, toY, 3, paints.drawPaint);
                    }

                    fromX = toX;
                    fromY = toY;
                }
            }

            canvas.drawLines(lines, paints.drawPaint);
        }

        paints.labelPaint.setAlpha(alpha);

        for (TextDetail textDetail : sketch.getTextDetails()) {
            Coord2D location = viewport.toView(textDetail.getPosition());
            float x = location.x, y = location.y;
            String text = textDetail.getText();
            setDrawColour(paints.labelPaint, textDetail);
            paints.labelPaint.setTextSize(textDetail.getSize() * viewScale);
            for (String line : text.split("\n")) {
                canvas.drawText(line, x, y, paints.labelPaint);
                y += paints.labelPaint.descent() - paints.labelPaint.ascent();
            }
        }

        for (SymbolDetail symbolDetail : sketch.getSymbolDetails()) {
            if (!couldBeVisible(symbolDetail)) {
                continue;
            }
            Coord2D location = viewport.toView(symbolDetail.getPosition());

            Symbol symbol = symbolDetail.getSymbol();

            int size = Math.round(symbolDetail.getSize() * viewScale);
            if (size < 1) {
                continue;
            }

            Drawable drawable = symbolDetail.getDrawable();
            float offset = size / 2f;
            int x = Math.round(location.x - offset), y = Math.round(location.y - offset);
            drawable.setBounds(x, y, x + size, y + size);
            drawable.setAlpha(alpha);
            Colour drawColour = symbolDetail.getDrawColour(isDarkModeActive);
            drawable.setColorFilter(
                    new PorterDuffColorFilter(drawColour.intValue, PorterDuff.Mode.SRC_IN));

            if (symbol.isDirectional()) {
                RotateDrawable rotateDrawable = new RotateDrawable();
                rotateDrawable.setDrawable(drawable);
                rotateDrawable.setPivotX(0.5f);
                rotateDrawable.setPivotY(0.5f);
                rotateDrawable.setLevel((int) (10000 * (symbolDetail.getAngle() / 360f)));
                rotateDrawable.setBounds(x, y, x + size, y + size);
                rotateDrawable.draw(canvas);

            } else { // skip some calcs for efficiency
                drawable.draw(canvas);
            }
        }
    }

    /**
     * Semantic lines are drawn individually via drawPath so their type's ornamentation (dashes,
     * tick stamps) can apply. Ornamentation is sized by getPatternScale.
     */
    private void drawTypedPath(
            Canvas canvas, PathDetail pathDetail, int alpha, boolean shadeRockSide) {
        float viewOffsetX = viewport.getOffsetX();
        float viewOffsetY = viewport.getOffsetY();
        float viewScale = viewport.getScale();
        Paint paint =
                paints.getLineTypePaint(
                        pathDetail.getLineType(), SketchPaints.getPatternScale(viewScale));
        paint.setColor(pathDetail.getDrawColour(isDarkModeActive).intValue);
        paint.setAlpha(alpha);

        typedPathScratch.rewind();
        boolean first = true;
        for (Coord2D point : pathDetail.getPath()) {
            float x = (point.x - viewOffsetX) * viewScale;
            float y = (point.y - viewOffsetY) * viewScale;
            if (first) {
                typedPathScratch.moveTo(x, y);
                first = false;
            } else {
                typedPathScratch.lineTo(x, y);
            }
        }
        if (shadeRockSide) {
            drawRockSide(canvas, typedPathScratch, paint.getColor(), alpha);
        }
        canvas.drawPath(typedPathScratch, paint);
    }

    /** Shades the outer side of a wall line type. */
    private void drawRockSide(Canvas canvas, Path path, int colour, int alpha) {
        List<WallRockSide.Layer> layers = WallRockSide.getLayers();
        Paint[] layerPaints =
                paints.getWallRockSidePaints(SketchPaints.getPatternScale(viewport.getScale()));
        for (int i = 0; i < layers.size(); i++) {
            Paint paint = layerPaints[i];
            paint.setColor(colour);
            paint.setAlpha(alpha * layers.get(i).getAlpha() / 255);
            canvas.drawPath(path, paint);
        }
    }

    /**
     * Whether a line can have its rock side shaded: any wall-kind line, excluding a stroke still in
     * progress (its orientation isn't decided until the stroke finishes). Ticked types need no
     * shading — their ornamentation already shows their orientation.
     */
    private static boolean isFinishedWall(Sketch sketch, PathDetail pathDetail) {
        return pathDetail.getLineType().isWall() && pathDetail != sketch.getActivePath();
    }

    /**
     * Draw the sketch's areas: each region is filled with its type's pattern and outlined. Holes
     * are drawn as further contours of the same path under the even-odd fill rule, so they are
     * outlined too but left unfilled.
     *
     * <p>The pattern is anchored to survey space and sized by getPatternScale, so it stays put on
     * the cave as the view is panned or zoomed. Every area of a type shares the same pattern, so
     * adjacent areas join seamlessly.
     */
    private void drawAreas(Canvas canvas, Sketch sketch, int alpha) {
        float viewOffsetX = viewport.getOffsetX();
        float viewOffsetY = viewport.getOffsetY();
        float viewScale = viewport.getScale();

        float patternScale = SketchPaints.getPatternScale(viewScale);
        areaFillMatrix.setScale(
                patternScale / SketchPaints.AREA_FILL_OVERSAMPLE,
                patternScale / SketchPaints.AREA_FILL_OVERSAMPLE);
        areaFillMatrix.postTranslate(-viewOffsetX * viewScale, -viewOffsetY * viewScale);

        for (AreaDetail areaDetail : sketch.getAreaDetails()) {

            if (!couldBeVisible(areaDetail)) {
                continue;
            }

            Path outline = new Path();
            outline.setFillType(Path.FillType.EVEN_ODD);
            for (List<Coord2D> contour : areaDetail.getContours()) {
                boolean first = true;
                for (Coord2D point : contour) {
                    float x = (point.x - viewOffsetX) * viewScale;
                    float y = (point.y - viewOffsetY) * viewScale;
                    if (first) {
                        outline.moveTo(x, y);
                        first = false;
                    } else {
                        outline.lineTo(x, y);
                    }
                }
                outline.close();
            }

            Colour colour = areaDetail.getDrawColour(isDarkModeActive);

            // the fill tile is an alpha mask, so it takes the paint's colour
            BitmapShader shader = paints.getAreaFillShader(areaDetail.getAreaType());
            shader.setLocalMatrix(areaFillMatrix);
            paints.areaFillPaint.setShader(shader);
            paints.areaFillPaint.setColor(colour.intValue);
            paints.areaFillPaint.setAlpha(alpha);
            canvas.drawPath(outline, paints.areaFillPaint);

            paints.areaPaint.setColor(colour.intValue);
            if (areaDetail.getAreaType().hasDefinedEdge()) {
                paints.areaPaint.setAlpha(alpha);
                paints.areaPaint.setStrokeWidth(paints.drawPaint.getStrokeWidth());
            } else {
                paints.areaPaint.setAlpha(Math.round(alpha * AreaType.LIGHT_OUTLINE_OPACITY));
                paints.areaPaint.setStrokeWidth(dpToPixels(AreaType.Fill.STROKE_WIDTH_DP));
            }
            canvas.drawPath(outline, paints.areaPaint);
        }
    }

    private void setDrawColour(Paint paint, SketchDetail sketchDetail) {
        Colour colour = sketchDetail.getDrawColour(isDarkModeActive);
        paint.setColor(colour.intValue);
    }

    protected void drawLegend(Canvas canvas) {

        String surveyLabel =
                state.getSurvey().getName()
                        + " L"
                        + TextTools.formatTo0dpWithComma(surveyLength)
                        + " V"
                        + TextTools.formatTo0dpWithComma(surveyHeight);

        float legendSize = paints.legendPaint.getTextSize();
        float offsetX = legendSize * 1.25f;
        float offsetY = legendSize * 1.25f;
        float y = getHeight() - offsetY;
        float x = offsetX;
        canvas.drawText(surveyLabel, x, y, paints.legendPaint);

        int minorGridSize = getMinorGridBoxSize();
        float scaleWidth = viewport.getScale() * minorGridSize;
        float scaleOffsetY = offsetY * 2;
        float scaleY = getHeight() - scaleOffsetY;
        paints.legendPaint.setStyle(Paint.Style.STROKE);
        Path scalePath = new Path();
        scalePath.moveTo(x, scaleY - paints.legendTickSizePx);
        scalePath.lineTo(x, scaleY);
        scalePath.lineTo(x + scaleWidth, scaleY);
        scalePath.lineTo(x + scaleWidth, scaleY - paints.legendTickSizePx);
        canvas.drawPath(scalePath, paints.legendPaint);
        paints.legendPaint.setStyle(Paint.Style.FILL);
        String scaleLabel = minorGridSize + "m";
        canvas.drawText(scaleLabel, x + scaleWidth + 0.3f * legendSize, scaleY, paints.legendPaint);
    }

    /**
     * Whether to draw the compass, which turns to show which way the device is facing. That only
     * means something in a view drawn with north at the top.
     */
    protected boolean isCompassShown() {
        return state.getProjectionType() == Projection2D.PLAN;
    }

    protected void drawCompass(Canvas canvas) {
        if (!SketchPreferences.Toggle.SHOW_COMPASS.isOn() || !isCompassShown()) {
            return;
        }

        float textSize = paints.legendPaint.getTextSize();
        Paint.FontMetrics metrics = paints.legendPaint.getFontMetrics();
        float textHeight = metrics.descent - metrics.ascent;
        float offsetX = textSize * 1.25f; // matches legend x
        float arrowLength = textSize * 2.5f;
        float arrowHeadSize = textSize * 0.6f;
        float cx = offsetX + arrowLength / 2f + textSize;
        float scaleBarY = getHeight() - textSize * 4f;
        float cy = scaleBarY - arrowLength / 2f - textHeight;

        canvas.save();
        canvas.rotate(-compassAzimuthDegrees, cx, cy);

        float tipY = cy - arrowLength / 2f;
        float tailY = cy + arrowLength / 2f;

        paints.legendPaint.setStyle(Paint.Style.STROKE);
        Path arrowPath = new Path();
        arrowPath.moveTo(cx - arrowHeadSize, tipY + arrowHeadSize);
        arrowPath.lineTo(cx, tipY);
        arrowPath.lineTo(cx + arrowHeadSize, tipY + arrowHeadSize);
        arrowPath.moveTo(cx, tipY);
        arrowPath.lineTo(cx, tailY);
        canvas.drawPath(arrowPath, paints.legendPaint);
        paints.legendPaint.setStyle(Paint.Style.FILL);
        canvas.drawText("N", cx - textSize * 0.35f, tipY - textSize * 0.2f, paints.legendPaint);

        canvas.restore();
    }

    private void drawHotCorners(Canvas canvas) {

        if (!isHotCornersModeActive) {
            return;
        }

        if (state.getSketchTool() == SketchTool.MODAL_MOVE) {
            paints.hotCornersPaint.setColor(
                    ContextCompat.getColor(getContext(), R.color.hotCornerActive));
            paints.hotCornersPaint.setAlpha(FADED_ALPHA);
        }

        final int side = (int) (HOT_CORNER_DISTANCE_PROPORTION * Math.min(getWidth(), getHeight()));
        topLeftCorner = new Rect(0, 0, side, side);
        topRightCorner = new Rect(getWidth() - side, 0, getWidth(), side);
        bottomRightCorner =
                new Rect(getWidth() - side, getHeight() - side, getWidth(), getHeight());

        canvas.drawRect(topLeftCorner, paints.hotCornersPaint);
        canvas.drawRect(topRightCorner, paints.hotCornersPaint);
        canvas.drawRect(bottomRightCorner, paints.hotCornersPaint);

        if (state.getSketchTool() == SketchTool.MODAL_MOVE) {
            paints.hotCornersPaint.setColor(
                    ContextCompat.getColor(getContext(), R.color.hotCorner));
            paints.hotCornersPaint.setAlpha(FADED_ALPHA);
        }
    }

    private void drawDebuggingInfo(Canvas canvas) {
        if (!activity.isDebugMode()) {
            return;
        }

        float offsetX = getWidth() * 0.03f;
        float offsetY = LEGEND_SIZE * 2;
        String label =
                "x="
                        + offsetX
                        + " y="
                        + offsetY
                        + " s2v="
                        + TextTools.formatTo2dp(viewport.getScale())
                        + " 1/s2v="
                        + TextTools.formatTo2dp(1 / viewport.getScale())
                        + "\n log (1/s2v) ="
                        + TextTools.formatTo2dp(Math.log(1 / viewport.getScale()))
                        + "\n log10 (1/s2v) ="
                        + TextTools.formatTo2dp(Math.log10(1 / viewport.getScale()));

        canvas.drawText(label, offsetX, offsetY, paints.legendPaint);
    }

    /**
     * Returns true if a sketch detail could be visible in the current view — i.e. its bounding box
     * intersects the screen area and it is large enough to occupy at least one pixel at the current
     * zoom level. Details that fail either check can be skipped without drawing.
     */
    private boolean couldBeVisible(SketchDetail sketchDetail) {
        return viewport.couldBeVisible(sketchDetail);
    }

    public void centreViewOnActiveStation() {
        centreViewOnStation(state.getSurvey().getActiveStation());
    }

    public void centreViewOnStation(Station station) {
        Coord2D activeStationCoord = state.getProjection().getStationMap().get(station);

        // not sure how this could be null, but at least one null pointer has been reported
        if (activeStationCoord == null) {
            activeStationCoord = Coord2D.ORIGIN;
        }

        centreViewOnSurveyPoint(activeStationCoord);
    }

    public void centreViewOnSurveyPoint(Coord2D point) {
        viewport.centreOn(point, getWidth(), getHeight());
    }

    public void adjustZoomBy(float delta) {
        float newZoom = viewport.getScale() * delta;
        setZoom(newZoom);
    }

    public void setZoom(float newZoom) {
        Coord2D centre = new Coord2D(getWidth() / 2f, getHeight() / 2f);
        setZoom(newZoom, centre);
    }

    public void setZoom(float newZoom, Coord2D focusOnScreen) {
        viewport.setZoom(newZoom, focusOnScreen);
    }

    public boolean canUndo() {
        return state.getSketch().canUndo();
    }

    public boolean canRedo() {
        return state.getSketch().canRedo();
    }

    public void undo() {
        state.getSketch().undo();
        invalidate();
    }

    public void redo() {
        state.getSketch().redo();
        invalidate();
    }

    public void setBrushColour(BrushColour brushColour) {
        currentColour = brushColour.getColour();
    }

    public void setLineType(LineType lineType) {
        currentLineType = lineType;
    }

    public void setAreaType(AreaType areaType) {
        currentAreaType = areaType;
    }

    public void setCurrentSymbol(Symbol symbol) {
        currentSymbol = symbol;
    }

    public void setSketchTool(SketchTool sketchTool) {
        state.setSketchTool(sketchTool);
    }

    public void setCompassAzimuth(float degrees) {
        compassAzimuthDegrees = degrees;
        if (SketchPreferences.Toggle.SHOW_COMPASS.isOn()
                && state.getProjectionType() == Projection2D.PLAN) {
            invalidate();
        }
    }

    public void setCachedStats(float surveyLength, float surveyHeight) {
        this.surveyLength = surveyLength;
        this.surveyHeight = surveyHeight;
    }

    public SketchTool getSketchTool() {
        return state.getSketchTool();
    }

    /** Gives the cross-section layer what it needs from this view, without making it public. */
    private class CrossSectionHost implements CrossSectionLayer.Host {
        @Override
        public Context getContext() {
            return SketchView.this.getContext();
        }

        @Override
        public ViewContext getViewContext() {
            return SketchView.this.getViewContext();
        }

        @Override
        public void showToast(int stringId) {
            activity.showSimpleToast(stringId);
        }

        @Override
        public void invalidate() {
            SketchView.this.invalidate();
        }

        @Override
        public void drawSketch(Canvas canvas, Sketch sketch, int alpha) {
            SketchView.this.drawSketch(canvas, sketch, alpha);
        }

        @Override
        public void drawLegs(
                Canvas canvas, Space<Coord2D> space, int alpha, Predicate<Leg> isInPlane) {
            SketchView.this.drawLegs(canvas, space, alpha, isInPlane);
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (!SketchPreferences.Toggle.PINCH_TO_ZOOM.isOn()) {
                return false;
            }

            if (state.getSketchTool() != SketchTool.PINCH_TO_ZOOM) {
                setSketchTool(SketchTool.PINCH_TO_ZOOM);
            }

            float x = detector.getFocusX();
            float y = detector.getFocusY();
            Coord2D focus = new Coord2D(x, y);

            float scaleFactor = detector.getScaleFactor();
            setZoom(viewport.getScale() * scaleFactor, focus);

            invalidate();
            return true;
        }
    }

    private class LongPressListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public void onLongPress(MotionEvent motionEvent) {
            Coord2D touchPointOnView = new Coord2D(motionEvent.getX(), motionEvent.getY());
            Station matchedStation = checkForStation(touchPointOnView);
            if (matchedStation != null && !menuShownInThisTouch) {
                if (state.getSketchTool().drawsPath()) {
                    state.getSketch().abandonActivePath();
                }
                showContextMenu(motionEvent, matchedStation);
                menuShownInThisTouch = true;
                return;
            }

            if (!state.getSketchTool().offersLineMenu()) {
                return;
            }
            PathDetail matchedPath = checkForPath(touchPointOnView);
            if (matchedPath != null && !menuShownInThisTouch) {
                state.getSketch().abandonActivePath();
                showLineContextMenu(motionEvent, matchedPath);
                menuShownInThisTouch = true;
            }
        }
    }
}

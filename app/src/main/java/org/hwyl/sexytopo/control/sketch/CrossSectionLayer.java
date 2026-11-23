package org.hwyl.sexytopo.control.sketch;

import static org.hwyl.sexytopo.control.sketch.ScreenUnits.dpToPixels;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.activity.CrossSectionActivity;
import org.hwyl.sexytopo.control.util.CrossSectioner;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.SketchPreferences;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.geometry.Projection2D;
import org.hwyl.sexytopo.model.geometry.Space;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.sketch.CrossSectionDetail;
import org.hwyl.sexytopo.model.sketch.SketchTool;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

/**
 * Everything to do with cross-sections on a sketch view: drawing them (the projected legs and
 * sub-sketch in a framed box with a drag handle, the connector to the station, the indicator at the
 * station), and the gestures that place, move, rotate and open them.
 *
 * <p>Drawing and touch share state here. Drawing records where each handle is, for touch to
 * hit-test, and the move and rotate gestures keep the in-progress drag or angle, for drawing to
 * preview.
 */
class CrossSectionLayer {

    /** What the layer needs the view it's part of to do, beyond what any tool does. */
    interface Host extends ToolHost {
        ViewContext getViewContext();
    }

    private static final int CONNECTOR_WIDTH_DP = 2;
    private static final int INDICATOR_WIDTH_DP = 2;
    private static final int BORDER_WIDTH_DP = 2;
    private static final int BORDER_PADDING_MIN_DP = 4;
    private static final int BORDER_PADDING_MAX_DP = 16;
    private static final float BORDER_PADDING_FRACTION = 0.05f;
    private static final float BORDER_CORNER_RADIUS_DP = 6.0f;
    private static final int HANDLE_WIDTH_DP = 8;
    private static final int HANDLE_HIT_PADDING_DP = 8;
    private static final float GLYPH_SIZE_DP = 13f;
    private static final float GLYPH_BOX_SIZE_DP = 18f;
    private static final int HANDLE_GRIP_WIDTH_DP = 2;
    private static final float HANDLE_GRIP_SPACING_DP = 5f;
    private static final float HANDLE_GRIP_LENGTH_FRACTION = 0.45f;

    private final Viewport viewport;
    private final SketchState state;
    private final SurveyDataRenderer surveyDataRenderer;
    private final Host host;

    // Each cross-section's legs and sketch are drawn through this, set up in turn for each section
    // as an inset in the main view, rather than from a copy moved and scaled into place
    private final Viewport insetViewport = new Viewport();
    private final SurveyDataRenderer insetDataRenderer;
    private final SketchRenderer insetSketchRenderer;

    private final Paint connectorPaint = new Paint();
    private final Paint indicatorPaint = new Paint();
    private final Paint verticalHandlePaint = new Paint();
    private final Paint handleGripPaint = new Paint();
    private final Paint verticalBorderPaint = new Paint();
    private final Paint horizontalHandlePaint = new Paint();
    private final Paint horizontalBorderPaint = new Paint();
    private final Paint glyphPaint = new Paint();
    private final Paint backgroundPaint = new Paint();

    private final Paint[] ANTI_ALIAS_PAINTS =
            new Paint[] {
                connectorPaint,
                indicatorPaint,
                verticalHandlePaint,
                handleGripPaint,
                verticalBorderPaint,
                horizontalHandlePaint,
                horizontalBorderPaint,
                glyphPaint
            };

    // Set when a press opens a cross-section's editor; the rest of that touch is swallowed.
    private boolean tappedInThisTouch = false;

    // With the move tool, a press on a cross-section opens it only if it turns out to be a tap
    // rather than a pan, so this is the one pressed, until the finger lifts or wanders off
    private CrossSectionDetail pressedWhileMoving = null;
    private Coord2D pressedWhileMovingAt = Coord2D.ORIGIN;

    // The station and orientation a new cross-section is being made for, until it's placed
    private String stationNameBeingSectioned = null;
    private CrossSection.Orientation orientationBeingCreated = CrossSection.Orientation.VERTICAL;

    // State for dragging a cross-section by its handle
    private CrossSectionDetail beingMoved = null;
    private Coord2D moveAnchorOnSurvey = Coord2D.ORIGIN;
    private Coord2D moveCurrentDelta = Coord2D.ORIGIN;

    // State for rotating a cross-section via drag-from-station gesture
    private CrossSectionDetail beingRotated = null;
    private Coord2D rotateFingerOnView = null;
    private Float previewAngle = null;

    // Cached per-detail handle rectangles (in view coords) populated each draw; used for hit-test.
    private final Map<CrossSectionDetail, RectF> handleRects = new LinkedHashMap<>();

    /** Places a new cross-section where the user lifts. */
    final ToolHandler positionTool = this::handlePosition;

    /** Drags a cross-section by its handle. */
    final ToolHandler moveTool = this::handleMove;

    /** Turns a cross-section to face the finger, showing a guide line from its station. */
    final ToolHandler rotateTool =
            new ToolHandler() {
                @Override
                public boolean onTouch(MotionEvent event) {
                    return handleRotate(event);
                }

                @Override
                public void drawFeedback(Canvas canvas) {
                    drawRotationGuideLine(canvas);
                }
            };

    CrossSectionLayer(
            Viewport viewport,
            SketchState state,
            SurveyDataRenderer surveyDataRenderer,
            BooleanSupplier isDebugMode,
            Host host) {
        this.viewport = viewport;
        this.state = state;
        this.surveyDataRenderer = surveyDataRenderer;
        this.host = host;
        this.insetDataRenderer = new SurveyDataRenderer(insetViewport, state);
        this.insetSketchRenderer = new SketchRenderer(insetViewport, isDebugMode);
    }

    /** In dark mode, details drawn in black are drawn in white instead. */
    void setDarkModeActive(boolean isDarkModeActive) {
        insetSketchRenderer.setDarkModeActive(isDarkModeActive);
    }

    /** Set the paints up from the theme and current preferences. */
    void initialisePaints(Context context) {

        insetDataRenderer.initialise(context);
        insetSketchRenderer.initialise(context);

        SketchPaints.applyAntiAliasPreference(ANTI_ALIAS_PAINTS);

        connectorPaint.setColor(ContextCompat.getColor(context, R.color.crossSectionConnection));
        connectorPaint.setStrokeWidth(dpToPixels(CONNECTOR_WIDTH_DP));
        connectorPaint.setStyle(Paint.Style.STROKE);

        indicatorPaint.setColor(ContextCompat.getColor(context, R.color.crossSectionIndicator));
        indicatorPaint.setStrokeWidth(dpToPixels(INDICATOR_WIDTH_DP));
        indicatorPaint.setStyle(Paint.Style.FILL);

        int primaryColor =
                SketchPaints.resolveThemeColor(context, androidx.appcompat.R.attr.colorPrimary);

        verticalHandlePaint.setColor(primaryColor);
        verticalHandlePaint.setStyle(Paint.Style.FILL);
        verticalHandlePaint.setAntiAlias(true);

        handleGripPaint.setColor(
                SketchPaints.resolveThemeColor(
                        context, com.google.android.material.R.attr.colorOnPrimary));
        handleGripPaint.setStrokeWidth(dpToPixels(HANDLE_GRIP_WIDTH_DP));
        handleGripPaint.setStyle(Paint.Style.STROKE);
        handleGripPaint.setStrokeCap(Paint.Cap.ROUND);

        verticalBorderPaint.setColor(primaryColor);
        verticalBorderPaint.setStrokeWidth(dpToPixels(BORDER_WIDTH_DP));
        verticalBorderPaint.setStyle(Paint.Style.STROKE);

        // Horizontal cross-sections get a differently coloured box so they can be told apart from
        // vertical ones at a glance
        int horizontalBoxColour =
                ContextCompat.getColor(context, R.color.crossSectionHorizontalBox);

        horizontalHandlePaint.setColor(horizontalBoxColour);
        horizontalHandlePaint.setStyle(Paint.Style.FILL);
        horizontalHandlePaint.setAntiAlias(true);

        horizontalBorderPaint.setColor(horizontalBoxColour);
        horizontalBorderPaint.setStrokeWidth(dpToPixels(BORDER_WIDTH_DP));
        horizontalBorderPaint.setStyle(Paint.Style.STROKE);

        glyphPaint.setTextSize(dpToPixels(GLYPH_SIZE_DP));
        glyphPaint.setTextAlign(Paint.Align.CENTER);
        glyphPaint.setStyle(Paint.Style.FILL);

        // The frame is filled with the sketch's own background, so what's underneath doesn't show
        // through and muddle the section
        backgroundPaint.setColor(ContextCompat.getColor(context, R.color.lightBackground));
        backgroundPaint.setStyle(Paint.Style.FILL);
    }

    // ********** Starting gestures from outside the view **********

    void startPositioning(Station station, CrossSection.Orientation orientation) {
        stationNameBeingSectioned = station.getName();
        orientationBeingCreated = orientation;
        state.startTemporaryTool(SketchTool.POSITION_CROSS_SECTION);
        host.showToast(R.string.sketch_position_cross_section_instruction);
    }

    void startRotating(Station station) {
        CrossSectionDetail detail = state.getSketch().getCrossSectionDetail(station);
        if (detail == null || !detail.getCrossSection().isRotatable()) {
            return;
        }
        beingRotated = detail;
        rotateFingerOnView = null;
        previewAngle = null;
        state.startTemporaryTool(SketchTool.ROTATE_CROSS_SECTION);
        host.showToast(R.string.sketch_rotate_cross_section_instruction);
    }

    void launchEditor(CrossSectionDetail detail) {
        Station station = detail.getCrossSection().getStation();
        if (station == null) {
            return;
        }
        Context context = host.getContext();
        Intent intent = new Intent(context, CrossSectionActivity.class);
        intent.putExtra(CrossSectionActivity.EXTRA_STATION_NAME, station.getName());
        Projection2D projectionType = state.getProjectionType();
        Projection2D parentProjection = projectionType == null ? Projection2D.PLAN : projectionType;
        intent.putExtra(CrossSectionActivity.EXTRA_PROJECTION, parentProjection.getAbbreviation());
        context.startActivity(intent);
    }

    // ********** Touch **********

    /** Call at the start of each touch sequence. */
    void onTouchStart() {
        tappedInThisTouch = false;
        pressedWhileMoving = null;
    }

    /** Whether this event is a press on a cross-section's drag handle, starting a move. */
    boolean isHandlePress(MotionEvent event) {
        if (GeneralPreferences.isLegacyCrossSectionsOn()) {
            return false; // no drag handle is drawn for legacy cross-sections
        }
        boolean currentlyActive = state.getSketchTool() == SketchTool.MOVE_CROSS_SECTION;
        boolean isDown = event.getAction() == MotionEvent.ACTION_DOWN;
        boolean hitAHandle = findHandleAt(event.getX(), event.getY()) != null;
        return !currentlyActive && isDown && hitAHandle;
    }

    /**
     * If the user presses on a cross-section, open its editor. Returns true if this touch has
     * opened one, in which case the rest of the touch should be ignored.
     */
    boolean handleBodyTap(MotionEvent event) {
        if (!SketchPreferences.Toggle.SHOW_X_SECTIONS.isOn()) {
            return false; // special case: can't tap on invisible X-sections
        }
        if (GeneralPreferences.isLegacyCrossSectionsOn()) {
            return false; // in legacy mode there is no frame to tap
        }
        SketchTool currentSketchTool = state.getSketchTool();
        if (currentSketchTool == SketchTool.MOVE) {
            handleTapWhileMoving(event);
            return false; // let the pan run; it's harmless if this turns out to be a tap
        }
        if (currentSketchTool == SketchTool.ERASE) {
            return false; // let handleErase deal with deleting cross-sections
        }
        if (currentSketchTool.isModal() || currentSketchTool == SketchTool.MOVE_CROSS_SECTION) {
            return false; // a handle drag, pan or pinch is in progress; let it run
        }

        // Open the editor as soon as a press lands on a cross-section body, before any stroke can
        // start on the main sketch over it. The remaining events of this gesture are swallowed.
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Coord2D touchOnSurvey = viewport.toSurvey(new Coord2D(event.getX(), event.getY()));
            CrossSectionDetail tapped = findBodyAt(touchOnSurvey);
            if (tapped != null) {
                launchEditor(tapped);
                tappedInThisTouch = true;
            }
            return tappedInThisTouch;
        }
        return tappedInThisTouch;
    }

    /**
     * With the move tool a press could be the start of a pan, so a cross-section is only opened
     * once the finger lifts, if it hasn't moved far or been held down long enough to be a
     * long-press.
     */
    private void handleTapWhileMoving(MotionEvent event) {
        Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressedWhileMoving = findBodyAt(viewport.toSurvey(touchPointOnView));
                pressedWhileMovingAt = touchPointOnView;
                break;

            case MotionEvent.ACTION_MOVE:
                if (pressedWhileMoving != null
                        && Space2DUtils.getDistance(touchPointOnView, pressedWhileMovingAt)
                                > getTouchSlop()) {
                    pressedWhileMoving = null;
                }
                break;

            case MotionEvent.ACTION_UP:
                long heldFor = event.getEventTime() - event.getDownTime();
                if (pressedWhileMoving != null
                        && heldFor < ViewConfiguration.getLongPressTimeout()) {
                    launchEditor(pressedWhileMoving);
                }
                pressedWhileMoving = null;
                break;

            default: // a second finger or a cancel: not a tap
                pressedWhileMoving = null;
                break;
        }
    }

    private float getTouchSlop() {
        return ViewConfiguration.get(host.getContext()).getScaledTouchSlop();
    }

    /**
     * Place the cross-section where the user has selected lifts.
     *
     * @noinspection SameReturnValue
     */
    private boolean handlePosition(MotionEvent event) {

        // Acting on UP rather than DOWN means this tool owns the
        // whole gesture; switching tools on DOWN would hand the rest
        // of the gesture to the previous tool, which never saw its
        // DOWN (e.g. MOVE would pan from a stale anchor and jump the
        // screen).
        if (event.getAction() != MotionEvent.ACTION_UP) {
            return true;
        }

        Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        Coord2D touchPointOnSurvey = viewport.toSurvey(touchPointOnView);

        Survey survey = state.getSurvey();
        final Station station = survey.getStationByName(stationNameBeingSectioned);
        stationNameBeingSectioned = null;
        CrossSection.Orientation orientation = orientationBeingCreated;
        orientationBeingCreated = CrossSection.Orientation.VERTICAL;
        if (station == null) {
            return true;
        }

        CrossSection crossSection = CrossSectioner.section(survey, station, orientation);

        CrossSectionDetail detail = new CrossSectionDetail(crossSection, touchPointOnSurvey);
        state.getSketch().addCrossSection(detail);

        state.endTemporaryTool();
        host.invalidate();

        return true;
    }

    /**
     * Drive the MOVE_CROSS_SECTION drag. The view switches to that tool when DOWN lands on a handle
     * (see isHandlePress); this is then dispatched for the rest of the touch sequence.
     */
    private boolean handleMove(MotionEvent event) {
        Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                CrossSectionDetail detailOnHandle =
                        findHandleAt(touchPointOnView.x, touchPointOnView.y);
                if (detailOnHandle == null) {
                    state.endTemporaryTool();
                    return false;
                }
                beingMoved = detailOnHandle;
                moveAnchorOnSurvey = viewport.toSurvey(touchPointOnView);
                moveCurrentDelta = Coord2D.ORIGIN;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (beingMoved != null) {
                    Coord2D current = viewport.toSurvey(touchPointOnView);
                    moveCurrentDelta = current.minus(moveAnchorOnSurvey);
                    host.invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (beingMoved != null) {
                    Coord2D current = viewport.toSurvey(touchPointOnView);
                    Coord2D delta = current.minus(moveAnchorOnSurvey);
                    CrossSectionDetail moved = beingMoved;
                    beingMoved = null;
                    moveCurrentDelta = Coord2D.ORIGIN;
                    if (delta.x != 0 || delta.y != 0) {
                        state.getSketch().replaceCrossSectionDetail(moved, moved.translate(delta));
                    }
                    host.invalidate();
                }
                state.endTemporaryTool();
                return true;

            case MotionEvent.ACTION_CANCEL:
                beingMoved = null;
                moveCurrentDelta = Coord2D.ORIGIN;
                state.endTemporaryTool();
                return true;

            default:
                return false;
        }
    }

    /**
     * Handle the rotation drag for a cross-section. On the plan the compass azimuth is computed
     * from the station's position in the main survey to the finger location. On the elevation,
     * which has no bearings, the section snaps to face left or right, whichever side of the station
     * the finger is on.
     */
    private boolean handleRotate(MotionEvent event) {
        if (beingRotated == null) {
            state.endTemporaryTool();
            return false;
        }

        Coord2D pivotOnSurvey = getRotationPivot(beingRotated);
        if (pivotOnSurvey == null) {
            state.endTemporaryTool();
            return false;
        }
        Coord2D fingerOnView = new Coord2D(event.getX(), event.getY());
        Coord2D fingerOnSurvey = viewport.toSurvey(fingerOnView);
        float dx = fingerOnSurvey.x - pivotOnSurvey.x;
        float dy = fingerOnSurvey.y - pivotOnSurvey.y;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                rotateFingerOnView = fingerOnView;
                Float angle = getRotationAngle(beingRotated, dx, dy);
                if (angle != null) {
                    previewAngle = angle;
                }
                host.invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                Float newAngle = getRotationAngle(beingRotated, dx, dy);
                if (newAngle != null) {
                    CrossSectionDetail rotated = beingRotated.withAngle(newAngle);
                    state.getSketch().replaceCrossSectionDetail(beingRotated, rotated);
                }
                beingRotated = null;
                rotateFingerOnView = null;
                previewAngle = null;
                state.endTemporaryTool();
                host.invalidate();
                return true;

            case MotionEvent.ACTION_CANCEL:
                beingRotated = null;
                rotateFingerOnView = null;
                previewAngle = null;
                state.endTemporaryTool();
                host.invalidate();
                return true;
        }
        return true;
    }

    /** Pivot for rotation: the station's location in the main survey projection. */
    private Coord2D getRotationPivot(CrossSectionDetail detail) {
        Station station = detail.getCrossSection().getStation();
        Space<Coord2D> projection = state.getProjection();
        if (station == null || projection == null) {
            return null;
        }
        return projection.getStationMap().get(station);
    }

    /**
     * The bearing to turn a cross-section to for a finger at (dx, dy) from its station, or null if
     * the finger is too close to the station to say.
     */
    private Float getRotationAngle(CrossSectionDetail detail, float dx, float dy) {
        if (state.getProjectionType() == Projection2D.EXTENDED_ELEVATION) {
            if (dx == 0) {
                return null;
            }
            return CrossSectioner.getAngleFacingOnElevation(
                    state.getSurvey(), detail.getCrossSection().getStation(), dx > 0);
        }
        if (dx == 0 && dy == 0) {
            return null;
        }
        return toAzimuth(dx, dy);
    }

    /** Compass azimuth (0 = North, 90 = East) for a vector in plan-view survey coords. */
    private static float toAzimuth(float dx, float dy) {
        // Plan view: +x = East, -y = North. atan2(x, -y) gives bearing clockwise from North.
        double radians = Math.atan2(dx, -dy);
        float degrees = (float) Math.toDegrees(radians);
        return ((degrees % 360) + 360) % 360;
    }

    /** Hit-test the cross-section handles. Returns the matching detail or null. */
    private CrossSectionDetail findHandleAt(float viewX, float viewY) {
        for (Map.Entry<CrossSectionDetail, RectF> entry : handleRects.entrySet()) {
            if (entry.getValue().contains(viewX, viewY)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** Whether a cross-section is drawn at this point, so a tap there would open it. */
    boolean isBodyShownAt(Coord2D surveyPoint) {
        return SketchPreferences.Toggle.SHOW_X_SECTIONS.isOn()
                && !GeneralPreferences.isLegacyCrossSectionsOn()
                && findBodyAt(surveyPoint) != null;
    }

    /**
     * Hit-test the body of a cross-section component (used to open the editor on tap). A component
     * occupies the area around its display centre; we pick whichever is closest within a tolerance.
     */
    CrossSectionDetail findBodyAt(Coord2D surveyPoint) {
        // Hit-test the detail's bounding box (legs + sub-sketch). If multiple overlap, pick the
        // one whose display centre is closest to the touch point.
        CrossSectionDetail best = null;
        float bestDistance = Float.MAX_VALUE;
        for (CrossSectionDetail detail : state.getSketch().getCrossSectionDetails()) {
            if (!detail.intersectsRectangle(surveyPoint, surveyPoint)) {
                continue;
            }
            float distance = detail.getDistanceFrom(surveyPoint);
            if (distance < bestDistance) {
                best = detail;
                bestDistance = distance;
            }
        }
        return best;
    }

    // ********** Drawing **********

    /**
     * Draw the cross-sections. Any whose station can't be found are dropped from the list, which is
     * the sketch's own.
     */
    void draw(Canvas canvas, List<CrossSectionDetail> crossSectionDetails, int alpha) {

        if (!SketchPreferences.Toggle.SHOW_X_SECTIONS.isOn()) {
            return;
        }

        connectorPaint.setAlpha(alpha);
        verticalHandlePaint.setAlpha(alpha);
        handleGripPaint.setAlpha(alpha);
        verticalBorderPaint.setAlpha(alpha);
        horizontalHandlePaint.setAlpha(alpha);
        horizontalBorderPaint.setAlpha(alpha);
        backgroundPaint.setAlpha(alpha);

        handleRects.clear();

        List<CrossSectionDetail> badXSections = new ArrayList<>();

        for (CrossSectionDetail detail : crossSectionDetails) {
            if (!drawCrossSection(canvas, detail, alpha)) {
                badXSections.add(detail);
            }
        }

        for (CrossSectionDetail badDetail : badXSections) {
            CrossSection xs = badDetail.getCrossSection();
            Station station = (xs != null) ? xs.getStation() : null;
            String name = (station != null) ? station.getName() : "Unknown";
            Log.e("Missing station details for cross section on station " + name + "; removing");
            crossSectionDetails.remove(badDetail);
        }
    }

    /**
     * Draw a single cross-section component: projected legs, sub-sketch, border, handle, connector,
     * and optional station label. Returns true on success, false if the detail has invalid data and
     * should be removed.
     */
    private boolean drawCrossSection(Canvas canvas, CrossSectionDetail originalDetail, int alpha) {

        // If this detail is being rotated, render a preview using the in-progress angle.
        CrossSectionDetail sectionDetail = originalDetail;
        if (originalDetail == beingRotated && previewAngle != null) {
            sectionDetail = originalDetail.withAngle(previewAngle);
        }

        // A detail being dragged is visualised at its new position via the drag delta.
        Coord2D dragDelta = (originalDetail == beingMoved) ? moveCurrentDelta : Coord2D.ORIGIN;

        if (!viewport.couldBeVisible(sectionDetail)) {
            return true;
        }

        CrossSection crossSection = sectionDetail.getCrossSection();
        if (crossSection == null) {
            return false;
        }

        Station station = crossSection.getStation();
        if (station == null) {
            return false;
        }

        Coord2D surveyStationLocation = state.getProjection().getStationMap().get(station);
        if (surveyStationLocation == null) {
            return false;
        }

        Coord2D centreOnSurvey = sectionDetail.getPosition().plus(dragDelta);
        Coord2D centreOnView = viewport.toView(centreOnSurvey);

        boolean isLegacy = GeneralPreferences.isLegacyCrossSectionsOn();

        // Legacy cross-sections have no frame, so nothing to fill
        RectF borderRect = null;
        if (!isLegacy) {
            borderRect = getBorderRect(sectionDetail, dragDelta);
            float cornerRadius = dpToPixels(BORDER_CORNER_RADIUS_DP);
            canvas.drawRoundRect(borderRect, cornerRadius, cornerRadius, backgroundPaint);
        }

        surveyDataRenderer.drawStationCross(canvas, centreOnView.x, centreOnView.y, alpha);

        if (SketchPreferences.Toggle.SHOW_STATION_LABELS.isOn()) {
            String description = station.getName() + " X";
            surveyDataRenderer.drawStationLabel(
                    canvas, description, centreOnView.x, centreOnView.y, alpha);
        }

        float xsScale = state.getSketch().getCrossSectionScale();
        insetViewport.setAsInsetOf(viewport, centreOnSurvey, xsScale);
        // Judge which splays are in the section's plane the same way the section editor does
        insetDataRenderer.drawLegs(
                canvas,
                crossSection.getProjection(),
                alpha,
                crossSection.getProjectionType()::isLegInPlane);

        Coord2D viewStationLocation = viewport.toView(surveyStationLocation);

        if (isLegacy) {
            // Legacy cross-sections: just the projected legs and a connector running to the centre,
            // with no editable frame, sub-sketch or drag handle.
            CanvasShapes.drawDashedLine(canvas, viewStationLocation, centreOnView, connectorPaint);
            return true;
        }

        // The frame fits the lines, not their decoration (e.g. the ticks on a wall), which would
        // otherwise spill out over the sketch around it
        float cornerRadius = dpToPixels(BORDER_CORNER_RADIUS_DP);
        Path frame = new Path();
        frame.addRoundRect(borderRect, cornerRadius, cornerRadius, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(frame);
        insetSketchRenderer.draw(canvas, sectionDetail.getSketch(), alpha);
        canvas.restore();

        boolean isHorizontal =
                sectionDetail.getCrossSection().getOrientation()
                        == CrossSection.Orientation.HORIZONTAL;
        Paint borderPaint = isHorizontal ? horizontalBorderPaint : verticalBorderPaint;
        Paint handlePaint = isHorizontal ? horizontalHandlePaint : verticalHandlePaint;

        canvas.drawRoundRect(borderRect, cornerRadius, cornerRadius, borderPaint);

        Coord2D connectorEnd =
                clipSegmentToRectBoundary(viewStationLocation, centreOnView, borderRect);
        if (connectorEnd != null) {
            CanvasShapes.drawDashedLine(canvas, viewStationLocation, connectorEnd, connectorPaint);
        }

        RectF handleRect = drawHandle(canvas, borderRect, handlePaint);
        handleRects.put(originalDetail, handleRect);

        if (host.getViewContext().canCreateHorizontalCrossSection()) {
            // Where both kinds can be made, mark each with the glyph from its menu item
            drawGlyph(
                    canvas,
                    borderRect,
                    sectionDetail.getCrossSection().getOrientation(),
                    handlePaint);
        }
        return true;
    }

    /**
     * Draw the orientation's glyph in a tab filling the bottom-right corner of the border, styled
     * like the handle bar.
     */
    private void drawGlyph(
            Canvas canvas,
            RectF borderRect,
            CrossSection.Orientation orientation,
            Paint handlePaint) {
        float size = dpToPixels(GLYPH_BOX_SIZE_DP);
        float cornerRadius = dpToPixels(BORDER_CORNER_RADIUS_DP);
        RectF box =
                new RectF(
                        borderRect.right - size,
                        borderRect.bottom - size,
                        borderRect.right,
                        borderRect.bottom);

        // Only the bottom-right corner is rounded, to follow the border
        Path boxPath = new Path();
        boxPath.addRoundRect(
                box,
                new float[] {
                    0f,
                    0f, // top-left
                    0f,
                    0f, // top-right
                    cornerRadius,
                    cornerRadius, // bottom-right
                    0f,
                    0f // bottom-left
                },
                Path.Direction.CW);
        canvas.drawPath(boxPath, handlePaint);

        glyphPaint.setColor(handleGripPaint.getColor());
        glyphPaint.setAlpha(handleGripPaint.getAlpha());
        float baseline = box.centerY() - (glyphPaint.ascent() + glyphPaint.descent()) / 2f;
        canvas.drawText(
                CrossSectionLabels.getGlyph(orientation), box.centerX(), baseline, glyphPaint);
    }

    /**
     * Draws a guide line from the station (in the main survey) to the touch point while rotating.
     */
    private void drawRotationGuideLine(Canvas canvas) {
        if (beingRotated == null || rotateFingerOnView == null) {
            return;
        }
        Coord2D pivotOnSurvey = getRotationPivot(beingRotated);
        if (pivotOnSurvey == null) {
            return;
        }
        Coord2D pivotOnView = viewport.toView(pivotOnSurvey);
        canvas.drawLine(
                pivotOnView.x,
                pivotOnView.y,
                rotateFingerOnView.x,
                rotateFingerOnView.y,
                connectorPaint);
    }

    /**
     * Clip the segment `from to to` to stop at the rectangle boundary. `to` is assumed to lie
     * inside the rect (it's the rect centre in practice). Returns the clipped endpoint, or `null`
     * if `from` is also inside (in which case there's no visible connector to draw).
     */
    private static Coord2D clipSegmentToRectBoundary(Coord2D from, Coord2D to, RectF rect) {
        if (rect.contains(from.x, from.y)) {
            return null;
        }
        float dx = to.x - from.x;
        float dy = to.y - from.y;
        // Find the largest t in [0,1] such that from + t*(to-from) is on the rect boundary while
        // moving from outside to inside.
        float tEnter = 0f;
        if (dx != 0) {
            float tLeft = (rect.left - from.x) / dx;
            float tRight = (rect.right - from.x) / dx;
            tEnter = Math.max(tEnter, Math.min(tLeft, tRight));
        }
        if (dy != 0) {
            float tTop = (rect.top - from.y) / dy;
            float tBottom = (rect.bottom - from.y) / dy;
            tEnter = Math.max(tEnter, Math.min(tTop, tBottom));
        }
        if (tEnter <= 0 || tEnter >= 1) {
            return to;
        }
        return new Coord2D(from.x + tEnter * dx, from.y + tEnter * dy);
    }

    /**
     * The frame around the cross-section's full extent (legs + sub-sketch), in view coords, with
     * room along the top for the handle.
     */
    private RectF getBorderRect(CrossSectionDetail sectionDetail, Coord2D dragDelta) {
        float xsScale = state.getSketch().getCrossSectionScale();
        Coord2D scaledTopLeft = sectionDetail.getTopLeftAtScale(xsScale).plus(dragDelta);
        Coord2D scaledBottomRight = sectionDetail.getBottomRightAtScale(xsScale).plus(dragDelta);
        Coord2D topLeft = viewport.toView(scaledTopLeft);
        Coord2D bottomRight = viewport.toView(scaledBottomRight);
        float contentWidth = bottomRight.x - topLeft.x;
        float contentHeight = bottomRight.y - topLeft.y;
        float scaledPadding = Math.min(contentWidth, contentHeight) * BORDER_PADDING_FRACTION;
        float padding =
                Math.max(
                        dpToPixels(BORDER_PADDING_MIN_DP),
                        Math.min(dpToPixels(BORDER_PADDING_MAX_DP), scaledPadding));
        float topPadding = padding + dpToPixels(HANDLE_WIDTH_DP);
        return new RectF(
                topLeft.x - padding,
                topLeft.y - topPadding,
                bottomRight.x + padding,
                bottomRight.y + padding);
    }

    /**
     * Draw a full-width handle bar along the top edge of the cross-section border. Returns the area
     * that counts as pressing it.
     */
    private RectF drawHandle(Canvas canvas, RectF borderRect, Paint handlePaint) {
        float handleHeight = dpToPixels(HANDLE_WIDTH_DP);
        float cornerRadius = dpToPixels(BORDER_CORNER_RADIUS_DP);

        RectF handleRect =
                new RectF(
                        borderRect.left,
                        borderRect.top,
                        borderRect.right,
                        borderRect.top + handleHeight);

        // Filled bar with rounded top corners, square bottom corners (flush with border).
        Path handlePath = new Path();
        handlePath.addRoundRect(
                handleRect,
                new float[] {
                    cornerRadius,
                    cornerRadius, // top-left
                    cornerRadius,
                    cornerRadius, // top-right
                    0f,
                    0f, // bottom-right
                    0f,
                    0f // bottom-left
                },
                Path.Direction.CW);
        canvas.drawPath(handlePath, handlePaint);

        // Grip ticks: three short vertical marks centred on the bar.
        float centreX = (handleRect.left + handleRect.right) / 2f;
        float centreY = (handleRect.top + handleRect.bottom) / 2f;
        float gripHalfLength = handleHeight * HANDLE_GRIP_LENGTH_FRACTION / 2f;
        float gripSpacing = dpToPixels(HANDLE_GRIP_SPACING_DP);
        float[] gripXs = {centreX - gripSpacing, centreX, centreX + gripSpacing};
        for (float gripX : gripXs) {
            canvas.drawLine(
                    gripX,
                    centreY - gripHalfLength,
                    gripX,
                    centreY + gripHalfLength,
                    handleGripPaint);
        }

        // Expand the rect a bit for friendlier hit-testing.
        float hitPadding = dpToPixels(HANDLE_HIT_PADDING_DP);
        return new RectF(
                handleRect.left - hitPadding,
                handleRect.top - hitPadding,
                handleRect.right + hitPadding,
                handleRect.bottom + hitPadding);
    }

    /** Draw the indicator at a station showing which way its cross-section faces, if it has one. */
    void drawIndicator(Canvas canvas, Station station, float x, float y, int alpha) {
        CrossSectionDetail crossSectionDetail = state.getSketch().getCrossSectionDetail(station);
        if (crossSectionDetail == null) {
            return;
        }

        indicatorPaint.setAlpha(alpha / 2);

        float indicatorWidth = (1 * viewport.getScale());
        CrossSectionIndicator indicator = getIndicator(crossSectionDetail, x, y, indicatorWidth);

        canvas.drawLine(
                indicator.getStartX(),
                indicator.getStartY(),
                indicator.getEndX(),
                indicator.getEndY(),
                indicatorPaint);

        Path path = new Path();
        path.moveTo(indicator.getArrowInnerX(), indicator.getArrowInnerY());
        path.lineTo(indicator.getArrowOuterX(), indicator.getArrowOuterY());
        path.lineTo(indicator.getArrowTipX(), indicator.getArrowTipY());
        path.lineTo(indicator.getArrowInnerX(), indicator.getArrowInnerY());

        canvas.drawPath(path, indicatorPaint);
    }

    /**
     * The indicator for a cross-section, facing the way it faces or the way it is being turned to.
     * On the plan that is its compass bearing. The elevation has no bearings, so a vertical section
     * faces left or right on the page and a horizontal one is looked at from above.
     */
    private CrossSectionIndicator getIndicator(
            CrossSectionDetail detail, float x, float y, float length) {
        CrossSection crossSection = detail.getCrossSection();
        if (!crossSection.isRotatable()) {
            return CrossSectionIndicator.lookingDown(x, y, length);
        }

        float angle =
                detail == beingRotated && previewAngle != null
                        ? previewAngle
                        : crossSection.getAngle();
        if (state.getProjectionType() == Projection2D.EXTENDED_ELEVATION) {
            boolean facingRight =
                    CrossSectioner.isFacingRightOnElevation(
                            state.getSurvey(), crossSection.getStation(), angle);
            return CrossSectionIndicator.onElevation(x, y, length, facingRight);
        }
        return CrossSectionIndicator.atAngle(x, y, length, angle);
    }
}

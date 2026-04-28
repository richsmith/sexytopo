package org.hwyl.sexytopo.control.graph;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RotateDrawable;
import android.text.Editable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import androidx.core.content.ContextCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SexyTopo;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.activity.CrossSectionActivity;
import org.hwyl.sexytopo.control.activity.GraphActivity;
import org.hwyl.sexytopo.control.components.DialogUtils;
import org.hwyl.sexytopo.control.util.CohenSutherlandAlgorithm;
import org.hwyl.sexytopo.control.util.CrossSectioner;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.SketchPreferences;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.BrushColour;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.sketch.CrossSectionDetail;
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
public class GraphView extends View {

    private final ScaleGestureDetector scaleGestureDetector;
    private final GestureDetector longPressDetector;

    // The offset of the viewing window (what can be seen on the screen) from the whole survey
    private Coord2D viewpointOffset = Coord2D.ORIGIN;

    // These variables are used in handling the dragging of the viewing window
    private Coord2D actionDownPointOnView = Coord2D.ORIGIN;
    private Coord2D actionDownViewpointOffset = Coord2D.ORIGIN;

    // ratio of metres on the survey to pixels on the view
    // zooming in increases this, zooming out decreases it
    private float surveyToViewScale = 60.0f;

    public static final float MIN_ZOOM = 0.1f;
    public static final float MAX_ZOOM = 500.0f;

    private static final int BOX_SIZE = 10; // every grid box is 10 units square

    public static final int SOLID_ALPHA = 0xff;
    public static final int FADED_ALPHA = 0xff / 5;
    private static final int STATION_STROKE_WIDTH_DP = 2;
    private static final int DASHED_LINE_INTERVAL_DP = 4;
    private static final int CROSS_SECTION_CONNECTOR_WIDTH_DP = 2;
    private static final int CROSS_SECTION_INDICATOR_WIDTH_DP = 2;
    private static final int CROSS_SECTION_BORDER_WIDTH_DP = 2;
    private static final int CROSS_SECTION_BORDER_PADDING_MIN_DP = 4;
    private static final int CROSS_SECTION_BORDER_PADDING_MAX_DP = 16;
    private static final float CROSS_SECTION_BORDER_PADDING_FRACTION = 0.05f;
    private static final float CROSS_SECTION_BORDER_CORNER_RADIUS_DP = 6.0f;
    private static final int CROSS_SECTION_HANDLE_WIDTH_DP = 8;
    private static final int CROSS_SECTION_HANDLE_GRIP_WIDTH_DP = 2;
    private static final float CROSS_SECTION_HANDLE_GRIP_SPACING_DP = 5f;
    private static final float CROSS_SECTION_HANDLE_GRIP_LENGTH_FRACTION = 0.45f;

    public static final int LEGEND_SIZE = 18;
    private static final int LEGEND_TICK_SIZE_DP = 5;
    private float legendTickSizePx;
    private float dashedLineIntervalPx;

    private static final float DELETE_DETAILS_WITHIN_N_DP = 5.0f;
    private static final float SELECTION_SENSITIVITY_DP = 25.0f;
    private static final float SNAP_TO_LINE_SENSITIVITY_DP = 25.0f;
    public static final float HOT_CORNER_DISTANCE_PROPORTION = 0.05f;
    private static final int STATION_LABEL_OFFSET_DP = 10;

    private float deleteDetailsWithinPx;
    private float selectionSensitivityPx;
    private float snapToLineSensitivityPx;
    private float stationLabelOffsetPx;

    private GraphActivity activity;

    protected Projection2D projectionType = Projection2D.PLAN;
    protected Survey survey;
    protected Space<Coord2D> projection;
    protected Sketch sketch;

    private Map<Survey, Space<Coord2D>> translatedConnectedSurveys = new HashMap<>();

    boolean surveyChanged;

    // cached preferences for performance
    private boolean isDarkModeActive = false;
    private boolean isTwoFingerModeActive = true;
    private boolean isHotCornersModeActive = true;

    // cached for performance
    protected Coord2D canvasBottomRight;
    protected Coord2D viewpointTopLeftOnSurvey;
    protected Coord2D viewpointBottomRightOnSurvey;
    private float surveyLength = 0;
    private float surveyHeight = 0;
    private Rect topLeftCorner;
    private Rect topRightCorner;
    private Rect bottomRightCorner;

    private Bitmap commentIcon, linkIcon;

    private float compassAzimuthDegrees = 0f;

    public SketchTool currentSketchTool = SketchTool.MOVE;
    // used to jump back to the previous tool when using one-use tools
    private SketchTool previousSketchTool = SketchTool.SELECT;

    // Flag to prevent double menu opening during this touch sequence
    private boolean menuShownInThisTouch = false;
    private Symbol currentSymbol = Symbol.getDefault();

    // a bit hacky but I can't think of a better way to do this
    private String stationNameBeingCrossSectioned = null;

    // State for dragging a cross-section component's handle on the plan.
    private CrossSectionDetail crossSectionBeingMoved = null;
    private Coord2D crossSectionMoveAnchorOnSurvey = Coord2D.ORIGIN;
    private Coord2D crossSectionMoveCurrentDelta = Coord2D.ORIGIN;

    // State for rotating a cross-section via drag-from-station gesture.
    private CrossSectionDetail crossSectionBeingRotated = null;
    private Coord2D crossSectionRotateFingerOnView = null;
    private Float crossSectionPreviewAngle = null;

    // Cached per-detail handle rectangles (in view coords) populated each draw; used for hit-test.
    private final Map<CrossSectionDetail, RectF> crossSectionHandleRects = new LinkedHashMap<>();

    // ********** Paints and other drawing variables **********

    protected final Paint stationPaint = new Paint();
    private final Paint iconPaint = new Paint();

    private final Paint legPaint = new Paint();
    private final Paint latestLegPaint = new Paint();
    private final Paint splayPaint = new Paint();

    private final Paint fadedLegPaint = new Paint();
    private final Paint fadedLatestLegPaint = new Paint();
    private final Paint fadedSplayPaint = new Paint();

    private final Paint drawPaint = new Paint();
    private final Paint labelPaint = new Paint();
    private final Paint highlightPaint = new Paint();
    private final Paint legendPaint = new Paint();
    private final Paint gridPaint = new Paint();
    private final Paint crossSectionConnectorPaint = new Paint();
    private final Paint crossSectionIndicatorPaint = new Paint();
    private final Paint crossSectionHandlePaint = new Paint();
    private final Paint crossSectionHandleGripPaint = new Paint();
    private final Paint crossSectionBorderPaint = new Paint();
    private final Paint hotCornersPaint = new Paint();

    private final Paint[] ANTI_ALIAS_PAINTS =
            new Paint[] {
                stationPaint,
                iconPaint,
                legendPaint,
                latestLegPaint,
                splayPaint,
                fadedLegPaint,
                fadedLatestLegPaint,
                fadedSplayPaint,
                drawPaint,
                labelPaint,
                legendPaint,
                crossSectionConnectorPaint,
                crossSectionIndicatorPaint,
                crossSectionHandlePaint,
                crossSectionHandleGripPaint,
                crossSectionBorderPaint
            };

    protected float stationCrossDiameterPx;

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        longPressDetector = new GestureDetector(context, new LongPressListener());
    }

    public void initialisePaint() {

        boolean applyAntiAlias = GeneralPreferences.isAntialiasingModeOn();
        for (Paint paint : ANTI_ALIAS_PAINTS) {
            if (paint.isAntiAlias() != applyAntiAlias) {
                paint.setAntiAlias(applyAntiAlias);
            }
        }

        int gridColour = ContextCompat.getColor(activity, R.color.grid);
        gridPaint.setColor(gridColour);

        int stationColour = ContextCompat.getColor(activity, R.color.station);
        stationPaint.setColor(stationColour);
        float stationStrokeWidthPx = dpToPixels(STATION_STROKE_WIDTH_DP);
        stationPaint.setStrokeWidth(stationStrokeWidthPx);
        int stationLabelFontSizeSp = GeneralPreferences.getStationLabelFontSizeSp();
        float stationLabelFontSizePixels = spToPixels(stationLabelFontSizeSp);
        stationPaint.setTextSize(stationLabelFontSizePixels);

        iconPaint.setColorFilter(new PorterDuffColorFilter(stationColour, PorterDuff.Mode.SRC_IN));

        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setStrokeWidth(stationStrokeWidthPx * 1.25f);
        int activeStationHighlightColor =
                androidx.core.content.ContextCompat.getColor(
                        getContext(), R.color.activeStationHighlight);
        highlightPaint.setColor(activeStationHighlightColor);

        // active legs/splays
        int legColour = ContextCompat.getColor(activity, R.color.leg);
        int latestLegColour = ContextCompat.getColor(activity, R.color.legLatest);
        int splayColour = ContextCompat.getColor(activity, R.color.splay);
        float legStrokeWidth = dpToPixels(GeneralPreferences.getLegStrokeWidthDp());
        legPaint.setStrokeWidth(legStrokeWidth);
        legPaint.setColor(legColour);

        latestLegPaint.setStrokeWidth(legStrokeWidth);
        latestLegPaint.setColor(latestLegColour);

        float splayStrokeWidth = dpToPixels(GeneralPreferences.getSplayStrokeWidthDp());
        splayPaint.setStrokeWidth(splayStrokeWidth);
        splayPaint.setColor(splayColour);

        // faded legs/splays
        fadedLegPaint.setStrokeWidth(legStrokeWidth);
        fadedLegPaint.setColor(legColour);
        fadedLegPaint.setAlpha(FADED_ALPHA);

        fadedLatestLegPaint.setStrokeWidth(legStrokeWidth);
        fadedLatestLegPaint.setColor(latestLegColour);
        fadedLatestLegPaint.setAlpha(FADED_ALPHA);

        fadedSplayPaint.setStrokeWidth(splayStrokeWidth);
        fadedSplayPaint.setColor(splayColour);
        fadedSplayPaint.setAlpha(FADED_ALPHA);

        int sketchDefaultColour = ContextCompat.getColor(activity, R.color.sketchDefault);
        drawPaint.setColor(sketchDefaultColour);
        drawPaint.setStrokeWidth(dpToPixels(GeneralPreferences.getSketchLineWidthDp()));
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        int legendColour = ContextCompat.getColor(activity, R.color.legend);
        legendPaint.setColor(legendColour);
        float legendSizeSp = GeneralPreferences.getLegendFontSizeSp();
        float legendSizePixels = spToPixels(legendSizeSp);
        float strokeWidthPixels = spToPixels(legendSizeSp * 0.1f);
        legendPaint.setStrokeWidth(strokeWidthPixels);
        legendPaint.setTextSize(legendSizePixels);

        int labelColour = ContextCompat.getColor(activity, R.color.station);
        labelPaint.setColor(labelColour);
        int labelSizeSp = GeneralPreferences.getLabelFontSizeSp();
        float labelSizePixels = spToPixels(labelSizeSp);
        labelPaint.setTextSize(labelSizePixels);

        crossSectionConnectorPaint.setColor(
                ContextCompat.getColor(activity, R.color.crossSectionConnection));
        crossSectionConnectorPaint.setStrokeWidth(dpToPixels(CROSS_SECTION_CONNECTOR_WIDTH_DP));
        crossSectionConnectorPaint.setStyle(Paint.Style.STROKE);

        crossSectionIndicatorPaint.setColor(
                ContextCompat.getColor(activity, R.color.crossSectionIndicator));
        crossSectionIndicatorPaint.setStrokeWidth(dpToPixels(CROSS_SECTION_INDICATOR_WIDTH_DP));
        crossSectionIndicatorPaint.setStyle(Paint.Style.FILL);

        int primaryColor = resolveThemeColor(activity, androidx.appcompat.R.attr.colorPrimary);

        crossSectionHandlePaint.setColor(primaryColor);
        crossSectionHandlePaint.setStyle(Paint.Style.FILL);
        crossSectionHandlePaint.setAntiAlias(true);

        crossSectionHandleGripPaint.setColor(
                resolveThemeColor(activity, com.google.android.material.R.attr.colorOnPrimary));
        crossSectionHandleGripPaint.setStrokeWidth(dpToPixels(CROSS_SECTION_HANDLE_GRIP_WIDTH_DP));
        crossSectionHandleGripPaint.setStyle(Paint.Style.STROKE);
        crossSectionHandleGripPaint.setStrokeCap(Paint.Cap.ROUND);

        crossSectionBorderPaint.setColor(primaryColor);
        crossSectionBorderPaint.setStrokeWidth(dpToPixels(CROSS_SECTION_BORDER_WIDTH_DP));
        crossSectionBorderPaint.setStyle(Paint.Style.STROKE);

        isTwoFingerModeActive = GeneralPreferences.isTwoFingerModeActive();

        isHotCornersModeActive = GeneralPreferences.isHotCornersModeActive();
        hotCornersPaint.setColor(ContextCompat.getColor(activity, R.color.hotCorner));
        hotCornersPaint.setAlpha(FADED_ALPHA);

        stationCrossDiameterPx = dpToPixels(GeneralPreferences.getStationCrossDiameterDp());
        legendTickSizePx = dpToPixels(LEGEND_TICK_SIZE_DP);
        dashedLineIntervalPx = dpToPixels(DASHED_LINE_INTERVAL_DP);
        deleteDetailsWithinPx = dpToPixels(DELETE_DETAILS_WITHIN_N_DP);
        selectionSensitivityPx = dpToPixels(SELECTION_SENSITIVITY_DP);
        snapToLineSensitivityPx = dpToPixels(SNAP_TO_LINE_SENSITIVITY_DP);
        stationLabelOffsetPx = dpToPixels(STATION_LABEL_OFFSET_DP);

        commentIcon = BitmapFactory.decodeResource(getResources(), R.drawable.speech_bubble);
        linkIcon = BitmapFactory.decodeResource(getResources(), R.drawable.link);
    }

    public void setActivity(GraphActivity graphActivity) {
        this.activity = graphActivity;
    }

    public void setSurvey(Survey survey) {
        if (survey != this.survey) {
            this.survey = survey;
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
        this.projectionType = projectionType;
    }

    public void setProjection(Space<Coord2D> projection) {
        this.projection = projection;
    }

    public void setSketch(Sketch sketch) {
        this.sketch = sketch;
    }

    public void setIsDarkModeActive(boolean isDarkModeActive) {
        this.isDarkModeActive = isDarkModeActive;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // Reset menu flag at start of new touch sequence
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            menuShownInThisTouch = false;
        }

        scaleGestureDetector.onTouchEvent(event);
        longPressDetector.onTouchEvent(event);

        if (scaleGestureDetector.isInProgress()) {
            return true;
        }

        if (currentSketchTool.isModal()
                && currentSketchTool != SketchTool.MOVE_CROSS_SECTION
                && event.getAction() == MotionEvent.ACTION_UP) {
            if (previousSketchTool != currentSketchTool) {
                setSketchTool(previousSketchTool);
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

        if (isCrossSectionMoveSelection(event)) {
            setSketchTool(SketchTool.MOVE_CROSS_SECTION);
        }

        if (handleCrossSectionBodyTap(event)) {
            return true;
        }

        switch (currentSketchTool) {
            case MOVE:
                return handleMove(event);
            case MODAL_MOVE:
                return handleMove(event);
            case DRAW:
                return handleDraw(event);
            case ERASE:
                return handleErase(event);
            case SYMBOL:
                return handleSymbol(event);
            case TEXT:
                return handleText(event);
            case SELECT:
                return handleSelect(event);
            case POSITION_CROSS_SECTION:
                return handlePositionCrossSection(event);
            case ROTATE_CROSS_SECTION:
                return handleRotateCrossSection(event);
            case MOVE_CROSS_SECTION:
                return handleMoveCrossSection(event);
        }
        return false;
    }

    private boolean isModalMoveSelection(MotionEvent event) {

        if (currentSketchTool == SketchTool.MODAL_MOVE) {
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

    private boolean isCrossSectionMoveSelection(MotionEvent event) {
        boolean currentlyActive = currentSketchTool == SketchTool.MOVE_CROSS_SECTION;
        boolean isDown = event.getAction() == MotionEvent.ACTION_DOWN;
        boolean hitAHandle = findCrossSectionHandleAt(event.getX(), event.getY()) != null;
        return !currentlyActive && isDown && hitAHandle;
    }

    protected Coord2D viewCoordsToSurveyCoords(final Coord2D coords) {
        // The more elegant way to do this is:
        // return coords.scale(1 / surveyToViewScale).plus(viewpointOffset);
        // ...but this method gets hit hard (profiled) so let's avoid creating intermediate objects:
        return new Coord2D(
                ((coords.x * (1 / surveyToViewScale)) + viewpointOffset.x),
                ((coords.y * (1 / surveyToViewScale)) + viewpointOffset.y));
    }

    // Warning: In tight loops during the draw phase we duplicate this logic to avoid
    //          creating too many Coord2D objects - be sure to mirror any updates in those places
    protected Coord2D surveyCoordsToViewCoords(final Coord2D coords) {
        // The more elegant way to do this is:
        // return coords.minus(viewpointOffset).scale(surveyToViewScale);
        // ...but this method gets hit hard (profiled) so let's avoid creating intermediate objects:
        return new Coord2D(
                ((coords.x - viewpointOffset.x) * surveyToViewScale),
                ((coords.y - viewpointOffset.y) * surveyToViewScale));
    }

    private boolean handleDraw(MotionEvent event) {

        Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        Coord2D surveyCoords = viewCoordsToSurveyCoords(touchPointOnView);

        boolean snapToLines = SketchPreferences.Toggle.SNAP_TO_LINES.isOn();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                actionDownPointOnView = touchPointOnView;
                Coord2D start = surveyCoords;
                if (snapToLines) {
                    Coord2D snappedStart = considerSnapToSketchLine(start);
                    if (snappedStart != null) {
                        start = snappedStart;
                    }
                }
                sketch.startNewPath(start);
                break;

            case MotionEvent.ACTION_MOVE:
                if (sketch.getActivePath() == null) {
                    // shouldn't be null, but just in case...
                    sketch.startNewPath(surveyCoords);
                } else {
                    sketch.getActivePath().lineTo(surveyCoords);
                }
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                if (touchPointOnView.equals(actionDownPointOnView)) { // handle dots
                    sketch.getActivePath().lineTo(surveyCoords);
                } else if (snapToLines) {
                    Coord2D snappedEnd = considerSnapToSketchLine(surveyCoords);
                    if (snappedEnd != null) {
                        sketch.getActivePath().lineTo(snappedEnd);
                        invalidate();
                    }
                }
                sketch.finishPath();
                break;

            default:
                return false;
        }

        return true;
    }

    private Coord2D considerSnapToSketchLine(Coord2D pointTouched) {
        float deltaInMetres = snapToLineSensitivityPx / surveyToViewScale;
        Coord2D closestPathEnd = sketch.findEligibleSnapPointWithin(pointTouched, deltaInMetres);
        return closestPathEnd; // null if nothing close found
    }

    private boolean handleMove(MotionEvent event) {

        Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                actionDownPointOnView = touchPointOnView;
                actionDownViewpointOffset = viewpointOffset;
                break;
            case MotionEvent.ACTION_MOVE:
                Coord2D surveyDelta =
                        touchPointOnView.minus(actionDownPointOnView).scale(1 / surveyToViewScale);
                viewpointOffset = actionDownViewpointOffset.minus(surveyDelta);
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
        Coord2D touchPointOnSurvey = viewCoordsToSurveyCoords(touchPointOnView);

        boolean deleteLineFragments = GeneralPreferences.isDeletePathFragmentsModeOn();

        float deleteToleranceInMetres = deleteDetailsWithinPx / surveyToViewScale;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                SketchDetail closestDetail =
                        sketch.findNearestVisibleDetailWithin(
                                touchPointOnSurvey, deleteToleranceInMetres, surveyToViewScale);

                if (closestDetail == null || closestDetail instanceof CrossSectionDetail) {
                    // missed, or hit a cross-section (use context menu to delete those)
                    return true;

                } else if (deleteLineFragments && closestDetail instanceof PathDetail) {
                    // you got part of the line
                    List<SketchDetail> fragments =
                            ((PathDetail) closestDetail)
                                    .getPathFragmentsOutsideRadius(
                                            touchPointOnSurvey, deleteToleranceInMetres);
                    sketch.deleteDetail(closestDetail, fragments);
                    invalidate();

                } else {
                    // bullseye!
                    sketch.deleteDetail(closestDetail);
                    invalidate();
                }
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                break;

            default:
                return false;
        }

        return true;
    }

    private boolean handleSymbol(MotionEvent event) {

        if (currentSymbol == Symbol.TEXT) {
            return handleText(event);
        }

        final Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        final Coord2D touchPointOnSurvey = viewCoordsToSurveyCoords(touchPointOnView);
        float startingSizeDp = GeneralPreferences.getSymbolStartingSizeDp();
        float startingSizePixels = SexyTopo.dpToPixels(startingSizeDp);
        float size = startingSizePixels / surveyToViewScale;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (currentSymbol.isDirectional()) {
                    actionDownPointOnView = touchPointOnView;
                } else {
                    sketch.addSymbolDetail(touchPointOnSurvey, currentSymbol, size, 0);
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                if (currentSymbol.isDirectional()) {
                    float angle =
                            Space2DUtils.getAngleBetween(actionDownPointOnView, touchPointOnView);
                    angle = Space2DUtils.adjustAngle(angle, -90);
                    Coord2D firstTouch = viewCoordsToSurveyCoords(actionDownPointOnView);
                    sketch.addSymbolDetail(firstTouch, currentSymbol, size, angle);
                    float distance =
                            Space2DUtils.getDistance(actionDownPointOnView, touchPointOnView);
                    if (distance < 5) {
                        activity.showSimpleToast(R.string.sketch_symbol_orientation_education);
                    }
                    invalidate();
                }
                return true;

            default:
                return false;
        }
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    private boolean handleText(MotionEvent event) {

        final Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        final Coord2D touchPointOnSurvey = viewCoordsToSurveyCoords(touchPointOnView);

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
                                    float startingSizePixels = spToPixels(startingSizeSp);
                                    float size = startingSizePixels / surveyToViewScale;
                                    sketch.addTextDetail(touchPointOnSurvey, text, size);
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

                } else if (newSelectedStation != survey.getActiveStation()) {
                    survey.setActiveStation(newSelectedStation);
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
        float selectionTolerance = selectionSensitivityPx / surveyToViewScale;
        Coord2D touchPointOnSurvey = viewCoordsToSurveyCoords(touchPointOnView);

        Station matchedStation =
                findNearestStationWithinDelta(projection, touchPointOnSurvey, selectionTolerance);
        return matchedStation; // this could be null if nothing is near
    }

    /**
     * @noinspection SameReturnValue
     */
    private boolean handlePositionCrossSection(MotionEvent event) {

        Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        Coord2D touchPointOnSurvey = viewCoordsToSurveyCoords(touchPointOnView);

        final Station station = survey.getStationByName(stationNameBeingCrossSectioned);
        stationNameBeingCrossSectioned = null;
        if (station == null) {
            return true;
        }

        CrossSection crossSection = CrossSectioner.section(survey, station);

        CrossSectionDetail detail = new CrossSectionDetail(crossSection, touchPointOnSurvey);
        sketch.addCrossSection(detail);

        setSketchTool(previousSketchTool);
        invalidate();

        return true;
    }

    /**
     * Drive the {@link SketchTool#MOVE_CROSS_SECTION} drag. Entry is set up in {@link
     * #onTouchEvent} when DOWN lands on a handle; this method is then dispatched for the rest of
     * the touch sequence.
     */
    private boolean handleMoveCrossSection(MotionEvent event) {
        Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                CrossSectionDetail detailOnHandle =
                        findCrossSectionHandleAt(touchPointOnView.x, touchPointOnView.y);
                if (detailOnHandle == null) {
                    setSketchTool(previousSketchTool);
                    return false;
                }
                crossSectionBeingMoved = detailOnHandle;
                crossSectionMoveAnchorOnSurvey = viewCoordsToSurveyCoords(touchPointOnView);
                crossSectionMoveCurrentDelta = Coord2D.ORIGIN;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (crossSectionBeingMoved != null) {
                    Coord2D current = viewCoordsToSurveyCoords(touchPointOnView);
                    crossSectionMoveCurrentDelta = current.minus(crossSectionMoveAnchorOnSurvey);
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (crossSectionBeingMoved != null) {
                    Coord2D current = viewCoordsToSurveyCoords(touchPointOnView);
                    Coord2D delta = current.minus(crossSectionMoveAnchorOnSurvey);
                    CrossSectionDetail moved = crossSectionBeingMoved;
                    crossSectionBeingMoved = null;
                    crossSectionMoveCurrentDelta = Coord2D.ORIGIN;
                    if (delta.x != 0 || delta.y != 0) {
                        sketch.replaceCrossSectionDetail(moved, moved.translate(delta));
                    }
                    invalidate();
                }
                setSketchTool(previousSketchTool);
                return true;

            case MotionEvent.ACTION_CANCEL:
                crossSectionBeingMoved = null;
                crossSectionMoveCurrentDelta = Coord2D.ORIGIN;
                setSketchTool(previousSketchTool);
                return true;

            default:
                return false;
        }
    }

    /** If user taps on a cross-section then open the editor. */
    private boolean handleCrossSectionBodyTap(MotionEvent event) {
        if (!SketchPreferences.Toggle.SHOW_X_SECTIONS.isOn()) {
            return false; // special case: can't tap on invisible X-sections
        }
        if (currentSketchTool == SketchTool.ERASE) {
            return false; // special case: user might be deleting the X-section
        }
        if (event.getAction() != MotionEvent.ACTION_UP) {
            return false; // ignore drags etc.
        }
        Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        if (!touchPointOnView.equals(actionDownPointOnView)) {
            return false;
        }
        Coord2D touchOnSurvey = viewCoordsToSurveyCoords(touchPointOnView);
        CrossSectionDetail tapped = findCrossSectionBodyAt(touchOnSurvey);
        if (tapped == null) {
            return false;
        }
        launchCrossSectionEditor(tapped);
        return true;
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
                this, station, survey, (int) event.getX(), (int) event.getY());
    }

    /**
     * @noinspection DuplicateBranchesInSwitch
     */
    protected ViewContext getViewContext() {
        if (projectionType == null) {
            return ViewContext.PLAN;
        }
        switch (projectionType) {
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

    private static float dpToPixels(float dp) {
        return Math.max(1f, SexyTopo.dpToPixels(dp));
    }

    private static int resolveThemeColor(Context context, int attr) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }

    public float spToPixels(float sp) {
        float scaledSizeInPixels =
                sp * getContext().getResources().getDisplayMetrics().scaledDensity;
        return scaledSizeInPixels;
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
        stationNameBeingCrossSectioned = station.getName();
        setSketchTool(SketchTool.POSITION_CROSS_SECTION);
        activity.showSimpleToast(R.string.sketch_position_cross_section_instruction);
    }

    public void handleRotateCrossSection(Station station) {
        CrossSectionDetail detail = sketch.getCrossSectionDetail(station);
        if (detail == null) {
            return;
        }
        crossSectionBeingRotated = detail;
        crossSectionRotateFingerOnView = null;
        crossSectionPreviewAngle = null;
        setSketchTool(SketchTool.ROTATE_CROSS_SECTION);
        activity.showSimpleToast(R.string.sketch_rotate_cross_section_instruction);
    }

    /**
     * Handle the rotation drag for a cross-section. The compass azimuth is computed from the
     * station's position in the main survey to the finger location.
     */
    private boolean handleRotateCrossSection(MotionEvent event) {
        if (crossSectionBeingRotated == null) {
            setSketchTool(previousSketchTool);
            return false;
        }

        Coord2D pivotOnSurvey = getRotationPivot(crossSectionBeingRotated);
        if (pivotOnSurvey == null) {
            setSketchTool(previousSketchTool);
            return false;
        }
        Coord2D fingerOnView = new Coord2D(event.getX(), event.getY());
        Coord2D fingerOnSurvey = viewCoordsToSurveyCoords(fingerOnView);
        float dx = fingerOnSurvey.x - pivotOnSurvey.x;
        float dy = fingerOnSurvey.y - pivotOnSurvey.y;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                crossSectionRotateFingerOnView = fingerOnView;
                if (dx != 0 || dy != 0) {
                    crossSectionPreviewAngle = toAzimuth(dx, dy);
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                if (dx != 0 || dy != 0) {
                    float newAngle = toAzimuth(dx, dy);
                    CrossSectionDetail rotated = crossSectionBeingRotated.withAngle(newAngle);
                    sketch.replaceCrossSectionDetail(crossSectionBeingRotated, rotated);
                }
                crossSectionBeingRotated = null;
                crossSectionRotateFingerOnView = null;
                crossSectionPreviewAngle = null;
                setSketchTool(previousSketchTool);
                invalidate();
                return true;

            case MotionEvent.ACTION_CANCEL:
                crossSectionBeingRotated = null;
                crossSectionRotateFingerOnView = null;
                crossSectionPreviewAngle = null;
                setSketchTool(previousSketchTool);
                invalidate();
                return true;
        }
        return true;
    }

    /** Pivot for rotation: the station's location in the main survey projection. */
    private Coord2D getRotationPivot(CrossSectionDetail detail) {
        Station station = detail.getCrossSection().getStation();
        if (station == null || projection == null) {
            return null;
        }
        return projection.getStationMap().get(station);
    }

    /** Compass azimuth (0 = North, 90 = East) for a vector in plan-view survey coords. */
    private static float toAzimuth(float dx, float dy) {
        // Plan view: +x = East, -y = North. atan2(x, -y) gives bearing clockwise from North.
        double radians = Math.atan2(dx, -dy);
        float degrees = (float) Math.toDegrees(radians);
        return ((degrees % 360) + 360) % 360;
    }

    public void launchCrossSectionEditor(CrossSectionDetail detail) {
        Station station = detail.getCrossSection().getStation();
        if (station == null) {
            return;
        }
        android.content.Intent intent =
                new android.content.Intent(getContext(), CrossSectionActivity.class);
        intent.putExtra(CrossSectionActivity.EXTRA_STATION_NAME, station.getName());
        getContext().startActivity(intent);
    }

    private void broadcastSurveyUpdated() {
        SurveyManager.getInstance(getContext().getApplicationContext()).broadcastSurveyUpdated();
    }

    protected void updateViewBounds() {
        canvasBottomRight = new Coord2D(getWidth(), getHeight());
        viewpointTopLeftOnSurvey = viewCoordsToSurveyCoords(Coord2D.ORIGIN);
        viewpointBottomRightOnSurvey = viewCoordsToSurveyCoords(canvasBottomRight);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        updateViewBounds();

        drawGrid(canvas);
        drawConnectedSurveys(canvas, projection, FADED_ALPHA);
        drawSurvey(canvas, survey, projection, SOLID_ALPHA);
        drawLegend(canvas);
        drawCompass(canvas);
        drawHotCorners(canvas);
        drawDebuggingInfo(canvas);
    }

    protected void drawSurvey(Canvas canvas, Survey survey, Space<Coord2D> projection, int alpha) {
        drawSketch(canvas, activity.getSketch(survey), alpha);
        drawCrossSections(canvas, sketch.getCrossSectionDetails(), alpha);
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
                                activity.getProjectionType(), survey, projection);
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
        for (Set<SurveyConnection> connectionSet : survey.getConnectedSurveys().values()) {
            for (SurveyConnection connection : connectionSet) {
                flatSet.add(connection.otherSurvey);
            }
        }
        return flatSet;
    }

    protected void drawGrid(Canvas canvas) {

        if (!SketchPreferences.Toggle.SHOW_GRID.isOn()) {
            return;
        }

        int tickSizeInMetres = getMinorGridBoxSize();
        int numberTicksJustBeforeViewpointOffsetX = (int) (viewpointOffset.x / tickSizeInMetres);

        for (int n = numberTicksJustBeforeViewpointOffsetX; true; n++) {
            float xSurvey = n * tickSizeInMetres;
            int xView = (int) ((xSurvey - viewpointOffset.x) * surveyToViewScale);
            gridPaint.setStrokeWidth(n % BOX_SIZE == 0 ? 3 : 1);
            canvas.drawLine(xView, 0, xView, getHeight(), gridPaint);
            if (xView >= getWidth()) {
                break;
            }
        }

        int numberTicksJustBeforeViewpointOffsetY = (int) (viewpointOffset.y / tickSizeInMetres);

        for (int n = numberTicksJustBeforeViewpointOffsetY; true; n++) {
            float ySurvey = n * tickSizeInMetres;
            int yView = (int) ((ySurvey - viewpointOffset.y) * surveyToViewScale);
            gridPaint.setStrokeWidth(n % BOX_SIZE == 0 ? 3 : 1);
            canvas.drawLine(0, yView, getWidth(), yView, gridPaint);
            if (yView >= getHeight()) {
                break;
            }
        }
    }

    public int getMinorGridBoxSize() {

        if (surveyToViewScale > 15) {
            return 1;
        } else if (surveyToViewScale > 2) {
            return 10;
        } else {
            return 100;
        }
    }

    private void drawSurveyData(Survey survey, Canvas canvas, Space<Coord2D> space, int alpha) {
        drawLegs(canvas, space, alpha);
        drawStations(survey, canvas, space, alpha);
    }

    private void drawCrossSections(
            Canvas canvas, List<CrossSectionDetail> crossSectionDetails, int alpha) {

        if (!SketchPreferences.Toggle.SHOW_X_SECTIONS.isOn()) {
            return;
        }

        crossSectionConnectorPaint.setAlpha(alpha);
        crossSectionHandlePaint.setAlpha(alpha);
        crossSectionHandleGripPaint.setAlpha(alpha);
        crossSectionBorderPaint.setAlpha(alpha);

        crossSectionHandleRects.clear();

        List<CrossSectionDetail> badXSections = new ArrayList<>();

        for (CrossSectionDetail detail : crossSectionDetails) {
            if (!drawCrossSection(canvas, detail, alpha)) {
                badXSections.add(detail);
            }
        }

        drawRotationGuideLine(canvas);

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
        if (originalDetail == crossSectionBeingRotated && crossSectionPreviewAngle != null) {
            sectionDetail = originalDetail.withAngle(crossSectionPreviewAngle);
        }

        // A detail being dragged is visualised at its new position via the drag delta.
        Coord2D dragDelta =
                (originalDetail == crossSectionBeingMoved)
                        ? crossSectionMoveCurrentDelta
                        : Coord2D.ORIGIN;

        if (!couldBeVisible(sectionDetail)) {
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

        Coord2D surveyStationLocation = this.projection.getStationMap().get(station);
        if (surveyStationLocation == null) {
            return false;
        }

        Coord2D centreOnSurvey = sectionDetail.getPosition().plus(dragDelta);
        Coord2D centreOnView = surveyCoordsToViewCoords(centreOnSurvey);
        drawStationCross(
                canvas,
                stationPaint,
                centreOnView.x,
                centreOnView.y,
                Math.round(stationCrossDiameterPx),
                alpha);

        if (SketchPreferences.Toggle.SHOW_STATION_LABELS.isOn()) {
            String description = station.getName() + " X";
            stationPaint.setAlpha(alpha);
            canvas.drawText(description, centreOnView.x, centreOnView.y, stationPaint);
        }

        Space<Coord2D> rawProjection = crossSection.getProjection();
        float xsScale = survey.getPlanSketch().getCrossSectionScale();
        Space<Coord2D> scaledProjection = rawProjection.scale(xsScale);
        Space<Coord2D> sectionProjection = Space2DUtils.translate(scaledProjection, centreOnSurvey);
        drawLegs(canvas, sectionProjection, alpha);

        drawCrossSectionSubSketch(canvas, sectionDetail, centreOnSurvey, alpha);

        RectF borderRect = drawCrossSectionBorder(canvas, sectionDetail, dragDelta);

        Coord2D viewStationLocation = surveyCoordsToViewCoords(surveyStationLocation);
        Coord2D connectorEnd =
                clipSegmentToRectBoundary(viewStationLocation, centreOnView, borderRect);
        if (connectorEnd != null) {
            drawDashedLine(
                    canvas,
                    viewStationLocation,
                    connectorEnd,
                    dashedLineIntervalPx,
                    crossSectionConnectorPaint);
        }

        RectF handleRect = drawCrossSectionHandle(canvas, borderRect);
        crossSectionHandleRects.put(originalDetail, handleRect);
        return true;
    }

    /**
     * Draws a guide line from the station (in the main survey) to the touch point while rotating.
     */
    private void drawRotationGuideLine(Canvas canvas) {
        if (crossSectionBeingRotated == null || crossSectionRotateFingerOnView == null) {
            return;
        }
        Coord2D pivotOnSurvey = getRotationPivot(crossSectionBeingRotated);
        if (pivotOnSurvey == null) {
            return;
        }
        Coord2D pivotOnView = surveyCoordsToViewCoords(pivotOnSurvey);
        canvas.drawLine(
                pivotOnView.x,
                pivotOnView.y,
                crossSectionRotateFingerOnView.x,
                crossSectionRotateFingerOnView.y,
                crossSectionConnectorPaint);
    }

    /**
     * Draw the user-drawn overlay (paths only) for a cross-section on the plan view, translated
     * from station-relative coords to the component's current display centre.
     */
    private void drawCrossSectionSubSketch(
            Canvas canvas, CrossSectionDetail sectionDetail, Coord2D centreOnSurvey, int alpha) {

        float xsScale = survey.getPlanSketch().getCrossSectionScale();
        Sketch subSketch = sectionDetail.getSketch().scale(xsScale).translate(centreOnSurvey);
        drawSketch(canvas, subSketch, alpha);
    }

    /**
     * Clip the segment `from to to` to stop at the rectangle boundary. `to` is assumed to lie
     * inside the rect (it's the rect centre in practice). Returns the clipped endpoint, or `null`
     * if `from` is also inside (in which case there's no visible connector to draw).
     */
    private Coord2D clipSegmentToRectBoundary(Coord2D from, Coord2D to, RectF rect) {
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

    /** Draw a rectangular border around the cross-section's full extent (legs + sub-sketch). */
    private RectF drawCrossSectionBorder(
            Canvas canvas, CrossSectionDetail sectionDetail, Coord2D dragDelta) {
        float xsScale = survey.getPlanSketch().getCrossSectionScale();
        Coord2D centre = sectionDetail.getPosition().plus(dragDelta);
        Coord2D origin = sectionDetail.getPosition();
        Coord2D scaledTopLeft =
                centre.plus(sectionDetail.getTopLeft().minus(origin).scale(xsScale));
        Coord2D scaledBottomRight =
                centre.plus(sectionDetail.getBottomRight().minus(origin).scale(xsScale));
        Coord2D topLeft = surveyCoordsToViewCoords(scaledTopLeft);
        Coord2D bottomRight = surveyCoordsToViewCoords(scaledBottomRight);
        float contentWidth = bottomRight.x - topLeft.x;
        float contentHeight = bottomRight.y - topLeft.y;
        float scaledPadding =
                Math.min(contentWidth, contentHeight) * CROSS_SECTION_BORDER_PADDING_FRACTION;
        float padding =
                Math.max(
                        dpToPixels(CROSS_SECTION_BORDER_PADDING_MIN_DP),
                        Math.min(dpToPixels(CROSS_SECTION_BORDER_PADDING_MAX_DP), scaledPadding));
        float topPadding = padding + dpToPixels(CROSS_SECTION_HANDLE_WIDTH_DP);
        RectF rect =
                new RectF(
                        topLeft.x - padding,
                        topLeft.y - topPadding,
                        bottomRight.x + padding,
                        bottomRight.y + padding);
        float cornerRadius = dpToPixels(CROSS_SECTION_BORDER_CORNER_RADIUS_DP);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, crossSectionBorderPaint);
        return rect;
    }

    /** Draw a full-width handle bar along the top edge of the cross-section border. */
    private RectF drawCrossSectionHandle(Canvas canvas, RectF borderRect) {
        float handleHeight = dpToPixels(CROSS_SECTION_HANDLE_WIDTH_DP);
        float cornerRadius = dpToPixels(CROSS_SECTION_BORDER_CORNER_RADIUS_DP);

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
        canvas.drawPath(handlePath, crossSectionHandlePaint);

        // Grip ticks: three short vertical marks centred on the bar.
        float centreX = (handleRect.left + handleRect.right) / 2f;
        float centreY = (handleRect.top + handleRect.bottom) / 2f;
        float gripHalfLength = handleHeight * CROSS_SECTION_HANDLE_GRIP_LENGTH_FRACTION / 2f;
        float gripSpacing = dpToPixels(CROSS_SECTION_HANDLE_GRIP_SPACING_DP);
        float[] gripXs = {centreX - gripSpacing, centreX, centreX + gripSpacing};
        for (float gripX : gripXs) {
            canvas.drawLine(
                    gripX,
                    centreY - gripHalfLength,
                    gripX,
                    centreY + gripHalfLength,
                    crossSectionHandleGripPaint);
        }

        // Expand the rect a bit for friendlier hit-testing.
        float hitPadding = dpToPixels(8);
        return new RectF(
                handleRect.left - hitPadding,
                handleRect.top - hitPadding,
                handleRect.right + hitPadding,
                handleRect.bottom + hitPadding);
    }

    /** Hit-test the cross-section handles. Returns the matching detail or null. */
    private CrossSectionDetail findCrossSectionHandleAt(float viewX, float viewY) {
        for (Map.Entry<CrossSectionDetail, RectF> entry : crossSectionHandleRects.entrySet()) {
            if (entry.getValue().contains(viewX, viewY)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Hit-test the body of a cross-section component (used to open the editor on tap). A component
     * occupies the area around its display centre; we pick whichever is closest within a tolerance.
     */
    private CrossSectionDetail findCrossSectionBodyAt(Coord2D surveyPoint) {
        // Hit-test the detail's bounding box (legs + sub-sketch). If multiple overlap, pick the
        // one whose display centre is closest to the touch point.
        CrossSectionDetail best = null;
        float bestDistance = Float.MAX_VALUE;
        for (CrossSectionDetail detail : sketch.getCrossSectionDetails()) {
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

    protected void drawLegs(Canvas canvas, Space<Coord2D> space, int baseAlpha) {

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
            Coord2D start = surveyCoordsToViewCoords(line.getStart());
            Coord2D end = surveyCoordsToViewCoords(line.getEnd());

            if (!isLineOnCanvas(start, end)) {
                continue;
            }

            boolean fade =
                    baseAlpha == FADED_ALPHA || (fadingNonActive && !isAttachedToActive(leg));

            Paint paint;
            if (highlightLatestLeg && survey.getMostRecentLeg() == leg) {
                paint = fade ? fadedLatestLegPaint : latestLegPaint;
            } else if (!leg.hasDestination()) {
                paint = fade ? fadedSplayPaint : splayPaint;
            } else {
                paint = fade ? fadedLegPaint : legPaint;
            }

            if (projectionType.isLegInPlane(leg)) {
                canvas.drawLine(start.x, start.y, end.x, end.y, paint);
            } else {
                drawDashedLine(canvas, start, end, dashedLineIntervalPx, paint);
            }
        }
    }

    private boolean isAttachedToActive(Leg leg) {
        return survey.getActiveStation().getOnwardLegs().contains(leg);
    }

    private boolean isLineOnCanvas(Coord2D start, Coord2D end) {
        return !CohenSutherlandAlgorithm.whollyOutside(
                start, end, Coord2D.ORIGIN, canvasBottomRight);
    }

    private void drawStations(Survey survey, Canvas canvas, Space<Coord2D> space, int baseAlpha) {

        boolean fadingNonActive = SketchPreferences.Toggle.FADE_NON_ACTIVE.isOn();
        boolean showStationLabels = SketchPreferences.Toggle.SHOW_STATION_LABELS.isOn();

        if (fadingNonActive) {
            baseAlpha = FADED_ALPHA;
        }

        int alpha = baseAlpha;
        stationPaint.setAlpha(alpha);

        for (Map.Entry<Station, Coord2D> entry : space.getStationMap().entrySet()) {
            Station station = entry.getKey();

            if (fadingNonActive && (station == survey.getActiveStation())) {
                alpha = SOLID_ALPHA;
                // setting alpha is measured as a relatively expensive call, so we change this as
                // little as possible
                stationPaint.setAlpha(alpha);
            }

            Coord2D translatedStation = surveyCoordsToViewCoords(entry.getValue());

            int x = (int) (translatedStation.x);
            int y = (int) (translatedStation.y);

            int stationCrossDiameterIntPx = Math.round(stationCrossDiameterPx);
            drawStationCross(canvas, stationPaint, x, y, stationCrossDiameterIntPx, alpha);

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
                canvas.drawText(name, nextX, y + stationLabelOffsetPx, stationPaint);
                nextX += Math.round(stationPaint.measureText(name)) + spacing;
            }

            List<Bitmap> icons = new ArrayList<>();
            if (station.hasComment()) {
                icons.add(commentIcon);
            }
            if (survey.hasLinkedSurveys(station)) {
                icons.add(linkIcon);
            }

            for (Bitmap icon : icons) {
                int yTop = y - stationCrossDiameterIntPx / 2;
                Rect rect =
                        new Rect(
                                nextX,
                                yTop,
                                nextX + stationCrossDiameterIntPx,
                                yTop + stationCrossDiameterIntPx);
                canvas.drawBitmap(icon, null, rect, iconPaint);
                nextX += stationCrossDiameterIntPx + spacing;
            }

            CrossSectionDetail crossSectionDetail = sketch.getCrossSectionDetail(station);
            if (crossSectionDetail != null) {
                drawCrossSectionIndicator(canvas, crossSectionDetail, x, y, alpha);
            }

            if (fadingNonActive && (station == survey.getActiveStation())) {
                alpha = baseAlpha;
                stationPaint.setAlpha(alpha);
            }
        }
    }

    private void drawCrossSectionIndicator(
            Canvas canvas, CrossSectionDetail crossSectionDetail, float x, float y, int alpha) {

        crossSectionIndicatorPaint.setAlpha(alpha / 2);
        CrossSection crossSection = crossSectionDetail.getCrossSection();

        float activeAngle =
                crossSectionPreviewAngle == null
                        ? crossSection.getAngle()
                        : crossSectionPreviewAngle;
        float angle = (float) Math.toRadians(activeAngle);
        float indicatorWidth = (1 * surveyToViewScale);
        float startX = x - ((indicatorWidth / 2) * (float) Math.cos(angle));
        float startY = y - ((indicatorWidth / 2) * (float) Math.sin(angle));
        float endX = x + ((indicatorWidth / 2) * (float) Math.cos(angle));
        float endY = y + ((indicatorWidth / 2) * (float) Math.sin(angle));

        canvas.drawLine(startX, startY, endX, endY, crossSectionIndicatorPaint);

        float lineLength =
                Space2DUtils.getDistance(new Coord2D(startX, startY), new Coord2D(endX, endY));
        float arrowLength = lineLength * 0.4f;
        float arrowOuterCornerX = startX;
        float arrowOuterCornerY = startY;
        float arrowInnerCornerX = startX + ((lineLength * 0.05f) * (float) Math.cos(angle));
        float arrowInnerCornerY = startY + ((lineLength * 0.05f) * (float) Math.sin(angle));
        float arrowAngle = (float) Math.toRadians(Space2DUtils.adjustAngle(activeAngle, -90));
        float arrowTipX = startX + (arrowLength * (float) Math.cos(arrowAngle));
        float arrowTipY = startY + (arrowLength * (float) Math.sin(arrowAngle));

        Path path = new Path();
        path.moveTo(arrowInnerCornerX, arrowInnerCornerY);
        path.lineTo(arrowOuterCornerX, arrowOuterCornerY);
        path.lineTo(arrowTipX, arrowTipY);
        path.lineTo(arrowInnerCornerX, arrowInnerCornerY);

        canvas.drawPath(path, crossSectionIndicatorPaint);
    }

    private void highlightActiveStation(Canvas canvas, float x, float y) {

        float diameter = stationCrossDiameterPx * 1.1f;
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
        canvas.drawPath(topLeft, highlightPaint);

        // top-right corner
        Path topRight = new Path();
        topRight.moveTo(innerRight, topY);
        topRight.lineTo(rightX, topY);
        topRight.lineTo(rightX, innerTop);
        canvas.drawPath(topRight, highlightPaint);

        // bottom-left corner
        Path bottomLeft = new Path();
        bottomLeft.moveTo(leftX, innerBottom);
        bottomLeft.lineTo(leftX, bottomY);
        bottomLeft.lineTo(innerLeft, bottomY);
        canvas.drawPath(bottomLeft, highlightPaint);

        // bottom-right corner
        Path bottomRight = new Path();
        bottomRight.moveTo(rightX, innerBottom);
        bottomRight.lineTo(rightX, bottomY);
        bottomRight.lineTo(innerRight, bottomY);
        canvas.drawPath(bottomRight, highlightPaint);
    }

    protected void drawStationCross(
            Canvas canvas, Paint paint, float x, float y, int crossDiameter, int alpha) {
        paint.setAlpha(alpha);
        float halfCross = crossDiameter / 2f;
        canvas.drawLine(x, y - halfCross, x, y + halfCross, paint);
        canvas.drawLine(x - halfCross, y, x + halfCross, y, paint);
    }

    protected void drawSketch(Canvas canvas, Sketch sketch, int alpha) {

        if (!SketchPreferences.Toggle.SHOW_SKETCH.isOn()) {
            return;
        }

        Colour lastColour = Colour.BLACK;

        drawPaint.setColor(lastColour.intValue);
        drawPaint.setAlpha(alpha);

        boolean isDebugMode = activity.isDebugMode();

        for (PathDetail pathDetail : sketch.getPathDetails()) {

            if (!couldBeVisible(pathDetail)) {
                continue;
            }

            // Avoiding constantly updating the paint colour saves approx. 10% of sketch draw time.
            // Ideally getPathDetails() would return the paths in colour order but HashSets
            // are unordered collections
            Colour drawColour = pathDetail.getDrawColour(isDarkModeActive);
            if (drawColour != lastColour) {
                lastColour = drawColour;
                drawPaint.setColor(lastColour.intValue);
            }

            List<Coord2D> path = pathDetail.getPath();

            int lineIndex = 0;
            float fromX = -1, fromY = -1;
            float[] lines = new float[path.size() * 4];

            // This loop is the slowest part of the draw phase. Pulling out the calculations from
            // within surveyCoordsToViewCoords saves a not insignificant amount of time by not
            // constructing many thousands of Coord2D objects (approx. 10% of sketch draw time)
            for (Coord2D point : path) {
                if (fromX == -1) {
                    // from = surveyCoordsToViewCoords(point);
                    fromX = (point.x - viewpointOffset.x) * surveyToViewScale;
                    fromY = (point.y - viewpointOffset.y) * surveyToViewScale;

                    if (isDebugMode) {
                        canvas.drawCircle(fromX, fromY, 3, drawPaint);
                    }
                } else {
                    // Coord2D to = surveyCoordsToViewCoords(point);
                    float toX = (point.x - viewpointOffset.x) * surveyToViewScale;
                    float toY = (point.y - viewpointOffset.y) * surveyToViewScale;

                    lines[lineIndex++] = fromX;
                    lines[lineIndex++] = fromY;
                    lines[lineIndex++] = toX;
                    lines[lineIndex++] = toY;

                    if (isDebugMode) {
                        canvas.drawCircle(toX, toY, 3, drawPaint);
                    }

                    fromX = toX;
                    fromY = toY;
                }
            }

            canvas.drawLines(lines, drawPaint);
        }

        labelPaint.setAlpha(alpha);

        for (TextDetail textDetail : sketch.getTextDetails()) {
            Coord2D location = surveyCoordsToViewCoords(textDetail.getPosition());
            float x = location.x, y = location.y;
            String text = textDetail.getText();
            setDrawColour(labelPaint, textDetail);
            labelPaint.setTextSize(textDetail.getSize() * surveyToViewScale);
            for (String line : text.split("\n")) {
                canvas.drawText(line, x, y, labelPaint);
                y += labelPaint.descent() - labelPaint.ascent();
            }
        }

        for (SymbolDetail symbolDetail : sketch.getSymbolDetails()) {
            if (!couldBeVisible(symbolDetail)) {
                continue;
            }
            Coord2D location = surveyCoordsToViewCoords(symbolDetail.getPosition());

            Symbol symbol = symbolDetail.getSymbol();

            int size = Math.round(symbolDetail.getSize() * surveyToViewScale);
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

    private void setDrawColour(Paint paint, SketchDetail sketchDetail) {
        Colour colour = sketchDetail.getDrawColour(isDarkModeActive);
        paint.setColor(colour.intValue);
    }

    protected void drawLegend(Canvas canvas) {

        String surveyLabel =
                survey.getName()
                        + " L"
                        + TextTools.formatTo0dpWithComma(surveyLength)
                        + " V"
                        + TextTools.formatTo0dpWithComma(surveyHeight);

        float legendSize = legendPaint.getTextSize();
        float offsetX = legendSize * 1.25f;
        float offsetY = legendSize * 1.25f;
        float y = getHeight() - offsetY;
        float x = offsetX;
        canvas.drawText(surveyLabel, x, y, legendPaint);

        int minorGridSize = getMinorGridBoxSize();
        float scaleWidth = surveyToViewScale * minorGridSize;
        float scaleOffsetY = offsetY * 2;
        float scaleY = getHeight() - scaleOffsetY;
        legendPaint.setStyle(Paint.Style.STROKE);
        Path scalePath = new Path();
        scalePath.moveTo(x, scaleY - legendTickSizePx);
        scalePath.lineTo(x, scaleY);
        scalePath.lineTo(x + scaleWidth, scaleY);
        scalePath.lineTo(x + scaleWidth, scaleY - legendTickSizePx);
        canvas.drawPath(scalePath, legendPaint);
        legendPaint.setStyle(Paint.Style.FILL);
        String scaleLabel = minorGridSize + "m";
        canvas.drawText(scaleLabel, x + scaleWidth + 0.3f * legendSize, scaleY, legendPaint);
    }

    private void drawCompass(Canvas canvas) {
        if (!SketchPreferences.Toggle.SHOW_COMPASS.isOn() || projectionType != Projection2D.PLAN) {
            return;
        }

        float textSize = legendPaint.getTextSize();
        Paint.FontMetrics metrics = legendPaint.getFontMetrics();
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

        legendPaint.setStyle(Paint.Style.STROKE);
        Path arrowPath = new Path();
        arrowPath.moveTo(cx - arrowHeadSize, tipY + arrowHeadSize);
        arrowPath.lineTo(cx, tipY);
        arrowPath.lineTo(cx + arrowHeadSize, tipY + arrowHeadSize);
        arrowPath.moveTo(cx, tipY);
        arrowPath.lineTo(cx, tailY);
        canvas.drawPath(arrowPath, legendPaint);
        legendPaint.setStyle(Paint.Style.FILL);
        canvas.drawText("N", cx - textSize * 0.35f, tipY - textSize * 0.2f, legendPaint);

        canvas.restore();
    }

    private void drawHotCorners(Canvas canvas) {

        if (!isHotCornersModeActive) {
            return;
        }

        if (currentSketchTool == SketchTool.MODAL_MOVE) {
            hotCornersPaint.setColor(ContextCompat.getColor(getContext(), R.color.hotCornerActive));
            hotCornersPaint.setAlpha(FADED_ALPHA);
        }

        final int side = (int) (HOT_CORNER_DISTANCE_PROPORTION * Math.min(getWidth(), getHeight()));
        topLeftCorner = new Rect(0, 0, side, side);
        topRightCorner = new Rect(getWidth() - side, 0, getWidth(), side);
        bottomRightCorner =
                new Rect(getWidth() - side, getHeight() - side, getWidth(), getHeight());

        canvas.drawRect(topLeftCorner, hotCornersPaint);
        canvas.drawRect(topRightCorner, hotCornersPaint);
        canvas.drawRect(bottomRightCorner, hotCornersPaint);

        if (currentSketchTool == SketchTool.MODAL_MOVE) {
            hotCornersPaint.setColor(ContextCompat.getColor(getContext(), R.color.hotCorner));
            hotCornersPaint.setAlpha(FADED_ALPHA);
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
                        + TextTools.formatTo2dp(surveyToViewScale)
                        + " 1/s2v="
                        + TextTools.formatTo2dp(1 / surveyToViewScale)
                        +
                        // " 1/log=" + TextTools.formatTo2dp(1 / Math.log(surveyToViewScale)) +
                        // " 1/log10=" + TextTools.formatTo2dp(1 / Math.log10(surveyToViewScale)) +
                        "\n log (1/s2v) ="
                        + TextTools.formatTo2dp(Math.log(1 / surveyToViewScale))
                        + "\n log10 (1/s2v) ="
                        + TextTools.formatTo2dp(Math.log10(1 / surveyToViewScale));

        canvas.drawText(label, offsetX, offsetY, legendPaint);
    }

    /**
     * Returns true if a sketch detail could be visible in the current view — i.e. its bounding box
     * intersects the screen area and it is large enough to occupy at least one pixel at the current
     * zoom level. Details that fail either check can be skipped without drawing.
     */
    private boolean couldBeVisible(SketchDetail sketchDetail) {
        boolean possiblyOnScreen =
                sketchDetail.intersectsRectangle(
                        viewpointTopLeftOnSurvey, viewpointBottomRightOnSurvey);
        boolean bigEnough = sketchDetail.couldBeVisibleAtScale(surveyToViewScale);

        return (possiblyOnScreen && bigEnough);
    }

    public void centreViewOnActiveStation() {
        centreViewOnStation(survey.getActiveStation());
    }

    public void centreViewOnStation(Station station) {
        Coord2D activeStationCoord = projection.getStationMap().get(station);

        // not sure how this could be null, but at least one null pointer has been reported
        if (activeStationCoord == null) {
            activeStationCoord = Coord2D.ORIGIN;
        }

        centreViewOnSurveyPoint(activeStationCoord);
    }

    public void centreViewOnSurveyPoint(Coord2D point) {

        float xDeltaInMetres = (getWidth() / 2f) / surveyToViewScale;
        float yDeltaInMetres = (getHeight() / 2f) / surveyToViewScale;

        float x = point.x - xDeltaInMetres;
        float y = point.y - yDeltaInMetres;

        viewpointOffset = new Coord2D(x, y);
    }

    private void drawDashedLine(
            Canvas canvas, Coord2D start, Coord2D end, float dashLength, Paint paint) {

        // this switcheroo is so we start from the end of the line and draw backwards
        // (we prefer the end of the line to line up with a dash rather than the start)
        Coord2D swap = end;
        end = start;
        start = swap;

        float lineLength = Space2DUtils.getDistance(start, end);
        int dashes = (int) (lineLength / dashLength / 2f);

        Coord2D direction = end.minus(start).normalise();
        Coord2D dashStep = direction.scale(dashLength);

        float stepX = dashStep.x;
        float stepY = dashStep.y;

        float[] lines = new float[dashes * 4]; // xy coords for start and end of dashes == 4

        int lineIndex = 0;
        float startX, startY, endX, endY;
        float previousX = 0, previousY = 0;

        for (int dashIndex = 0; dashIndex < dashes; dashIndex++) {

            if (dashIndex == 0) {
                startX = start.x;
                startY = start.y;
            } else {
                startX = previousX + stepX;
                startY = previousY + stepY;
            }

            endX = startX + stepX;
            endY = startY + stepY;

            lines[lineIndex++] = startX;
            lines[lineIndex++] = startY;
            lines[lineIndex++] = endX;
            lines[lineIndex++] = endY;

            previousX = endX;
            previousY = endY;
        }

        canvas.drawLines(lines, paint);
    }

    public void adjustZoomBy(float delta) {
        float newZoom = surveyToViewScale * delta;
        setZoom(newZoom);
    }

    public void setZoom(float newZoom) {
        Coord2D centre = new Coord2D(getWidth() / 2f, getHeight() / 2f);
        setZoom(newZoom, centre);
    }

    public void setZoom(float newZoom, Coord2D focusOnScreen) {

        if (MIN_ZOOM >= newZoom || newZoom >= MAX_ZOOM) {
            return;
        }

        Coord2D focusInSurveyCoords = viewCoordsToSurveyCoords(focusOnScreen);

        Coord2D delta = focusInSurveyCoords.minus(viewpointOffset);

        Coord2D scaledDelta = delta.scale(surveyToViewScale / newZoom);
        viewpointOffset = focusInSurveyCoords.minus(scaledDelta);

        surveyToViewScale = newZoom;
    }

    public void undo() {
        sketch.undo();
        invalidate();
    }

    public void redo() {
        sketch.redo();
        invalidate();
    }

    public void setBrushColour(BrushColour brushColour) {
        sketch.setActiveColour(brushColour.getColour());
    }

    public void setCurrentSymbol(Symbol symbol) {
        currentSymbol = symbol;
    }

    public void setSketchTool(SketchTool sketchTool) {
        if (previousSketchTool != currentSketchTool && !currentSketchTool.isModal()) {
            previousSketchTool = currentSketchTool;
        }
        currentSketchTool = sketchTool;
    }

    public void setCompassAzimuth(float degrees) {
        compassAzimuthDegrees = degrees;
        if (SketchPreferences.Toggle.SHOW_COMPASS.isOn() && projectionType == Projection2D.PLAN) {
            invalidate();
        }
    }

    public void setCachedStats(float surveyLength, float surveyHeight) {
        this.surveyLength = surveyLength;
        this.surveyHeight = surveyHeight;
    }

    public SketchTool getSketchTool() {
        return currentSketchTool;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            if (currentSketchTool != SketchTool.PINCH_TO_ZOOM) {
                setSketchTool(SketchTool.PINCH_TO_ZOOM);
            }

            float x = detector.getFocusX();
            float y = detector.getFocusY();
            Coord2D focus = new Coord2D(x, y);

            float scaleFactor = detector.getScaleFactor();
            setZoom(surveyToViewScale * scaleFactor, focus);

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
                showContextMenu(motionEvent, matchedStation);
                menuShownInThisTouch = true;
            }
        }
    }
}

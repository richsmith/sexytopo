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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RotateDrawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SexyTopo;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.activity.GraphActivity;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


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

    public static final Colour LEG_COLOUR = Colour.RED;
    public static final Colour LATEST_LEG_COLOUR = Colour.MAGENTA;
    public static final Colour HIGHLIGHT_COLOUR = Colour.GOLD;
    public static final Colour DEFAULT_SKETCH_COLOUR = Colour.BLACK;
    public static final Colour CROSS_SECTION_CONNECTION_COLOUR = Colour.SILVER;

    public static final int SOLID_ALPHA = 0xff;
    public static final int FADED_ALPHA = 0xff / 5;

    public static final int STATION_COLOUR = Colour.DARK_RED.intValue;
    public static final int STATION_DIAMETER = 8;

    public static final int STATION_STROKE_WIDTH = 5;
    public static final int HIGHLIGHT_OUTLINE = 4;
    private final float DASHED_LINE_INTERVAL = 5;

    public static final int LEGEND_SIZE = 18;
    public final float LEGEND_TICK_SIZE = 5;



    public static final float DELETE_PATHS_WITHIN_N_PIXELS = 5.0f;
    public static final float SELECTION_SENSITIVITY_IN_PIXELS = 25.0f;
    public static final float SNAP_TO_LINE_SENSITIVITY_IN_PIXELS = 25.0f;
    public static final float HOT_CORNER_DISTANCE_PROPORTION = 0.05f;

    public static final int STATION_LABEL_OFFSET = 10;

    private GraphActivity activity;

    private Projection2D projectionType;
    private Survey survey;
    private Space<Coord2D> projection;
    private Sketch sketch;

    private Map<Survey, Space<Coord2D>> translatedConnectedSurveys = new HashMap<>();

    boolean surveyChanged;

    // cached preferences for performance
    private boolean isDarkModeActive = false;
    private boolean isTwoFingerModeActive = true;
    private boolean isHotCornersModeActive = true;


    // cached for performance
    private Coord2D canvasBottomRight;
    private Coord2D viewpointTopLeftOnSurvey;
    private Coord2D viewpointBottomRightOnSurvey;
    private float surveyLength = 0;
    private float surveyHeight = 0;
    private Rect topLeftCorner;
    private Rect topRightCorner;
    private Rect bottomRightCorner;

    private Bitmap commentIcon, linkIcon;

    public SketchTool currentSketchTool = SketchTool.MOVE;
    // used to jump back to the previous tool when using one-use tools
    private SketchTool previousSketchTool = SketchTool.SELECT;

    // Flag to prevent double menu opening during this touch sequence
    private boolean menuShownInThisTouch = false;
    private Symbol currentSymbol = Symbol.getDefault();

    // a bit hacky but I can't think of a better way to do this
    private String stationNameBeingCrossSectioned = null;


    // ********** Paints and other drawing variables **********

    private final Paint stationPaint = new Paint();

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
    private final Paint hotCornersPaint = new Paint();

    private final Paint[] ANTI_ALIAS_PAINTS = new Paint[] {
            stationPaint, legendPaint, latestLegPaint, splayPaint,
            fadedLegPaint, fadedLatestLegPaint, fadedSplayPaint,
            drawPaint, labelPaint, legendPaint,
            crossSectionConnectorPaint, crossSectionIndicatorPaint
    };

    private int stationCrossDiameter;


    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        longPressDetector = new GestureDetector(context, new LongPressListener());
    }


    public void initialisePaint() {

        boolean applyAntiAlias = GeneralPreferences.isAntialiasingModeOn();
        for (Paint paint: ANTI_ALIAS_PAINTS) {
            if (paint.isAntiAlias() != applyAntiAlias) {
                paint.setAntiAlias(applyAntiAlias);
            }
        }

        int gridColour = ContextCompat.getColor(activity, R.color.grid);
        gridPaint.setColor(gridColour);

        int stationColour = ContextCompat.getColor(activity, R.color.station);
        stationPaint.setColor(stationColour);
        stationPaint.setStrokeWidth(STATION_STROKE_WIDTH);
        int stationLabelFontSizeSp = GeneralPreferences.getStationLabelFontSizeSp();
        float stationLabelFontSizePixels = spToPixels(stationLabelFontSizeSp);
        stationPaint.setTextSize(stationLabelFontSizePixels);

        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setStrokeWidth(HIGHLIGHT_OUTLINE);
        int activeStationHighlightColor = androidx.core.content.ContextCompat.getColor(
            getContext(), R.color.activeStationHighlight);
        highlightPaint.setColor(activeStationHighlightColor);

        // active legs/splays
        int legStrokeWidth = GeneralPreferences.getLegStrokeWidth();
        legPaint.setStrokeWidth(legStrokeWidth);
        legPaint.setColor(LEG_COLOUR.intValue);

        latestLegPaint.setStrokeWidth(legStrokeWidth);
        latestLegPaint.setColor(LATEST_LEG_COLOUR.intValue);

        int splayStrokeWidth = GeneralPreferences.getSplayStrokeWidth();
        splayPaint.setStrokeWidth(splayStrokeWidth);
        splayPaint.setColor(LEG_COLOUR.intValue);

        // faded legs/splays
        fadedLegPaint.setStrokeWidth(legStrokeWidth);
        fadedLegPaint.setColor(LEG_COLOUR.intValue);
        fadedLegPaint.setAlpha(FADED_ALPHA);

        fadedLatestLegPaint.setStrokeWidth(legStrokeWidth);
        fadedLatestLegPaint.setColor(LATEST_LEG_COLOUR.intValue);
        fadedLatestLegPaint.setAlpha(FADED_ALPHA);

        fadedSplayPaint.setStrokeWidth(splayStrokeWidth);
        fadedSplayPaint.setColor(LEG_COLOUR.intValue);
        fadedSplayPaint.setAlpha(FADED_ALPHA);

        drawPaint.setColor(DEFAULT_SKETCH_COLOUR.intValue);
        drawPaint.setStrokeWidth(3);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        int legendColour = ContextCompat.getColor(activity, R.color.legend);
        legendPaint.setColor(legendColour);
        float legendSizeSp = GeneralPreferences.getLegendFontSizeSp();
        float legendSizePixels = spToPixels(legendSizeSp);
        legendPaint.setTextSize(legendSizePixels);

        int labelColour = ContextCompat.getColor(activity, R.color.station);
        legPaint.setColor(labelColour);
        int labelSizeSp = GeneralPreferences.getLabelFontSizeSp();
        float labelSizePixels = spToPixels(labelSizeSp);
        legPaint.setTextSize(labelSizePixels);

        crossSectionConnectorPaint.setColor(CROSS_SECTION_CONNECTION_COLOUR.intValue);
        crossSectionConnectorPaint.setStrokeWidth(3);
        crossSectionConnectorPaint.setStyle(Paint.Style.STROKE);

        crossSectionIndicatorPaint.setColor(STATION_COLOUR);
        crossSectionIndicatorPaint.setStrokeWidth(2);
        crossSectionIndicatorPaint.setStyle(Paint.Style.FILL);

        isTwoFingerModeActive = GeneralPreferences.isTwoFingerModeActive();

        isHotCornersModeActive = GeneralPreferences.isHotCornersModeActive();
        hotCornersPaint.setColor(Colour.GREY.intValue);
        hotCornersPaint.setAlpha(FADED_ALPHA);

        stationCrossDiameter = GeneralPreferences.getStationCrossDiameterPixels();

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

        if (currentSketchTool.isModal() && event.getAction() == MotionEvent.ACTION_UP) {
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

        switch (currentSketchTool) {
            case MOVE:
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
                (hitLeftEdge && (hitBottomEdge || hitTopEdge)) ||
                (hitRightEdge && (hitBottomEdge || hitTopEdge));

        return hitCorner;
    }


    private Coord2D viewCoordsToSurveyCoords(final Coord2D coords) {
        // The more elegant way to do this is:
        // return coords.scale(1 / surveyToViewScale).plus(viewpointOffset);
        // ...but this method gets hit hard (profiled) so let's avoid creating intermediate objects:
        return new Coord2D(((coords.x * (1 / surveyToViewScale)) + viewpointOffset.x),
                           ((coords.y * (1 / surveyToViewScale)) + viewpointOffset.y));
    }

    // Warning: In tight loops during the draw phase we duplicate this logic to avoid
    //          creating too many Coord2D objects - be sure to mirror any updates in those places
    private Coord2D surveyCoordsToViewCoords(final Coord2D coords) {
        // The more elegant way to do this is:
        // return coords.minus(viewpointOffset).scale(surveyToViewScale);
        // ...but this method gets hit hard (profiled) so let's avoid creating intermediate objects:
        return new Coord2D(((coords.x - viewpointOffset.x) * surveyToViewScale),
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
        float deltaInMetres = SNAP_TO_LINE_SENSITIVITY_IN_PIXELS / surveyToViewScale;
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

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                SketchDetail closestDetail = sketch.findNearestDetailWithin(
                        touchPointOnSurvey, DELETE_PATHS_WITHIN_N_PIXELS);

                // you missed, try again :P
                if (closestDetail == null) {
                    return true;

                // you got part of the line
                } else if (deleteLineFragments && closestDetail instanceof PathDetail) {
                    List<SketchDetail> fragments =
                            ((PathDetail)closestDetail).getPathFragmentsOutsideRadius(
                                    touchPointOnSurvey, DELETE_PATHS_WITHIN_N_PIXELS / 4);
                    sketch.deleteDetail(closestDetail, fragments);
                    invalidate();

                // bullseye!
                } else {
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
                    float angle = Space2DUtils.getAngleBetween(
                        actionDownPointOnView, touchPointOnView);
                    angle = Space2DUtils.adjustAngle(angle, -90);
                    Coord2D firstTouch = viewCoordsToSurveyCoords(actionDownPointOnView);
                    sketch.addSymbolDetail(firstTouch, currentSymbol, size, angle);
                    float distance = Space2DUtils.getDistance(
                        actionDownPointOnView, touchPointOnView);
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
                TextInputLayout inputLayout = new TextInputLayout(getContext());
                inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
                inputLayout.setHint(getContext().getString(R.string.graph_text_hint));

                TextInputEditText input = new TextInputEditText(getContext());
                inputLayout.addView(input);

                int paddingH = (int) (24 * getResources().getDisplayMetrics().density);
                int paddingV = (int) (20 * getResources().getDisplayMetrics().density);
                inputLayout.setPadding(paddingH, paddingV, paddingH, 0);

                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
                builder.setView(inputLayout)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        String text = input.getText().toString();
                        int startingSizeSp = GeneralPreferences.getTextStartingSizeSp();
                        float startingSizePixels = spToPixels(startingSizeSp);
                        float size = startingSizePixels / surveyToViewScale;
                        sketch.addTextDetail(touchPointOnSurvey, text, size);
                        invalidate();
                    })
                    .setNegativeButton(R.string.cancel, null);

                android.app.Dialog dialog = builder.create();

                // Automatically select text field
                dialog.setOnShowListener(dialogInterface -> {
                    input.requestFocus();
                    InputMethodManager imm = (InputMethodManager)
                        getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
                });

                // Keep keyboard visible
                dialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
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
        float selectionTolerance =
                SELECTION_SENSITIVITY_IN_PIXELS / surveyToViewScale;
        Coord2D touchPointOnSurvey = viewCoordsToSurveyCoords(touchPointOnView);

        Station matchedStation = findNearestStationWithinDelta(projection,
                touchPointOnSurvey, selectionTolerance);
        return matchedStation; // this could be null if nothing is near
    }


    private boolean handlePositionCrossSection(MotionEvent event) {

        Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        Coord2D touchPointOnSurvey = viewCoordsToSurveyCoords(touchPointOnView);

        final Station station = survey.getStationByName(stationNameBeingCrossSectioned);
        stationNameBeingCrossSectioned = null;
        if (station == null) {
            return true;
        }

        CrossSection crossSection = CrossSectioner.section(survey, station);

        sketch.addCrossSection(crossSection, touchPointOnSurvey);

        setSketchTool(previousSketchTool);
        invalidate();

        return true;
    }


    private void showContextMenu(MotionEvent event, final Station station) {
        // Determine view context based on projection type
        ViewContext viewContext = getViewContextFromProjection();

        // Use activity as listener (it implements ContextMenuManager.StationMenuListener)
        ContextMenuManager menuManager = new ContextMenuManager(getContext(), viewContext, activity);
        menuManager.showMenu(this, station, survey, (int) event.getX(), (int) event.getY());
    }

    /**
     * Determine the view context for the context menu based on current projection.
     */
    private ViewContext getViewContextFromProjection() {
        if (projectionType == null) {
            return ViewContext.PLAN;
        }

        if (projectionType == Projection2D.PLAN) {
            return ViewContext.PLAN;
        } else if (projectionType == Projection2D.EXTENDED_ELEVATION) {
            return ViewContext.EXTENDED_ELEVATION;
        } else {
            return ViewContext.ELEVATION;
        }
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

    private void broadcastSurveyUpdated() {
        SurveyManager.getInstance(getContext().getApplicationContext()).broadcastSurveyUpdated();
    }


    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        canvasBottomRight = new Coord2D(getWidth(), getHeight());

        viewpointTopLeftOnSurvey = viewCoordsToSurveyCoords(Coord2D.ORIGIN);
        viewpointBottomRightOnSurvey = viewCoordsToSurveyCoords(canvasBottomRight);

        if (SketchPreferences.Toggle.SHOW_GRID.isOn()) {
            drawGrid(canvas);
        }

        if (SketchPreferences.Toggle.SHOW_CONNECTIONS.isOn()) {
            drawConnectedSurveys(canvas, projection, FADED_ALPHA);
        }

        drawSurvey(canvas, survey, projection, SOLID_ALPHA);

        drawLegend(canvas);
        drawHotCorners(canvas);

        if (activity.isDebugMode()) {
            drawDebuggingInfo(canvas);
        }
    }

    private void drawSurvey(Canvas canvas, Survey survey, Space<Coord2D> projection, int alpha) {
        drawSketch(canvas, activity.getSketch(survey), alpha);
        drawCrossSections(canvas, sketch.getCrossSectionDetails(), alpha);
        drawSurveyData(survey, canvas, projection, alpha);
    }


    private void drawConnectedSurveys(Canvas canvas, Space<Coord2D> projection, int alpha) {

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
                String name =  translatedConnectedSurvey.getName();
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


    private void drawGrid(Canvas canvas) {

        int tickSizeInMetres = getMinorGridBoxSize();
        int numberTicksJustBeforeViewpointOffsetX = (int)(viewpointOffset.x / tickSizeInMetres);

        for (int n = numberTicksJustBeforeViewpointOffsetX; true; n++) {
            float xSurvey = n * tickSizeInMetres;
            int xView = (int)((xSurvey - viewpointOffset.x) * surveyToViewScale);
            gridPaint.setStrokeWidth(n % BOX_SIZE == 0 ? 3 : 1);
            canvas.drawLine(xView, 0, xView, getHeight(), gridPaint);
            if (xView >= getWidth()) {
                break;
            }
        }

        int numberTicksJustBeforeViewpointOffsetY = (int)(viewpointOffset.y / tickSizeInMetres);

        for (int n = numberTicksJustBeforeViewpointOffsetY; true; n++) {
            float ySurvey = n * tickSizeInMetres;
            int yView = (int)((ySurvey - viewpointOffset.y) * surveyToViewScale);
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

        boolean showStationLabels = SketchPreferences.Toggle.SHOW_STATION_LABELS.isOn();

        crossSectionConnectorPaint.setAlpha(alpha);

        List<CrossSectionDetail> badXSections = new ArrayList<>();

        for (CrossSectionDetail sectionDetail : crossSectionDetails) {

            if (!couldBeOnScreen(sectionDetail)) {
                continue;
            }

            CrossSection crossSection = sectionDetail.getCrossSection();
            if (crossSection == null) {
                badXSections.add(sectionDetail);
                continue;
            }

            Station station = crossSection.getStation();
            if (station == null) {
                badXSections.add(sectionDetail);
                continue;
            }

            Coord2D surveyStationLocation = this.projection.getStationMap().get(station);
            if (surveyStationLocation == null) {
                badXSections.add(sectionDetail);
                continue;
            }

            Coord2D centreOnSurvey = sectionDetail.getPosition();
            Coord2D centreOnView = surveyCoordsToViewCoords(centreOnSurvey);
            drawStationCross(
                    canvas, stationPaint, centreOnView.x, centreOnView.y, STATION_DIAMETER, alpha);

            String description =
                    sectionDetail.getCrossSection().getStation().getName() + " X";
            if (showStationLabels) {
                stationPaint.setAlpha(alpha);
                canvas.drawText(description, centreOnView.x, centreOnView.y, stationPaint);
            }

            Space<Coord2D> projection = sectionDetail.getProjection();

            drawLegs(canvas, projection, alpha);

            Coord2D viewStationLocation = surveyCoordsToViewCoords(surveyStationLocation);
            drawDashedLine(
                    canvas, viewStationLocation, centreOnView,
                    DASHED_LINE_INTERVAL, crossSectionConnectorPaint);
        }

        for (CrossSectionDetail crossSectionDetail : badXSections) {
            Station station = crossSectionDetail.getCrossSection().getStation();
            String name = station == null? "Unknown" : station.getName();
            Log.e("Missing station details for cross section on station " + name + "; removing");
            crossSectionDetails.remove(crossSectionDetail);
        }
    }


    private void drawLegs(Canvas canvas, Space<Coord2D> space, int baseAlpha) {

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

            boolean fade = baseAlpha == FADED_ALPHA || (fadingNonActive && !isAttachedToActive(leg));

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
                drawDashedLine(canvas, start, end, DASHED_LINE_INTERVAL, paint);
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

            int x = (int)(translatedStation.x);
            int y = (int)(translatedStation.y);

            drawStationCross(canvas, stationPaint, x, y, stationCrossDiameter, alpha);

            if (station == survey.getActiveStation()) {
                highlightActiveStation(canvas, x, y);
            }

            int spacing = stationCrossDiameter / 2;
            int nextX = x + stationCrossDiameter;

            if (showStationLabels) {
                String name = station.getName();
                if (station == survey.getOrigin()) {
                    name = name + " (" + survey.getName() + ")";
                }
                canvas.drawText(name,
                        nextX,
                        y + STATION_LABEL_OFFSET,
                        stationPaint);
                nextX += Math.round(stationPaint.measureText(name)) + spacing;
            }

            List<Bitmap> icons = new ArrayList<>();
            if (station.hasComment()) {
                icons.add(commentIcon);
            }
            if (survey.hasLinkedSurveys(station)) {
                icons.add(linkIcon);
            }

            for (Bitmap icon : icons) {int yTop = y - stationCrossDiameter / 2;
                Rect rect = new Rect(nextX, yTop, nextX + stationCrossDiameter, yTop + stationCrossDiameter);
                canvas.drawBitmap(icon, null, rect, stationPaint);
                nextX += stationCrossDiameter + spacing;
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

        float angle = (float)Math.toRadians(crossSection.getAngle());
        float indicatorWidth = (1 * surveyToViewScale);
        float startX = x - ((indicatorWidth / 2) * (float)Math.cos(angle));
        float startY = y - ((indicatorWidth / 2) * (float)Math.sin(angle));
        float endX = x + ((indicatorWidth / 2) * (float)Math.cos(angle));
        float endY = y + ((indicatorWidth / 2) * (float)Math.sin(angle));

        canvas.drawLine(startX, startY, endX, endY, crossSectionIndicatorPaint);

        float lineLength =
                Space2DUtils.getDistance(new Coord2D(startX, startY), new Coord2D(endX, endY));
        float arrowLength = lineLength * 0.4f;
        float arrowOuterCornerX = startX;
        float arrowOuterCornerY = startY;
        float arrowInnerCornerX = startX + ((lineLength * 0.05f) * (float)Math.cos(angle));
        float arrowInnerCornerY = startY + ((lineLength * 0.05f) * (float)Math.sin(angle));
        float arrowAngle = (float)Math.toRadians(Space2DUtils.adjustAngle(
                crossSection.getAngle(), -90));
        float arrowTipX = startX + (arrowLength * (float)Math.cos(arrowAngle));
        float arrowTipY = startY + (arrowLength * (float)Math.sin(arrowAngle));


        Path path = new Path();
        path.moveTo(arrowInnerCornerX, arrowInnerCornerY);
        path.lineTo(arrowOuterCornerX, arrowOuterCornerY);
        path.lineTo(arrowTipX, arrowTipY);
        path.lineTo(arrowInnerCornerX, arrowInnerCornerY);

        canvas.drawPath(path, crossSectionIndicatorPaint);
    }


    private void highlightActiveStation(Canvas canvas, float x, float y) {

        float diameter = 22;
        int gap = 6;
        float topY = y - (diameter / 2);
        float bottomY = y + (diameter / 2);
        float leftX = x - (diameter / 2);
        float rightX = x + (diameter / 2);

        float innerLeft = leftX + ((diameter - gap) / 2);
        float innerRight = innerLeft + gap;
        float innerTop = topY + ((diameter - gap) / 2);
        float innerBottom = innerTop + gap;

        // top lines
        canvas.drawLine(leftX, topY, innerLeft, topY, highlightPaint);
        canvas.drawLine(innerRight, topY, rightX, topY, highlightPaint);
        // bottom lines
        canvas.drawLine(leftX, bottomY, innerLeft, bottomY, highlightPaint);
        canvas.drawLine(innerRight, bottomY, rightX, bottomY, highlightPaint);
        // left lines
        canvas.drawLine(leftX, topY, leftX, innerTop, highlightPaint);
        canvas.drawLine(leftX, innerBottom, leftX, bottomY, highlightPaint);
        // right lines
        canvas.drawLine(rightX, topY, rightX, innerTop, highlightPaint);
        canvas.drawLine(rightX, innerBottom, rightX, bottomY, highlightPaint);
    }


    private void drawStationCross(
            Canvas canvas, Paint paint, float x, float y, int crossDiameter, int alpha) {
        paint.setAlpha(alpha);
        float halfCross = crossDiameter / 2f;
        canvas.drawLine(x , y - halfCross, x, y + halfCross, paint);
        canvas.drawLine(x - halfCross, y, x + halfCross, y, paint);
    }



    private void drawSketch(Canvas canvas, Sketch sketch, int alpha) {

        if (!SketchPreferences.Toggle.SHOW_SKETCH.isOn()) {
            return;
        }

        Colour lastColour = Colour.BLACK;

        drawPaint.setColor(lastColour.intValue);
        drawPaint.setAlpha(alpha);

        boolean isDebugMode = activity.isDebugMode();

        for (PathDetail pathDetail : sketch.getPathDetails()) {

            if (!couldBeOnScreen(pathDetail)) {
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
                    //from = surveyCoordsToViewCoords(point);
                    fromX = (point.x - viewpointOffset.x) * surveyToViewScale;
                    fromY = (point.y - viewpointOffset.y) * surveyToViewScale;

                    if (isDebugMode) {
                        canvas.drawCircle(fromX, fromY, 3, drawPaint);
                    }
                } else {
                    //Coord2D to = surveyCoordsToViewCoords(point);
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
            if (!couldBeOnScreen(symbolDetail)) {
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


    private void drawLegend(Canvas canvas) {

        String surveyLabel =
            survey.getName() +
            " L" + TextTools.formatTo0dpWithComma(surveyLength) +
            " V" + TextTools.formatTo0dpWithComma(surveyHeight);

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
        canvas.drawLine(
                x, scaleY, x + scaleWidth, scaleY, legendPaint);
        canvas.drawLine(
                x, scaleY, offsetX, scaleY - LEGEND_TICK_SIZE, legendPaint);
        canvas.drawLine(x + scaleWidth, scaleY,
                x + scaleWidth, scaleY - LEGEND_TICK_SIZE, legendPaint);
        String scaleLabel = minorGridSize + "m";
        canvas.drawText(scaleLabel, x + scaleWidth + 0.2f * legendSize, scaleY, legendPaint);

    }


    private void drawHotCorners(Canvas canvas) {

        if (!isHotCornersModeActive) {
            return;
        }

        if (currentSketchTool == SketchTool.MODAL_MOVE) {
            hotCornersPaint.setColor(Colour.YELLOW.intValue);
            hotCornersPaint.setAlpha(FADED_ALPHA);
        }

        final int side = (int) (HOT_CORNER_DISTANCE_PROPORTION * getWidth());
        topLeftCorner = new Rect(0, 0, side, side);
        topRightCorner = new Rect(getWidth() - side, 0, getWidth(), side);
        bottomRightCorner = new Rect(getWidth() - side, getHeight() - side, getWidth(), getHeight());

        canvas.drawRect(topLeftCorner, hotCornersPaint);
        canvas.drawRect(topRightCorner, hotCornersPaint);
        canvas.drawRect(bottomRightCorner, hotCornersPaint);

        if (currentSketchTool == SketchTool.MODAL_MOVE) {
            hotCornersPaint.setColor(Colour.GREY.intValue);
            hotCornersPaint.setAlpha(FADED_ALPHA);
        }

    }

    private void drawDebuggingInfo(Canvas canvas) {
        float offsetX = getWidth() * 0.03f;
        float offsetY = LEGEND_SIZE * 2;
        String label = "x=" + offsetX + " y=" + offsetY +
                " s2v=" + TextTools.formatTo2dp(surveyToViewScale) +
                " 1/s2v=" + TextTools.formatTo2dp(1 / surveyToViewScale) +
                //" 1/log=" + TextTools.formatTo2dp(1 / Math.log(surveyToViewScale)) +
                //" 1/log10=" + TextTools.formatTo2dp(1 / Math.log10(surveyToViewScale)) +
                "\n log (1/s2v) =" + TextTools.formatTo2dp(Math.log(1 /surveyToViewScale)) +
                "\n log10 (1/s2v) =" + TextTools.formatTo2dp(Math.log10(1 /surveyToViewScale));

        canvas.drawText(label, offsetX, offsetY, legendPaint);
    }


    private boolean couldBeOnScreen(SketchDetail sketchDetail) {
        return sketchDetail.intersectsRectangle(
                viewpointTopLeftOnSurvey, viewpointBottomRightOnSurvey);
    }


    public void centreViewOnActiveStation() {
        centreViewOnStation(survey.getActiveStation());
    }

    public void centreViewOnStation(Station station) {
        Coord2D activeStationCoord =
                projection.getStationMap().get(station);

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


    private void drawDashedLine(Canvas canvas,
                                Coord2D start, Coord2D end,
                                float dashLength,
                                Paint paint) {

        // this switcheroo is so we start from the end of the line and draw backwards
        // (we prefer the end of the line to line up with a dash rather than the start)
        Coord2D swap = end;
        end = start;
        start = swap;

        float lineLength = Space2DUtils.getDistance(start, end);
        int dashes = (int)(lineLength / dashLength / 2f);

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

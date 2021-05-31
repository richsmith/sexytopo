package org.hwyl.sexytopo.control.graph;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.PopupWindow;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.activity.ExtendedElevationActivity;
import org.hwyl.sexytopo.control.activity.GraphActivity;
import org.hwyl.sexytopo.control.activity.PlanActivity;
import org.hwyl.sexytopo.control.activity.TableActivity;
import org.hwyl.sexytopo.control.util.CohenSutherlandAlgorithm;
import org.hwyl.sexytopo.control.util.CrossSectioner;
import org.hwyl.sexytopo.control.util.PreferenceAccess;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.control.util.SurveyStats;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Direction;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class GraphView extends View {

    public static boolean DEBUG = false;

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector longPressDetector;

    // The offset of the viewing window (what can be seen on the screen) from the whole survey
    private Coord2D viewpointOffset = Coord2D.ORIGIN;

    // These variables are used in handling the dragging of the viewing window
    private Coord2D actionDownPointOnView = Coord2D.ORIGIN;
    private Coord2D actionDownViewpointOffset = Coord2D.ORIGIN;

    // ratio of metres on the survey to pixels on the view
    // zooming in increases this, zooming out decreases it
    private double surveyToViewScale = 60.0;

    public static final double MIN_ZOOM = 0.1;
    public static final double MAX_ZOOM = 500.0;

    private GraphActivity activity;

    private Projection2D projectionType;
    private Survey survey;
    private Space<Coord2D> projection;
    private Sketch sketch;

    private Map<Survey, Space<Coord2D>> translatedConnectedSurveys = new HashMap<>();

    // cached for performance
    private Coord2D canvasBottomRight;
    private Coord2D viewpointTopLeftOnSurvey;
    private Coord2D viewpointBottomRightOnSurvey;
    private double surveyLength = 0;
    private double surveyHeight = 0;
    private Rect topLeftCorner;
    private Rect topRightCorner;
    private Rect bottomRightCorner;

    public static final Colour LEG_COLOUR = Colour.RED;
    public static final Colour LATEST_LEG_COLOUR = Colour.MAGENTA;
    public static final Colour HIGHLIGHT_COLOUR = Colour.GOLD;
    public static final Colour DEFAULT_SKETCH_COLOUR = Colour.BLACK;
    public static final Colour CROSS_SECTION_CONNECTION_COLOUR = Colour.SILVER;

    public static final int SOLID_ALPHA = 0xff;
    public static final int FADED_ALPHA = 0xff / 5;

    public static final int STATION_COLOUR = Colour.DARK_RED.intValue;
    public static final int STATION_DIAMETER = 8;
    public static final int CROSS_DIAMETER = 16;
    public static final int STATION_STROKE_WIDTH = 5;
    public static final int HIGHLIGHT_OUTLINE = 4;

    public static final int LEGEND_SIZE = 18;
    public static final Colour LEGEND_COLOUR = Colour.BLACK;
    public static final Colour GRID_COLOUR = Colour.LIGHT_GREY;

    public static final double DELETE_PATHS_WITHIN_N_PIXELS = 5.0;
    public static final double SELECTION_SENSITIVITY_IN_PIXELS = 25.0;
    public static final double SNAP_TO_LINE_SENSITIVITY_IN_PIXELS = 25.0;
    public static final double HOT_CORNER_DISTANCE_PROPORTION = 0.05;

    public static final int STATION_LABEL_OFFSET = 10;

    private Bitmap commentIcon, linkIcon;

    public SketchTool currentSketchTool = SketchTool.MOVE;
    // used to jump back to the previous tool when using one-use tools
    private SketchTool previousSketchTool = SketchTool.SELECT;
    private Symbol currentSymbol = Symbol.getDefault();

    private final DashPathEffect dashPathEffect = new DashPathEffect(new float[]{5, 5}, 0);

    private final Paint stationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint legPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint latestLegPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint splayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint fadedLegPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fadedLatestLegPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fadedSplayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint drawPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint legendPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint();
    private final Paint crossSectionConnectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint crossSectionIndicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hotCornersPaint = new Paint();

    private final Path dashedPath = new Path();

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        longPressDetector = new GestureDetector(context, new LongPressListener());
        initialisePaint();
    }


    public void initialisePaint() {

        stationPaint.setColor(STATION_COLOUR);
        stationPaint.setStrokeWidth(STATION_STROKE_WIDTH);
        int labelSize = PreferenceAccess.getInt(getContext(), "pref_station_label_font_size", 22);
        stationPaint.setTextSize(labelSize);

        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setStrokeWidth(HIGHLIGHT_OUTLINE);
        highlightPaint.setColor(HIGHLIGHT_COLOUR.intValue);

        // active legs/splays
        int legStrokeWidth = PreferenceAccess.getInt(getContext(), "pref_leg_width", 3);
        legPaint.setStrokeWidth(legStrokeWidth);
        legPaint.setColor(LEG_COLOUR.intValue);

        latestLegPaint.setStrokeWidth(legStrokeWidth);
        latestLegPaint.setColor(LATEST_LEG_COLOUR.intValue);

        int splayStrokeWidth = PreferenceAccess.getInt(getContext(), "pref_splay_width", 1);
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

        gridPaint.setColor(GRID_COLOUR.intValue);

        drawPaint.setColor(DEFAULT_SKETCH_COLOUR.intValue);
        drawPaint.setStrokeWidth(3);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        legendPaint.setColor(LEGEND_COLOUR.intValue);
        legendPaint.setTextSize(LEGEND_SIZE);

        labelPaint.setColor(STATION_COLOUR);
        int textSize = PreferenceAccess.getInt(getContext(), "pref_survey_text_font_size", 32);
        labelPaint.setTextSize(textSize);

        crossSectionConnectorPaint.setColor(CROSS_SECTION_CONNECTION_COLOUR.intValue);
        crossSectionConnectorPaint.setStrokeWidth(3);
        crossSectionConnectorPaint.setStyle(Paint.Style.STROKE);
        crossSectionConnectorPaint.setPathEffect(dashPathEffect);

        crossSectionIndicatorPaint.setColor(STATION_COLOUR);
        crossSectionIndicatorPaint.setStrokeWidth(2);
        crossSectionIndicatorPaint.setStyle(Paint.Style.FILL);

        hotCornersPaint.setColor(Colour.GREY.intValue);
        hotCornersPaint.setAlpha(FADED_ALPHA);

        commentIcon = BitmapFactory.decodeResource(getResources(), R.drawable.speech_bubble);
        linkIcon = BitmapFactory.decodeResource(getResources(), R.drawable.link);
    }


    public void setActivity(GraphActivity graphActivity) {
        this.activity = graphActivity;
    }


    public void setSurvey(Survey survey) {
        if (survey != this.survey) {
            this.survey = survey;
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


    @Override
    public boolean onTouchEvent(MotionEvent event) {

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

        considerModalMoveSelection(event);

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

    private void considerModalMoveSelection(MotionEvent event) {

        if (currentSketchTool == SketchTool.MODAL_MOVE) {
            return;
        }

        if (!activity.getBooleanPreference("pref_hot_corners")) {
            return;
        }

        float x = event.getX();
        float y = event.getY();
        int height = getHeight();
        int width = getWidth();

        double corner_delta = Math.min(height, width) * HOT_CORNER_DISTANCE_PROPORTION;

        boolean hitLeftEdge = x < corner_delta;
        boolean hitRightEdge = x > width - corner_delta;
        boolean hitTopEdge = y < corner_delta;
        boolean hitBottomEdge = y > height - corner_delta;

        boolean hitCorner =
                (hitLeftEdge && (hitBottomEdge || hitTopEdge)) ||
                (hitRightEdge && (hitBottomEdge || hitTopEdge));

        if (hitCorner) {
            setSketchTool(SketchTool.MODAL_MOVE);
        }
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

        boolean snapToLines = getDisplayPreference(GraphActivity.DisplayPreference.SNAP_TO_LINES);

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

        double deltaInMetres = SNAP_TO_LINE_SENSITIVITY_IN_PIXELS / surveyToViewScale;

        Coord2D closestPathEnd = sketch.findEligibleSnapPointWithin(pointTouched, deltaInMetres);
        if (closestPathEnd != null) {
            return closestPathEnd;
        } else {
            return null;
        }
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

        boolean deleteLineFragments = PreferenceAccess.getBoolean(
            getContext(), "pref_delete_path_fragments", true);

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
                break;
            case MotionEvent.ACTION_UP:
                break;
            default:
                return false;
        }

        return true;
    }


    private boolean handleSymbol(MotionEvent event) {

        final Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        final Coord2D touchPointOnSurvey = viewCoordsToSurveyCoords(touchPointOnView);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                int startingSize = PreferenceAccess.getInt(getContext(),
                        "pref_survey_symbol_size", 35);
                float size = startingSize / (float)surveyToViewScale;
                sketch.addSymbolDetail(touchPointOnSurvey, currentSymbol, size);
                invalidate();
                return true;
            default:
                return false;
        }
    }


    private boolean handleText(MotionEvent event) {

        final Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        final Coord2D touchPointOnSurvey = viewCoordsToSurveyCoords(touchPointOnView);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                final EditText input = new EditText(getContext());
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setView(input)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String text = input.getText().toString();
                                int startingSize = PreferenceAccess.getInt(getContext(),
                                        "pref_survey_text_tool_font_size", 50);
                                float size = startingSize / (float)surveyToViewScale;
                                sketch.addTextDetail(touchPointOnSurvey, text, size);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        });
                AlertDialog dialog = builder.create();
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
                    setActiveStation(newSelectedStation);
                    invalidate();
                    return true;

                } else { // double selection opens context menu
                    showContextMenu(event, newSelectedStation);
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

        double selectionTolerance =
                SELECTION_SENSITIVITY_IN_PIXELS / surveyToViewScale;

        Coord2D touchPointOnSurvey = viewCoordsToSurveyCoords(touchPointOnView);

        Station newSelectedStation = findNearestStationWithinDelta(projection,
                touchPointOnSurvey, selectionTolerance);

        return newSelectedStation; // this could be null if nothing is near
    }


    private boolean handlePositionCrossSection(MotionEvent event) {

        Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        Coord2D touchPointOnSurvey = viewCoordsToSurveyCoords(touchPointOnView);

        final Station station = survey.getActiveStation();
        CrossSection crossSection = CrossSectioner.section(survey, station);

        sketch.addCrossSection(crossSection, touchPointOnSurvey);

        setSketchTool(previousSketchTool);
        invalidate();

        return true;
    }


    private void showContextMenu(MotionEvent event, final Station station) {

        OnClickListener listener = new OnClickListener() {
            @Override
            public void onClick(View view) {
                switch(view.getId()) {
                    case R.id.graph_station_select:
                        setActiveStation(station);
                        invalidate();
                        break;
                    case R.id.graph_station_toggle_left_right:
                        Direction newDirection = station.getExtendedElevationDirection().opposite();
                        SurveyUpdater.setDirectionOfSubtree(survey, station,newDirection);
                        broadcastSurveyUpdated();
                        invalidate();
                        break;
                    case R.id.graph_station_comment:
                        openCommentDialog(station);
                        break;
                    case R.id.graph_station_reverse:
                        SurveyUpdater.reverseLeg(survey, station);
                        broadcastSurveyUpdated();
                        invalidate();
                        break;
                    case R.id.graph_station_delete:
                        askAboutDeletingStation(station);
                        invalidate();
                        break;
                    case R.id.graph_station_new_cross_section:
                        setActiveStation(station);
                        setSketchTool(SketchTool.POSITION_CROSS_SECTION);
                        activity.showSimpleToast(R.string.position_cross_section_instruction);
                        break;
                    case R.id.graph_station_jump_to_table:
                        activity.jumpToStation(station, TableActivity.class);
                        break;
                    case R.id.graph_station_jump_to_plan:
                        activity.jumpToStation(station, PlanActivity.class);
                        break;
                    case R.id.graph_station_jump_to_ee:
                        activity.jumpToStation(station, ExtendedElevationActivity.class);
                        break;
                    case R.id.graph_station_start_new_survey:
                        if (!survey.isSaved()) {
                            activity.showSimpleToast(R.string.cannot_extend_unsaved_survey);
                        }
                        activity.continueSurvey(station);
                        break;
                    case R.id.graph_station_unlink_survey:
                        activity.unlinkSurvey(station);
                        break;
                }
            }
        };

        PopupWindow menu = activity.getContextMenu(station, listener);

        View unlinkSurveyButton = menu.getContentView().findViewById(R.id.graph_station_unlink_survey);
        unlinkSurveyButton.setEnabled(survey.hasLinkedSurveys(station));

        View commentButton = menu.getContentView().findViewById(R.id.graph_station_comment);
        commentButton.setEnabled(station != survey.getOrigin());

        menu.showAtLocation(this, Gravity.START | Gravity.TOP,
                (int) (event.getX()), (int) (event.getY()));
    }


    private void openCommentDialog(final Station station) {
        final EditText input = new EditText(getContext());
        input.setLines(8);
        input.setGravity(Gravity.START | Gravity.TOP);
        input.setText(station.getComment());
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(input)
                .setTitle(station.getName())
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        station.setComment(input.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();
    }


    private void askAboutDeletingStation(final Station station) {

        int numFullLegsToBeDeleted = 1 + SurveyStats.calcNumberSubFullLegs(station);
        int numSplaysToBeDeleted = SurveyStats.calcNumberSubSplays(station);

        Context context = getContext();
        String message = context.getString(R.string.this_will_delete);

        if (numFullLegsToBeDeleted > 0) {
            String noun = context.getString(R.string.leg).toLowerCase();
            message += "\n" + TextTools.pluralise(numFullLegsToBeDeleted, noun);
            noun = context.getString(R.string.station).toLowerCase();
            message += " (" + TextTools.pluralise(numFullLegsToBeDeleted, noun) + ")";
        }
        if (numSplaysToBeDeleted > 0) {
            String noun = context.getString(R.string.splay).toLowerCase();
            message += "\n" + TextTools.pluralise(numSplaysToBeDeleted, noun);
        }

        new AlertDialog.Builder(getContext())
                .setMessage(message)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SurveyUpdater.deleteStation(survey, station);
                        broadcastSurveyUpdated();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }


    private static Station findNearestStationWithinDelta(
            Space<Coord2D> space, Coord2D target, double delta) {

        double shortest = Double.MAX_VALUE;
        Station best = null;

        for (Station station : space.getStationMap().keySet()) {
            Coord2D point = space.getStationMap().get(station);
            double distance = Space2DUtils.getDistance(point, target);

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

    private void setActiveStation(Station station) {
        survey.setActiveStation(station);
        broadcastSurveyUpdated();
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

        if (getDisplayPreference(GraphActivity.DisplayPreference.SHOW_GRID)) {
            drawGrid(canvas);
        }

        if (getDisplayPreference(GraphActivity.DisplayPreference.SHOW_CONNECTIONS)) {
            drawConnectedSurveys(canvas, projection, FADED_ALPHA);
        }

        drawSurvey(canvas, survey, projection, SOLID_ALPHA);

        drawLegend(canvas);
        drawHotCorners(canvas);

        if (DEBUG) {
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
                        ConnectedSurveys.getTranslatedConnectedSurveys(activity, survey, projection);
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

        // FIXME need a better tick size function when we sort out zooming
        //scale           <-adjustZoomBy out 1  10   20    adjustZoomBy in->
        //inverted                   1  0.1  0.05
        // tick size in m <-adjustZoomBy out 10 1    1     adjustZoomBy in->

        int tickSizeInMetres = getMinorGridBoxSize();
        double boxSize = 10;

        int numberTicksJustBeforeViewpointOffsetX = (int)(viewpointOffset.x / tickSizeInMetres);

        for (int n = numberTicksJustBeforeViewpointOffsetX; true; n++) {
            double xSurvey = n * tickSizeInMetres;
            int xView = (int)((xSurvey - viewpointOffset.x) * surveyToViewScale);
            gridPaint.setStrokeWidth(n % boxSize == 0 ? 3 : 1);
            canvas.drawLine(xView, 0, xView, getHeight(), gridPaint);
            if (xView >= getWidth()) {
                break;
            }
        }

        int numberTicksJustBeforeViewpointOffsetY = (int)(viewpointOffset.y / tickSizeInMetres);

        for (int n = numberTicksJustBeforeViewpointOffsetY; true; n++) {
            double ySurvey = n * tickSizeInMetres;
            int yView = (int)((ySurvey - viewpointOffset.y) * surveyToViewScale);
            gridPaint.setStrokeWidth(n % boxSize == 0 ? 3 : 1);
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
            Canvas canvas, Set<CrossSectionDetail> crossSectionDetails, int alpha) {

        boolean showStationLabels =
                getDisplayPreference(GraphActivity.DisplayPreference.SHOW_STATION_LABELS);

        crossSectionConnectorPaint.setAlpha(alpha);

        List<CrossSectionDetail> badXSections = new LinkedList<>();

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
            drawStationCross(canvas, stationPaint,
                    (float) centreOnView.x, (float) centreOnView.y,
                    STATION_DIAMETER, alpha);

            String description =
                    sectionDetail.getCrossSection().getStation().getName() + " X";
            if (showStationLabels) {
                stationPaint.setAlpha(alpha);
                canvas.drawText(description,
                        (float) centreOnView.x,
                        (float) centreOnView.y,
                        stationPaint);
            }

            Space<Coord2D> projection = sectionDetail.getProjection();

            drawLegs(canvas, projection, alpha);

            Coord2D viewStationLocation = surveyCoordsToViewCoords(surveyStationLocation);
            drawLineAsPath(
                    canvas, viewStationLocation, centreOnView, crossSectionConnectorPaint);
        }

        for (CrossSectionDetail crossSectionDetail : badXSections) {
            Station station = crossSectionDetail.getCrossSection().getStation();
            String name = station == null? "Unknown" : station.getName();
            Log.e("Missing station details for cross section on station " + name + "; removing");
            crossSectionDetails.remove(crossSectionDetail);
        }
    }


    private void drawLegs(Canvas canvas, Space<Coord2D> space, int baseAlpha) {

        boolean showSplays = getDisplayPreference(GraphActivity.DisplayPreference.SHOW_SPLAYS);
        boolean highlightLatestLeg =
                PreferenceAccess.getBoolean(
                        getContext(), "pref_key_highlight_latest_leg", true);

        boolean fadingNonActive =
                getDisplayPreference(GraphActivity.DisplayPreference.FADE_NON_ACTIVE);

        Map<Leg, Line<Coord2D>> legMap = space.getLegMap();

        for (Leg leg : legMap.keySet()) {

            if (!showSplays && !leg.hasDestination()) {
                continue;
            }

            Line<Coord2D> line = legMap.get(leg);

            Coord2D start = surveyCoordsToViewCoords(line.getStart());
            Coord2D end = surveyCoordsToViewCoords(line.getEnd());

            if (!isLineOnCanvas(start, end)) {
                continue;
            }

            boolean fade = baseAlpha == FADED_ALPHA || (fadingNonActive && !isAttachedToActive(leg));

            Paint paint;
            if (!leg.hasDestination()) {
                paint = fade ? fadedSplayPaint : splayPaint;
            } else if (highlightLatestLeg && survey.getMostRecentLeg() == leg) {
                paint = fade ? fadedLatestLegPaint : latestLegPaint;
            } else {
                paint = fade ? fadedLegPaint : legPaint;
            }

            if (projectionType.isLegInPlane(leg)) {
                canvas.drawLine((float)start.x, (float)start.y, (float)end.x, (float)end.y, paint);
			} else {
                paint.setPathEffect(dashPathEffect);
                drawLineAsPath(canvas, start, end, paint);
                paint.setStyle(Paint.Style.STROKE);
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

        boolean fadingNonActive =
                getDisplayPreference(GraphActivity.DisplayPreference.FADE_NON_ACTIVE);

        if (fadingNonActive) {
            baseAlpha = FADED_ALPHA;
        }

        int alpha = baseAlpha;
        stationPaint.setAlpha(alpha);

        boolean showStationLabels =
                getDisplayPreference(GraphActivity.DisplayPreference.SHOW_STATION_LABELS);

        int crossDiameter =
                PreferenceAccess.getInt(this.getContext(), "pref_station_diameter", CROSS_DIAMETER);

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

            drawStationCross(canvas, stationPaint, x, y, crossDiameter, alpha);

            if (station == survey.getActiveStation()) {
                highlightActiveStation(canvas, x, y);
            }

            int spacing = crossDiameter / 2;
            int nextX = x + crossDiameter;

            if (showStationLabels) {
                String name = station.getName();
                if (station == survey.getOrigin()) {
                    name = name + " (" + survey.getName() + ")";
                }
                canvas.drawText(name,
                        nextX,
                        y + STATION_LABEL_OFFSET,
                        stationPaint);
                nextX += stationPaint.measureText(name) + spacing;
            }

            List<Bitmap> icons = new LinkedList<>();
            if (station.hasComment()) {
                icons.add(commentIcon);
            }
            if (survey.hasLinkedSurveys(station)) {
                icons.add(linkIcon);
            }

            for (Bitmap icon : icons) {int yTop = y - crossDiameter / 2;
                Rect rect = new Rect(nextX, yTop, nextX + crossDiameter, yTop + crossDiameter);
                canvas.drawBitmap(icon, null, rect, stationPaint);
                nextX += crossDiameter + spacing;
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
        float indicatorWidth = (float)(1 * surveyToViewScale);
        float startX = x - ((indicatorWidth / 2) * (float)Math.cos(angle));
        float startY = y - ((indicatorWidth / 2) * (float)Math.sin(angle));
        float endX = x + ((indicatorWidth / 2) * (float)Math.cos(angle));
        float endY = y + ((indicatorWidth / 2) * (float)Math.sin(angle));

        canvas.drawLine(startX, startY, endX, endY, crossSectionIndicatorPaint);

        float lineLength = (float)Space2DUtils.getDistance(
                new Coord2D(startX, startY), new Coord2D(endX, endY));
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


    private void drawStationCross(Canvas canvas, Paint paint, float x, float y, int crossDiameter, int alpha) {
        paint.setAlpha(alpha);
        canvas.drawLine(x , y - crossDiameter / 2, x, y + crossDiameter / 2, paint);
        canvas.drawLine(x - crossDiameter / 2, y, x + crossDiameter / 2, y, paint);
    }


    public boolean getDisplayPreference(GraphActivity.DisplayPreference preference) {
        SharedPreferences preferences =
            getContext().getSharedPreferences("display", Context.MODE_PRIVATE);
        boolean isSelected =
            preferences.getBoolean(preference.toString(), preference.getDefault());
        return isSelected;
    }


    private void drawSketch(Canvas canvas, Sketch sketch, int alpha) {

        if (!getDisplayPreference(GraphActivity.DisplayPreference.SHOW_SKETCH)) {
            return;
        }

        Colour lastColour = Colour.BLACK;

        drawPaint.setColor(lastColour.intValue);
        drawPaint.setAlpha(alpha);

        for (PathDetail pathDetail : sketch.getPathDetails()) {

            if (!couldBeOnScreen(pathDetail)) {
                continue;
            }

            // Avoiding constantly updating the paint colour saves approx. 10% of sketch draw time.
            // Ideally getPathDetails() would return the paths in colour order but HashSets
            // are unordered collections
            if (pathDetail.getColour() != lastColour) {
                lastColour = pathDetail.getColour();
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
                    fromX = (float)((point.x - viewpointOffset.x) * surveyToViewScale);
                    fromY = (float)((point.y - viewpointOffset.y) * surveyToViewScale);
                } else {
                    //Coord2D to = surveyCoordsToViewCoords(point);
                    float toX = (float)((point.x - viewpointOffset.x) * surveyToViewScale);
                    float toY = (float)((point.y - viewpointOffset.y) * surveyToViewScale);

                    lines[lineIndex++] = fromX;
                    lines[lineIndex++] = fromY;
                    lines[lineIndex++] = toX;
                    lines[lineIndex++] = toY;

                    fromX = toX;
                    fromY = toY;
                }
            }

            canvas.drawLines(lines, drawPaint);
        }

        labelPaint.setAlpha(alpha);

        for (TextDetail textDetail : sketch.getTextDetails()) {
            Coord2D location = surveyCoordsToViewCoords(textDetail.getPosition());
            float x = (float)location.x;
            float y = (float)location.y;
            String text = textDetail.getText();
            labelPaint.setColor(textDetail.getColour().intValue);
            labelPaint.setTextSize((float)(textDetail.getSize() * surveyToViewScale));
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
            Bitmap bitmap = symbolDetail.getSymbol().getBitmap();

            int size = (int)(symbolDetail.getSize() * surveyToViewScale);
            if (size == 0) {
                continue;
            }

            bitmap = Bitmap.createScaledBitmap(bitmap, size, size, true);
            float x = (float)location.x;
            float y = (float)location.y;
            canvas.drawBitmap(bitmap, x, y, labelPaint);
        }
    }


    private void drawLegend(Canvas canvas) {

        String surveyLabel =
            survey.getName() +
            " L" + TextTools.formatTo0dpWithComma(surveyLength) +
            " H" + TextTools.formatTo0dpWithComma(surveyHeight);

        float offsetX = getWidth() * 0.03f;
        float offsetY = getHeight() - LEGEND_SIZE * 2;
        canvas.drawText(surveyLabel, offsetX, offsetY, legendPaint);

        int minorGridSize = getMinorGridBoxSize();
        float scaleWidth = (float)surveyToViewScale * minorGridSize;
        float scaleOffsetY = getHeight() - (LEGEND_SIZE * 4);
        canvas.drawLine(offsetX, scaleOffsetY, offsetX + scaleWidth, scaleOffsetY, legendPaint);
        final float TICK_SIZE = 5;
        canvas.drawLine(offsetX, scaleOffsetY, offsetX, scaleOffsetY - TICK_SIZE, legendPaint);
        canvas.drawLine(offsetX + scaleWidth, scaleOffsetY, offsetX + scaleWidth, scaleOffsetY - TICK_SIZE, legendPaint);
        String scaleLabel = minorGridSize + "m";
        canvas.drawText(scaleLabel, offsetX + scaleWidth + 5, scaleOffsetY, legendPaint);

    }


    private void drawHotCorners(Canvas canvas) {

        if (!activity.getBooleanPreference("pref_hot_corners")) {
            return;
        }

        if (currentSketchTool == SketchTool.MODAL_MOVE) {
            hotCornersPaint.setColor(Colour.YELLOW.intValue);
            hotCornersPaint.setAlpha(FADED_ALPHA);
        }

        if (topLeftCorner == null || topRightCorner == null || bottomRightCorner == null) {
            final int side = (int) (HOT_CORNER_DISTANCE_PROPORTION * getWidth());
            topLeftCorner =
                new Rect(0, 0, side, side);
            topRightCorner =
                new Rect(getWidth() - side, 0, getWidth(), side);
            bottomRightCorner =
                new Rect(getWidth() - side, getHeight() - side, getWidth(), getHeight());
        }

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

        double xDeltaInMetres = ((double)getWidth() / 2) / surveyToViewScale;
        double yDeltaInMetres = ((double)getHeight() / 2) / surveyToViewScale;

        double x = point.x - xDeltaInMetres;
        double y = point.y - yDeltaInMetres;

        viewpointOffset = new Coord2D(x, y);
    }


    private void drawLine(Canvas canvas, Coord2D start, Coord2D end, Paint paint, int alpha) {
        paint.setAlpha(alpha);
        canvas.drawLine(
                (float)(start.x), (float)(start.y),
                (float)(end.x), (float)(end.y),
                paint);
    }


    /**
     * Drawing as a path is useful in order to apply path effects (buggy when drawing lines)
     */
    private void drawLineAsPath(Canvas canvas, Coord2D start, Coord2D end, Paint paint) {
        dashedPath.reset();
        dashedPath.moveTo((float)(start.x), (float)(start.y));
        dashedPath.lineTo((float)(end.x), (float)(end.y));
        canvas.drawPath(dashedPath, paint);
    }


    public void adjustZoomBy(double delta) {
        double newZoom = surveyToViewScale * delta;
        setZoom(newZoom);
    }

    public void setZoom(double newZoom) {
        Coord2D centre = new Coord2D((double) getWidth() / 2, (double) getHeight() / 2);
        setZoom(newZoom, centre);
    }

    public void setZoom(double newZoom, Coord2D focusOnScreen) {

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


    public void setCachedStats(double surveyLength, double surveyHeight) {
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

            double x = detector.getFocusX();
            double y = detector.getFocusY();
            Coord2D focus = new Coord2D(x, y);

            double scaleFactor = detector.getScaleFactor();
            setZoom(surveyToViewScale * scaleFactor, focus);

            invalidate();
            return true;
        }
    }

    private class LongPressListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public void onLongPress(MotionEvent motionEvent) {
            Coord2D touchPointOnView = new Coord2D(motionEvent.getX(), motionEvent.getY());
            Station newSelectedStation = checkForStation(touchPointOnView);
            if (newSelectedStation != null) {
                showContextMenu(motionEvent, newSelectedStation);
            }
        }
    }



}

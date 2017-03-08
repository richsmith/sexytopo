package org.hwyl.sexytopo.control.graph;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.PopupWindow;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.activity.GraphActivity;
import org.hwyl.sexytopo.control.util.CrossSectioner;
import org.hwyl.sexytopo.control.util.PreferenceAccess;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.control.util.SpaceFlipper;
import org.hwyl.sexytopo.control.util.SurveyStats;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.sketch.CrossSectionDetail;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.SketchDetail;
import org.hwyl.sexytopo.model.sketch.TextDetail;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.SurveyConnection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class GraphView extends View {

    // The offset of the viewing window (what can be seen on the screen) from the whole survey
    private Coord2D viewpointOffset = Coord2D.ORIGIN;

    // These variables are used in handling the dragging of the viewing window
    private Coord2D actionDownPointOnView = Coord2D.ORIGIN;
    private Coord2D actionDownViewpointOffset = Coord2D.ORIGIN;

    // ratio of metres on the survey to pixels on the view
    // zoom in increases this, zooming out decreases it
    private double surveyToViewScale = 60.0; // 10 pixels is one metre

    public static final double MAX_ZOOM = 120.0;

    private GraphActivity activity;

    private Survey survey;
    private Space<Coord2D> projection;
    private Sketch sketch;

    private Map<Survey, Space<Coord2D>> translatedConnectedSurveys = new HashMap<>();


    public static final Colour LEG_COLOUR = Colour.RED;
	public static final Colour SPLAY_COLOUR = Colour.PINK;
    public static final Colour LATEST_LEG_COLOUR = Colour.MAGENTA;
    public static final Colour HIGHLIGHT_COLOUR = Colour.GOLD;
    public static final Colour DEFAULT_SKETCH_COLOUR = Colour.BLACK;

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


    public static final int STATION_LABEL_OFFSET = 10;



    public enum BrushColour {

        BLACK(R.id.buttonBlack, Colour.BLACK),
        BROWN(R.id.buttonBrown, Colour.BROWN),
        ORANGE(R.id.buttonOrange, Colour.ORANGE),
        GREEN(R.id.buttonGreen, Colour.GREEN),
        BLUE(R.id.buttonBlue, Colour.BLUE),
        PURPLE(R.id.buttonPurple, Colour.PURPLE);

        private final int id;
        private final Colour colour;
        BrushColour(int id, Colour colour) {
            this.id = id;
            this.colour = colour;
        }

        public int getId() {
            return id;
        }
    }

    public enum SketchTool {
        MOVE(R.id.buttonMove),
        DRAW(R.id.buttonDraw, true),
        ERASE(R.id.buttonErase),
        TEXT(R.id.buttonText, true),
        SELECT(R.id.buttonSelect),
        POSITION_CROSS_SECTION(R.id.graph_station_new_cross_section);

        private int id;
        private boolean usesColour = false;

        SketchTool(int id) {
            this.id = id;
        }

        SketchTool(int id, boolean usesColour) {
            this.id = id;
            this.usesColour = usesColour;
        }

        public int getId() {
            return id;
        }

        public boolean usesColour() {
            return usesColour;
        }
    }
    public SketchTool currentSketchTool = SketchTool.MOVE;
    // used to jump back to the previous tool when using one-use tools
    private SketchTool previousSketchTool = SketchTool.SELECT;

    private Paint stationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint legPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint drawPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint legendPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint gridPaint = new Paint();



    public GraphView(Context context) {
        super(context);
        initialise();
    }

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialise();
    }

    public void initialise() {

        stationPaint.setColor(STATION_COLOUR);
        stationPaint.setStrokeWidth(STATION_STROKE_WIDTH);
        int labelSize = PreferenceAccess.getInt(getContext(), "pref_station_label_font_size", 22);
        stationPaint.setTextSize(labelSize);

        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setStrokeWidth(HIGHLIGHT_OUTLINE);
        highlightPaint.setColor(HIGHLIGHT_COLOUR.intValue);

        legPaint.setARGB(127, 255, 0, 0);
        int legStrokeWidth = PreferenceAccess.getInt(getContext(), "pref_leg_width", 3);
        legPaint.setStrokeWidth(legStrokeWidth);
        legPaint.setColor(LEG_COLOUR.intValue);

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
    }


    public void setActivity(GraphActivity graphActivity) {
        this.activity = graphActivity;
    }


    public void setSurvey(Survey survey) {
        if (survey != this.survey) {
            this.survey = survey;
            centreViewOnActiveStation();

        }
    }


    public void setProjection(Space<Coord2D> projection) {
        // We're going to flip the projection vertically because we want North at the top and
        // the survey is recorded assuming that that is the +ve axis. However, on the
        // screen this is reversed: DOWN is the +ve access. I think this makes sense...
        // we just have to remember to reverse the flip when exporting the sketch :)
        this.projection = SpaceFlipper.flipVertically(projection);
    }


    public void setSketch(Sketch sketch) {
        this.sketch = sketch;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (currentSketchTool) {
            case MOVE:
                return handleMove(event);
            case DRAW:
                return handleDraw(event);
            case ERASE:
                return handleErase(event);
            case TEXT:
                return handleText(event);
            case SELECT:
                return handleSelect(event);
            case POSITION_CROSS_SECTION:
                return handlePositionCrossSection(event);
        }
        return false;
    }


    private Coord2D viewCoordsToSurveyCoords(Coord2D coords) {
        // The more elegant way to do this is:
        // return coords.scale(1 / surveyToViewScale).plus(viewpointOffset);
        // ...but this method gets hit hard (profiled) so let's avoid creating intermediate objects:
        return new Coord2D(((coords.getX() * (1 / surveyToViewScale)) + viewpointOffset.getX()),
                ((coords.getY() * (1 / surveyToViewScale)) + viewpointOffset.getY()));
    }


    private Coord2D surveyCoordsToViewCoords(Coord2D coords) {
        // The more elegant way to do this is:
        // return coords.minus(viewpointOffset).scale(surveyToViewScale);
        // ...but this method gets hit hard (profiled) so let's avoid creating intermediate objects:
        return new Coord2D(((coords.getX() - viewpointOffset.getX()) * surveyToViewScale),
                ((coords.getY() - viewpointOffset.getY()) * surveyToViewScale));
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
                sketch.getActivePath().lineTo(surveyCoords);
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

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                /*SketchDetail closestDetail =
                        sketch.findNearestPathWithin(sketch.getPathDetails(),
                                touchPointOnSurvey, DELETE_PATHS_WITHIN_N_PIXELS);*/
                SketchDetail closestDetail = sketch.findNearestDetailWithin(
                        touchPointOnSurvey, DELETE_PATHS_WITHIN_N_PIXELS);
                if (closestDetail != null) {
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
                                sketch.addTextDetail(touchPointOnSurvey, input.getText().toString());
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
        Coord2D touchPointOnSurvey = viewCoordsToSurveyCoords(touchPointOnView);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                double selectionTolerance =
                        SELECTION_SENSITIVITY_IN_PIXELS / surveyToViewScale;

                Station newSelectedStation = findNearestStationWithinDelta(projection,
                        touchPointOnSurvey, selectionTolerance);

                if (newSelectedStation == null) {
                    return true;

                } else if (newSelectedStation != survey.getActiveStation()) {
                    survey.setActiveStation(newSelectedStation);
                    invalidate();
                    return true;

                } else { // double selection opens context menu
                    showContextMenu(event, newSelectedStation);
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
                    case R.id.graph_station_reverse:
                        SurveyUpdater.reverseLeg(survey, station);
                        SurveyManager.getInstance(getContext()).broadcastSurveyUpdated();
                        invalidate();
                        break;
                    case R.id.graph_station_delete:
                        askAboutDeletingStation(station);
                        invalidate();
                        break;
                    case R.id.graph_station_new_cross_section:
                        setSketchTool(SketchTool.POSITION_CROSS_SECTION);
                        activity.showSimpleToast(R.string.position_cross_section_instruction);
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

        PopupWindow menu = StationContextMenu.getFakeStationContextMenu(
                getContext(), station, listener);

        boolean hasLinkedSurveys = survey.getConnectedSurveys().containsKey(station);
        View unlinkSurveyButton = menu.getContentView().findViewById(R.id.graph_station_unlink_survey);
        unlinkSurveyButton.setEnabled(hasLinkedSurveys);

        menu.showAtLocation(this, Gravity.LEFT | Gravity.TOP,
                (int) (event.getX()), (int) (event.getY()));
    }


    private void askAboutDeletingStation(final Station station) {
        int legsToBeDeleted = SurveyStats.calcNumberSubLegs(station);
        int stationsToBeDeleted = SurveyStats.calcNumberSubStations(station);
        String message = "This will delete\n" +
                TextTools.pluralise(legsToBeDeleted, "leg") +
                " and " + TextTools.pluralise(stationsToBeDeleted, "station");
        new AlertDialog.Builder(getContext())
                .setMessage(message)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        survey.deleteStation(station);
                        SurveyManager.getInstance(getContext()).broadcastSurveyUpdated();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }


    private static Station findNearestStationWithinDelta(Space<Coord2D> space, Coord2D target, double delta) {
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


    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        if (getDisplayPreference(GraphActivity.DisplayPreference.SHOW_GRID)) {
            drawGrid(canvas);
        }

        if (getDisplayPreference(GraphActivity.DisplayPreference.SHOW_CONNECTIONS)) {
            drawConnectedSurveys(canvas, projection, 50);
        }

        drawSurvey(canvas, survey, projection, 255);

        drawLegend(canvas, survey);
    }

    private void drawSurvey(Canvas canvas, Survey survey, Space<Coord2D> projection, int alpha) {

        drawSketch(canvas, activity.getSketch(survey), alpha);

        drawCrossSections(canvas, sketch.getCrossSectionDetails(), alpha);

        drawSurveyData(survey, canvas, projection, alpha);
    }


    private void drawConnectedSurveys(Canvas canvas, Space<Coord2D> projection, int alpha) {

        if (doTranslatedConnectedSurveysNeedUpdating()) {
            this.translatedConnectedSurveys =
                    ConnectedSurveys.getTranslatedConnectedSurveys(activity, survey, projection);
        }

        for (Survey translatedConnectedSurvey : translatedConnectedSurveys.keySet()) {
            Space<Coord2D> connectedProjection =
                        translatedConnectedSurveys.get(translatedConnectedSurvey);
            drawSurvey(canvas, translatedConnectedSurvey, connectedProjection, alpha);
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
        //scale           <-zoom out 1  10   20    zoom in->
        //inverted                   1  0.1  0.05
        // tick size in m <-zoom out 10 1    1     zoom in->
        int tickSizeInMetres = (1 / (int)roundDown(surveyToViewScale, 10)) * 10;
        tickSizeInMetres = Math.max(1, tickSizeInMetres);

        int numberTicksJustBeforeViewpointOffsetX = (int)(viewpointOffset.getX() / tickSizeInMetres);

        for (int n = numberTicksJustBeforeViewpointOffsetX; true; n++) {
            double xSurvey = n * tickSizeInMetres;
            int xView = (int)((xSurvey - viewpointOffset.getX()) * surveyToViewScale);
            gridPaint.setStrokeWidth(n % 10 == 0 ? 3 : 1);
            canvas.drawLine(xView, 0, xView, getHeight(), gridPaint);
            if (xView >= getWidth()) {
                break;
            }
        }

        int numberTicksJustBeforeViewpointOffsetY = (int)(viewpointOffset.getY() / tickSizeInMetres);

        for (int n = numberTicksJustBeforeViewpointOffsetY; true; n++) {
            double ySurvey = n * tickSizeInMetres;
            int yView = (int)((ySurvey - viewpointOffset.getY()) * surveyToViewScale);
            gridPaint.setStrokeWidth(n % 10 == 0 ? 3 : 1);
            canvas.drawLine(0, yView, getWidth(), yView, gridPaint);
            if (yView >= getHeight()) {
                break;
            }
        }


    }

    private static double roundDown(double value, double factor) {
        // FIXME, this is crazy code
        double total = 1;
        for (int i = 0; true; i++) {

            if ((total * factor) > value) {
                return total;
            } else {
                total *= factor;
            }
        }
    }

    private void drawSurveyData(Survey survey, Canvas canvas, Space<Coord2D> space, int alpha) {

        drawLegs(canvas, space, alpha);

        drawStations(survey, canvas, space, alpha);
    }


    private void drawCrossSections(Canvas canvas, Set<CrossSectionDetail> crossSectionDetails, int alpha) {
        for (CrossSectionDetail sectionDetail : crossSectionDetails) {
            Coord2D centreOnSurvey = sectionDetail.getPosition();
            Coord2D centreOnView = surveyCoordsToViewCoords(centreOnSurvey);
            drawStation(canvas, stationPaint,
                    (float) centreOnView.getX(), (float) centreOnView.getY(),
                    STATION_DIAMETER, alpha);

            String description =
                    sectionDetail.getCrossSection().getStation().getName() + " X-section";
            if (getDisplayPreference(GraphActivity.DisplayPreference.SHOW_STATION_LABELS)) {
                stationPaint.setAlpha(alpha);
                canvas.drawText(description,
                        (float) centreOnView.getX(),
                        (float) centreOnView.getY(),
                        stationPaint);
            }

            Space<Coord2D> projection = sectionDetail.getProjection();


            drawLegs(canvas, projection, alpha);
        }
    }


    private void drawLegs(Canvas canvas, Space<Coord2D> space, int alpha) {

        Map<Leg, Line<Coord2D>> legMap = space.getLegMap();

        for (Leg leg : legMap.keySet()) {

            if (!getDisplayPreference(GraphActivity.DisplayPreference.SHOW_SPLAYS) &&
                    !leg.hasDestination()) {
                continue;
            }
            Line<Coord2D> line = legMap.get(leg);

            Coord2D start = surveyCoordsToViewCoords(line.getStart());
            Coord2D end = surveyCoordsToViewCoords(line.getEnd());

            if (PreferenceAccess.getBoolean(getContext(), "pref_key_highlight_latest_leg", true)
                    && survey.getMostRecentLeg() == leg) {
                legPaint.setColor(LATEST_LEG_COLOUR.intValue);
            } else if (!leg.hasDestination()) {
				legPaint.setColor(SPLAY_COLOUR.intValue);
			} else {
				legPaint.setColor(LEG_COLOUR.intValue);
			}

            legPaint.setAlpha(alpha);

            boolean legIsHorizontalish = -45 < leg.getInclination() && leg.getInclination() < 45;
			if (legIsHorizontalish) {
				drawPaint.setStyle(Paint.Style.STROKE);
			} else {
				//drawPaint.setStyle(Paint.Style.DOTTED);
			}

            canvas.drawLine(
                (float)(start.getX()), (float)(start.getY()),
                (float)(end.getX()), (float)(end.getY()),
                legPaint);

        }

    }


    private void drawStations(Survey survey, Canvas canvas, Space<Coord2D> space, int alpha) {

        stationPaint.setAlpha(alpha);

        int crossDiameter =
                PreferenceAccess.getInt(this.getContext(), "pref_station_diameter", CROSS_DIAMETER);

        for (Map.Entry<Station, Coord2D> entry : space.getStationMap().entrySet()) {
            Station station = entry.getKey();
            Coord2D translatedStation = surveyCoordsToViewCoords(entry.getValue());

            int x = (int)(translatedStation.getX());
            int y = (int)(translatedStation.getY());

            drawStation(canvas, stationPaint, x, y, crossDiameter, alpha);

            if (station == survey.getActiveStation()) {
                highlightActiveStation(canvas, x, y);
            }

            if (getDisplayPreference(GraphActivity.DisplayPreference.SHOW_STATION_LABELS)) {
                String name = station.getName();
                if (station == survey.getOrigin()) {
                    name = name + " (" + survey.getName() + ")";
                }
                canvas.drawText(name,
                        x + STATION_LABEL_OFFSET,
                        y + STATION_LABEL_OFFSET,
                        stationPaint);
            }

        }
    }


    private void highlightActiveStation(Canvas canvas, float x, float y) {

        int diameter = 22;
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


    private void drawStation(Canvas canvas, Paint paint, float x, float y, int crossDiameter, int alpha) {
        paint.setAlpha(alpha);
        canvas.drawLine(x , y - crossDiameter / 2, x, y + crossDiameter / 2, paint);
        canvas.drawLine(x - crossDiameter / 2, y, x + crossDiameter / 2, y, paint);
    }


    private boolean getDisplayPreference(GraphActivity.DisplayPreference preference) {
        SharedPreferences preferences =
            getContext().getSharedPreferences("display", Context.MODE_PRIVATE);
        boolean isSelected =
            preferences.getBoolean(preference.toString(), preference.getDefault());
        return isSelected;
    }


    private void drawSketch(Canvas canvas, Sketch sketch, int alpha) {

        for (PathDetail pathDetail : sketch.getPathDetails()) {

            if (!couldBeOnScreen(pathDetail)) {
                continue;
            }

            List<Coord2D> path = pathDetail.getPath();
            Coord2D from = null;
            for (Coord2D point : path) {
                if (from == null) {
                    from = surveyCoordsToViewCoords(point);
                    continue;
                } else {
                    Coord2D to = surveyCoordsToViewCoords(point);
                    drawPaint.setColor(pathDetail.getColour().intValue);
                    drawPaint.setAlpha(alpha);

                    canvas.drawLine(
                            (float) from.getX(), (float) from.getY(),
                            (float) to.getX(), (float) to.getY(),
                            drawPaint);
                    from = to;
                }
            }
        }

        for (TextDetail textDetail : sketch.getTextDetails()) {
            Coord2D location = surveyCoordsToViewCoords(textDetail.getPosition());
            String text = textDetail.getText();
            labelPaint.setColor(textDetail.getColour().intValue);
            canvas.drawText(text, (float)location.getX(), (float)location.getY(), labelPaint);
        }
    }


    private void drawLegend(Canvas canvas, Survey survey) {

        String surveyLabel =
            survey.getName() +
            " L" + TextTools.formatTo0dpWithComma(SurveyStats.calcTotalLength(survey)) +
            " H" + TextTools.formatTo0dpWithComma(SurveyStats.calcHeightRange(survey));

        float offsetX = getWidth() * 0.03f;
        float offsetY = getHeight() - LEGEND_SIZE * 2;
        canvas.drawText(surveyLabel, offsetX, offsetY, legendPaint);

        float scaleWidth = (float)surveyToViewScale * 1;
        float scaleOffsetY = getHeight() - (LEGEND_SIZE * 4);
        canvas.drawLine(offsetX, scaleOffsetY, offsetX + scaleWidth, scaleOffsetY, legendPaint);
        final float TICK_SIZE = 5;
        canvas.drawLine(offsetX, scaleOffsetY, offsetX, scaleOffsetY - TICK_SIZE, legendPaint);
        canvas.drawLine(offsetX + scaleWidth, scaleOffsetY, offsetX + scaleWidth, scaleOffsetY - TICK_SIZE, legendPaint);
        canvas.drawText("1m", offsetX + scaleWidth + 5, scaleOffsetY, legendPaint);

    }


    private boolean couldBeOnScreen(SketchDetail sketchDetail) {
        Coord2D topLeft = viewCoordsToSurveyCoords(Coord2D.ORIGIN);
        Coord2D bottomRight = viewCoordsToSurveyCoords(new Coord2D(getWidth(), getHeight()));
        return sketchDetail.intersectsRectangle(topLeft, bottomRight);
    }


    public void centreViewOnActiveStation() {
        // call this in a post thread so we can ask for the view to be centred even before the
        // view is fully drawn
        post(new Runnable() {
            @Override
            public void run() {
                Coord2D activeStationCoord =
                        projection.getStationMap().get(survey.getActiveStation());
                centreViewOnSurveyPoint(activeStationCoord);
            }
        });
    }


    public void centreViewOnSurveyPoint(Coord2D point) {

        double xDeltaInMetres = ((double)getWidth() / 2) / surveyToViewScale;
        double yDeltaInMetres = ((double)getHeight() / 2) / surveyToViewScale;

        double x = point.getX() - xDeltaInMetres;
        double y = point.getY() - yDeltaInMetres;

        viewpointOffset = new Coord2D(x, y);
    }


    public void zoom(double delta) {

        double newZoom = surveyToViewScale + delta;

        if (0 >= newZoom || newZoom >= MAX_ZOOM) {
            return;
        }

        // first record where we are
        Coord2D centre = new Coord2D((double) getWidth() / 2, (double) getHeight() / 2);
        Coord2D centreInSurveyCoords = viewCoordsToSurveyCoords(centre);

        // then perform the actual zoom
        surveyToViewScale = newZoom;

        centreViewOnSurveyPoint(centreInSurveyCoords);
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
        sketch.setActiveColour(brushColour.colour);
    }


    public void setSketchTool(SketchTool sketchTool) {
        this.currentSketchTool = sketchTool;
    }


    public SketchTool getSketchTool() {
        return currentSketchTool;
    }


}

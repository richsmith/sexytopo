package org.hwyl.sexytopo.control.graph;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.activity.GraphActivity;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.control.util.SpaceFlipper;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.SketchDetail;
import org.hwyl.sexytopo.model.sketch.TextDetail;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.Map;

/**
 * Created by rls on 27/07/14.
 */
public class GraphView extends View implements PopupMenu.OnMenuItemClickListener {

    // FIXME hack!
private boolean firstTime = true;

    // The offset of the viewing window (what can be seen on the screen) from the whole survey
    private Coord2D viewpointOffset = Coord2D.ORIGIN;

    // These variables are used in handling the dragging of the viewing window
    private Coord2D actionDownPointOnView = Coord2D.ORIGIN;
    private Coord2D actionDownViewpointOffset = Coord2D.ORIGIN;

    private Station selectedStation;

    // ratio of metres on the survey to pixels on the view
    // zoom in increases this, zooming out decreases it
    private double surveyToViewScale = 60.0; // 10 pixels is one metre

    public static final double MAX_ZOOM = 120.0;

    private Survey survey;
    private Space<Coord2D> projection;
    private Sketch sketch;


    public static final int LEG_COLOUR = Color.RED;
    public static final int LEG_STROKE_WIDTH = 4;
    public static final int HIGHLIGHT_COLOUR = Color.YELLOW;
    public static final int DEFAULT_SKETCH_COLOUR = Color.BLACK;

    public static final int STATION_COLOUR = Color.RED;
    public static final int STATION_DIAMETER = 8;
    public static final int HIGHLIGHT_DIAMETER = 12;
    public static final int STATION_LABEL_SIZE = 20;

    public static final int LABEL_SIZE = 34;

    public static final int GRID_COLOUR = Color.LTGRAY;

    public static final double DELETE_PATHS_WITHIN_N_PIXELS = 5.0;
    public static final double SELECTION_SENSITIVITY_IN_PIXELS = 25.0;
    public static final double SNAP_TO_LINE_SENSITIVITY_IN_PIXELS = 25.0;

    private static final int SKETCH_BROWN = 0xFFA52A2A;
    private static final int SKETCH_ORANGE = 0xFFFFA500;
    private static final int SKETCH_GREEN = 0xFF00DD00;


    public static final int STATION_LABEL_OFFSET = 10;

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.setActiveStation:
                if (selectedStation != null) {
                    survey.setActiveStation(selectedStation);
                    invalidate();
                }
                return true;
        }

        return false;
    }


    public enum BrushColour {

        BLACK(R.id.buttonBlack, Color.BLACK),
        BROWN(R.id.buttonBrown, SKETCH_BROWN),
        ORANGE(R.id.buttonOrange, SKETCH_ORANGE),
        GREEN(R.id.buttonGreen, SKETCH_GREEN),
        BLUE(R.id.buttonBlue, Color.BLUE),
        PURPLE(R.id.buttonPurple, Color.MAGENTA);

        private final int id;
        private final int colour;
        BrushColour(int id, int colour) {
            this.id = id;
            this.colour = colour;
        }

        public int getId() {
            return id;
        }
    }

    public enum SketchTool {
        MOVE(R.id.buttonMove),
        DRAW(R.id.buttonDraw),
        ERASE(R.id.buttonErase),
        TEXT(R.id.buttonText),
        SELECT(R.id.buttonSelect);

        private int id;
        SketchTool(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }
    public SketchTool currentSketchTool = SketchTool.MOVE;

    private Paint stationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint legPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint drawPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint gridPaint = new Paint();



    public GraphView(Context context) {
        super(context);
        initialise();
    }

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialise();
    }

    private void initialise() {

        stationPaint.setColor(STATION_COLOUR);
        stationPaint.setTextSize(STATION_LABEL_SIZE);

        //highlightPaint.setStyle(Paint.Style.STROKE);
        //highlightPaint.setStrokeWidth(HIGHLIGHT_STROKE_WIDTH);
        highlightPaint.setColor(HIGHLIGHT_COLOUR);

        legPaint.setARGB(127, 255, 0, 0);
        legPaint.setStrokeWidth(LEG_STROKE_WIDTH);
        legPaint.setColor(LEG_COLOUR);

        gridPaint.setColor(GRID_COLOUR);

        drawPaint.setColor(DEFAULT_SKETCH_COLOUR);
        drawPaint.setStrokeWidth(3);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        labelPaint.setColor(STATION_COLOUR);
        labelPaint.setTextSize(LABEL_SIZE);
    }


    public void setSurvey(Survey survey) {
        this.survey = survey;
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
        }
        return false;
    }




    private Coord2D viewCoordsToSurveyCoords(Coord2D coords) {
        return coords.scale(1 / surveyToViewScale).plus(viewpointOffset);
    }

    private Coord2D surveyCoordsToViewCoords(Coord2D coords) {
        return coords.minus(viewpointOffset).scale(surveyToViewScale);
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

                selectedStation = findNearestStationWithinDelta(projection,
                        touchPointOnSurvey, selectionTolerance);

                if (selectedStation == null) {
                    return true;
                } else if (true) {
                    // forget the pop-up menu for now... just select what we're pointing at
                    survey.setActiveStation(selectedStation);
                    invalidate();
                    return true;
                }

                LinearLayout ll = new LinearLayout(this.getContext());
                Button dummyView = new Button(getContext());
                ll.addView(dummyView);
                dummyView.setText("foo");
                PopupWindow window = new PopupWindow(ll);
                window.showAtLocation(this, Gravity.CENTER, (int)event.getX(), (int)event.getY());
                //window.showAtLocation(this, Gravity.CENTER, 150, 150);
                window.showAsDropDown(this);
                window.update((int)event.getX(), (int)event.getY(), 0, 0);

                PopupMenu popup = new PopupMenu(this.getContext(), window.getContentView());
                popup.getMenuInflater().inflate(R.menu.graph_station_selected, popup.getMenu());
                popup.setOnMenuItemClickListener(this);
                popup.show();

            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                break;
            default:
                return false;
        }

        return true;
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

        if (firstTime) {
            centreViewOnActiveStation();
            firstTime = false;
        }

        if (getDisplayPreference(GraphActivity.DisplayPreference.SHOW_GRID)) {
            drawGrid(canvas);
        }

        drawSurvey(canvas, projection);

        drawSketch(canvas, sketch);

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

    private void drawSurvey(Canvas canvas, Space<Coord2D> space) {

        drawLegs(canvas, space);

        drawStations(canvas, space);

        // highlight active station after everything else to ensure it's on top
        highlightActiveStation(canvas);
    }


    private void drawLegs(Canvas canvas, Space<Coord2D> space) {

        Map<Leg, Line<Coord2D>> legMap = space.getLegMap();

        for (Leg leg : legMap.keySet()) {

            if (!getDisplayPreference(GraphActivity.DisplayPreference.SHOW_SPLAYS) &&
                    !leg.hasDestination()) {
                continue;
            }
            Line<Coord2D> line = legMap.get(leg);

            Coord2D translatedStart = surveyCoordsToViewCoords(line.getStart());
            Coord2D translatedEnd = surveyCoordsToViewCoords(line.getEnd());
            canvas.drawLine(
                    (int)(translatedStart.getX()), (int)(translatedStart.getY()),
                    (int)(translatedEnd.getX()), (int)(translatedEnd.getY()),
                    legPaint);

        }

    }


    private void drawStations(Canvas canvas, Space<Coord2D> space) {
        for (Map.Entry<Station, Coord2D> entry : space.getStationMap().entrySet()) {


            Station station = entry.getKey();
            Coord2D translatedStation = surveyCoordsToViewCoords(entry.getValue());
            int x = (int)(translatedStation.getX());
            int y = (int)(translatedStation.getY());

            if (station == survey.getActiveStation()) {
                canvas.drawCircle(x, y,
                        STATION_DIAMETER, highlightPaint);
            } else {
                canvas.drawCircle(x, y,
                        STATION_DIAMETER, stationPaint);
            }

            if (getDisplayPreference(GraphActivity.DisplayPreference.SHOW_STATION_LABELS)) {
                canvas.drawText(station.getName(),
                        x + STATION_LABEL_OFFSET,
                        y + STATION_LABEL_OFFSET,
                        stationPaint);
            }

        }
    }


    private void highlightActiveStation(Canvas canvas) {
        Coord2D activeStationCoord = projection.getStationMap().get(survey.getActiveStation());
        Coord2D activeStationViewCoord = surveyCoordsToViewCoords(activeStationCoord);
        canvas.drawCircle((int)activeStationViewCoord.getX(), (int)activeStationViewCoord.getY(),
                HIGHLIGHT_DIAMETER, highlightPaint);
        canvas.drawCircle((int)activeStationViewCoord.getX(), (int)activeStationViewCoord.getY(),
                STATION_DIAMETER, stationPaint);
    }



    private boolean getDisplayPreference(GraphActivity.DisplayPreference preference) {
        SharedPreferences preferences =
            getContext().getSharedPreferences("display", Context.MODE_PRIVATE);
        boolean isSelected =
            preferences.getBoolean(preference.toString(), preference.getDefault());
        return isSelected;
    }


    private void drawSketch(Canvas canvas, Sketch sketch) {

        Matrix matrix = new Matrix();
        matrix.setTranslate(-(float) viewpointOffset.getX(), -(float) viewpointOffset.getY());
        matrix.postScale((float) (surveyToViewScale), (float) (surveyToViewScale));


        for (PathDetail pathDetail : sketch.getPathDetails()) {
            Path translatedPath = new Path(pathDetail.getAndroidPath());
            translatedPath.transform(matrix);
            drawPaint.setColor(pathDetail.getColour());
            canvas.drawPath(translatedPath, drawPaint);
        }

        for (TextDetail textDetail : sketch.getTextDetails()) {
            Coord2D location = surveyCoordsToViewCoords(textDetail.getLocation());
            String text = textDetail.getText();
            labelPaint.setColor(textDetail.getColour());
            canvas.drawText(text, (float)location.getX(), (float)location.getY(), labelPaint);
        }

    }



    public void centreViewOnActiveStation() {
        Coord2D activeStationCoord = projection.getStationMap().get(survey.getActiveStation());
        centreViewOnSurveyPoint(activeStationCoord);
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


}

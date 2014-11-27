package org.hwyl.sexytopo.control;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import org.hwyl.sexytopo.model.Station;
import org.hwyl.sexytopo.model.Survey;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.Sketch;

import java.util.Map;

/**
 * Created by rls on 27/07/14.
 */
public class GraphView extends View {

    // FIXME hack!
private boolean firstTime = true;

    // The offset of the viewing window (what can be seen on the screen) from the whole survey
    private Coord2D viewpointOffset = Coord2D.ORIGIN;

    // These variables are used in handling the dragging of the viewing window
    private Coord2D actionDownPointOnView = Coord2D.ORIGIN;
    private Coord2D actionDownViewpointOffset = Coord2D.ORIGIN;

    // ratio of metres on the survey to pixels on the view
    // zoom in increases this, zooming out decreases it
    private double surveyToViewScale = 10.0; // 10 pixels is one metre

    private Survey survey;
    private Space<Coord2D> projection;
    private Sketch sketch;

    public static final int STATION_COLOUR = Color.RED;
    public static final int LEG_COLOUR = Color.RED;
    public static final int HIGHLIGHT_COLOUR = Color.YELLOW;
    public static final int DEFAULT_SKETCH_COLOUR = Color.BLACK;

    public static final int STATION_DIAMETER = 10;
    public static final int HIGHLIGHT_DIAMETER = 14;

    public enum SketchTool {
        MOVE, DRAW, ERASE
    }
    public SketchTool currentSketchTool = SketchTool.MOVE;





    private Paint stationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint legPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint drawPaint = new Paint(Paint.ANTI_ALIAS_FLAG);


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

        legPaint.setARGB(127, 255, 0, 0);
        legPaint.setStrokeWidth(5);


        drawPaint.setColor(DEFAULT_SKETCH_COLOUR);
        drawPaint.setStrokeWidth(3);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
    }


    public void setSurvey(Survey survey) {
        this.survey = survey; // FIXME might be able to get away with not passing in the full survey
    }


    public void setProjection(Space<Coord2D> projection) {
        this.projection = projection;
    }


    public void setSketch(Sketch sketch) {
        this.sketch = sketch;
    }


    public void centreViewOnActiveStation() {
        Coord2D activeStationCoord = projection.getStationMap().get(survey.getActiveStation());
        double viewWidthInMetres = getWidth() * (1 / surveyToViewScale);
        double viewHeightInMetres = getHeight() * (1 / surveyToViewScale);
        Coord2D delta = new Coord2D(viewWidthInMetres / 2, viewHeightInMetres / 2);
        viewpointOffset = activeStationCoord.minus(delta);
    }



    final GestureDetector gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
        public void onLongPress(MotionEvent e) {
            Log.e("", "Longpress detected");
        }
    });

    /*
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    };*/




    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (currentSketchTool) {
            case MOVE:
                return handleMove(event);
            case DRAW:
                return handleDraw(event);
            case ERASE:
                return handleErase(event);
        }
        return false;
    }




    private Coord2D viewCoordsToSurveyCoords(Coord2D coords) {
        return coords.scale(1 / surveyToViewScale).plus(viewpointOffset);
    }

    private Coord2D surveyCoordsToViewCoords(Coord2D coords) {
        return coords.flipVertically().minus(viewpointOffset).scale(surveyToViewScale);
    }



    private boolean handleDraw(MotionEvent event) {

        Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        Coord2D surveyCoords = viewCoordsToSurveyCoords(touchPointOnView);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                actionDownPointOnView = touchPointOnView;
                Path newPath = sketch.startNewPath();
                newPath.moveTo((float)surveyCoords.getX(), (float)surveyCoords.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                Path activePath = sketch.getActivePath();
                activePath.lineTo((float)surveyCoords.getX(), (float)surveyCoords.getY());
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (touchPointOnView.equals(actionDownPointOnView)) {
                    sketch.getActivePath().lineTo((float)surveyCoords.getX(), (float)surveyCoords.getY());
                } // FIXME keep this?
                sketch.finishPath();
                break;
            default:
                return false;
        }

         return true;
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
                break;
            case MotionEvent.ACTION_UP:
                break;
            default:
                return false;
        }

        return true;
    }


    private boolean handleErase(MotionEvent event) {

        Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                break;
            default:
                return false;
        }

        return true;
    }



    /*
    private static void getElementsToDraw(Space<Coord2D> space, int x, int y, int height, int width) {

        Set<Coord2D> visibleStations = new HashSet<>();

        for (Coord2D coord2D : space.getStationMap()) {
            if (isWithin(coord2D, x, x + width, y, y + height)) {
                visibleStations.add(coord2D);
            }
        }

        Set<Line<Coord2D>> visibleLegs = new HashSet<>();

        for (Line<Coord2D> line : space.getLegMap()) {
            if (isWithin(line.getStart(), x, x + width, y, y + height) ||
                isWithin(line.getEnd(), x, x + width, y, y + height)) {
                visibleLegs.add(line);
            }
        }
    }

    private static boolean isWithin(Coord2D coord2D, int minX, int maxX, int minY, int maxY) {
        return minX <= coord2D.getX() && coord2D.getX() <= maxX &&
               minY <= coord2D.getY() && coord2D.getY() <= maxX;
    }*/


    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        if (firstTime) {
            centreViewOnActiveStation();
            firstTime = false;
        }

        drawGrid(canvas);

        drawSurvey(canvas, projection);


        Matrix matrix = new Matrix();
        matrix.setTranslate(-(float) viewpointOffset.getX(), -(float) viewpointOffset.getY());
        matrix.postScale((float) (surveyToViewScale), (float) (surveyToViewScale));


        for (Sketch.PathDetail pathDetail : sketch.getPathDetails()) {
            Path translatedPath = new Path(pathDetail.getPath());
            translatedPath.transform(matrix);
            drawPaint.setColor(pathDetail.getColour());
            canvas.drawPath(translatedPath, drawPaint);
        }


    }


    private void drawGrid(Canvas canvas) {


        Paint axisPaint = new Paint();
        axisPaint.setColor(Color.LTGRAY);

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
            axisPaint.setStrokeWidth(n % 10 == 0? 3 : 1);
            canvas.drawLine(xView, 0, xView, getHeight(), axisPaint);
            if (xView >= getWidth()) {
                break;
            }
        }

        int numberTicksJustBeforeViewpointOffsetY = (int)(viewpointOffset.getY() / tickSizeInMetres);

        for (int n = numberTicksJustBeforeViewpointOffsetY; true; n++) {
            double ySurvey = n * tickSizeInMetres;
            int yView = (int)((ySurvey - viewpointOffset.getY()) * surveyToViewScale);
            axisPaint.setStrokeWidth(n % 10 == 0? 3 : 1);
            canvas.drawLine(0, yView, getWidth(), yView, axisPaint);
            if (yView >= getHeight()) {
                break;
            }
        }


    }

    private static double roundDown(double value, double factor)
    {
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

        for (Line<Coord2D> leg : space.getLegMap().values()) {
            Coord2D translatedStart = surveyCoordsToViewCoords(leg.getStart());
            Coord2D translatedEnd = surveyCoordsToViewCoords(leg.getEnd());
            canvas.drawLine(
                    (int)(translatedStart.getX()), (int)(translatedStart.getY()),
                    (int)(translatedEnd.getX()), (int)(translatedEnd.getY()),
                    legPaint);

        }


        for (Map.Entry<Station, Coord2D> entry : space.getStationMap().entrySet()) {
            Coord2D translatedStation = surveyCoordsToViewCoords(entry.getValue());

            if (entry.getKey() == survey.getActiveStation()) {
                stationPaint.setColor(HIGHLIGHT_COLOUR);
                canvas.drawCircle((int) (translatedStation.getX()), (int) (translatedStation.getY()),
                        STATION_DIAMETER + HIGHLIGHT_DIAMETER, stationPaint);
            }
                stationPaint.setColor(STATION_COLOUR);
            //}
            canvas.drawCircle((int)(translatedStation.getX()), (int)(translatedStation.getY()),
                    STATION_DIAMETER, stationPaint);


        }
    }


/*
    private BoundingBox getBoundingBox(Space<Coord2D> space) {
        Set<Coord2D> points = space.getAllCoords();

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (Coord2D point : points) {
            minX = Math.min((int)point.getX(), minX);
            maxX = Math.max((int)point.getX(), maxX);
            minY = Math.min((int)point.getY(), minY);
            maxY = Math.max((int)point.getY(), maxY);
        }

        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }*/


    public void zoom(double amount) {

        if ((surveyToViewScale + amount) <= 0) {
            return; // no point allowing zooming out so that the survey is a point
        }

        surveyToViewScale += amount;

        //viewpointOffset = viewpointOffset.scale(surveyToViewScale);
        //currentX *= surveyToViewScale;
        //currentY *= surveyToViewScale;
    }

    public void undo() {
        sketch.undo();
        invalidate();
    }

    public void redo() {
        sketch.redo();
        invalidate();
    }


    private class BoundingBox {
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        private BoundingBox(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;

        }
    }





}

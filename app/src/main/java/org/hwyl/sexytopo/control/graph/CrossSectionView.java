package org.hwyl.sexytopo.control.graph;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import org.hwyl.sexytopo.control.activity.CrossSectionActivity;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.SketchPreferences;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Line;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.BrushColour;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.SketchTool;
import org.hwyl.sexytopo.model.survey.Leg;

import java.util.List;
import java.util.Map;

public class CrossSectionView extends View {

    private final ScaleGestureDetector scaleGestureDetector;
    private final GestureDetector gestureDetector;

    private Coord2D viewpointOffset = Coord2D.ORIGIN;
    private Coord2D actionDownPointOnView = Coord2D.ORIGIN;
    private Coord2D actionDownViewpointOffset = Coord2D.ORIGIN;

    private float surveyToViewScale = 100.0f;
    private static final float MIN_ZOOM = 10.0f;
    private static final float MAX_ZOOM = 1000.0f;

    private CrossSection crossSection;
    private CrossSectionActivity activity;
    
    private SketchTool currentSketchTool = SketchTool.DRAW;
    private BrushColour currentBrushColour = BrushColour.BLACK;

    private final Paint splayPaint = new Paint();
    private final Paint stationPaint = new Paint();
    private final Paint gridPaint = new Paint();
    private final Paint drawPaint = new Paint();
    
    private static final int STATION_DIAMETER = 12;
    private static final int SPLAY_COLOUR = Colour.RED.intValue;
    private static final int STATION_COLOUR = Colour.DARK_RED.intValue;

    public CrossSectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
        initialisePaint();
    }

    private void initialisePaint() {
        boolean antiAlias = GeneralPreferences.isAntialiasingModeOn();
        
        splayPaint.setColor(SPLAY_COLOUR);
        splayPaint.setStrokeWidth(GeneralPreferences.getSplayStrokeWidth());
        splayPaint.setStyle(Paint.Style.STROKE);
        splayPaint.setAntiAlias(antiAlias);

        stationPaint.setColor(STATION_COLOUR);
        stationPaint.setStrokeWidth(5);
        stationPaint.setStyle(Paint.Style.STROKE);
        stationPaint.setAntiAlias(antiAlias);
        
        gridPaint.setColor(0x40808080);
        gridPaint.setStrokeWidth(1);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setAntiAlias(antiAlias);
        
        drawPaint.setStrokeWidth(3);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setAntiAlias(antiAlias);
    }

    public void setActivity(CrossSectionActivity activity) {
        this.activity = activity;
    }

    public void setCrossSection(CrossSection crossSection) {
        this.crossSection = crossSection;
        centreView();
    }
    
    public void setSketchTool(SketchTool tool) {
        this.currentSketchTool = tool;
    }
    
    public SketchTool getSketchTool() {
        return currentSketchTool;
    }
    
    public void setBrushColour(BrushColour colour) {
        this.currentBrushColour = colour;
    }

    private void centreView() {
        viewpointOffset = Coord2D.ORIGIN;
        float centreX = getWidth() / 2f / surveyToViewScale;
        float centreY = getHeight() / 2f / surveyToViewScale;
        viewpointOffset = new Coord2D(-centreX, -centreY);
        invalidate();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        if (scaleGestureDetector.isInProgress()) {
            return true;
        }

        switch (currentSketchTool) {
            case MOVE:
                return handleMove(event);
            case DRAW:
                return handleDraw(event);
            case ERASE:
                return handleErase(event);
            default:
                return handleMove(event);
        }
    }

    private boolean handleMove(MotionEvent event) {
        Coord2D touchPoint = new Coord2D(event.getX(), event.getY());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                actionDownPointOnView = touchPoint;
                actionDownViewpointOffset = viewpointOffset;
                return true;

            case MotionEvent.ACTION_MOVE:
                float deltaX = (touchPoint.x - actionDownPointOnView.x) / surveyToViewScale;
                float deltaY = (touchPoint.y - actionDownPointOnView.y) / surveyToViewScale;
                viewpointOffset = new Coord2D(
                    actionDownViewpointOffset.x - deltaX,
                    actionDownViewpointOffset.y - deltaY);
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                return true;
        }
        return false;
    }
    
    private boolean handleDraw(MotionEvent event) {
        if (crossSection == null) return false;
        
        Sketch sketch = crossSection.getSketch();
        Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        Coord2D surveyCoords = viewCoordsToSurveyCoords(touchPointOnView);
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                sketch.setActiveColour(currentBrushColour.getColour());
                sketch.startNewPath(surveyCoords);
                return true;
                
            case MotionEvent.ACTION_MOVE:
                PathDetail activePath = sketch.getActivePath();
                if (activePath != null) {
                    activePath.lineTo(surveyCoords);
                    invalidate();
                }
                return true;
                
            case MotionEvent.ACTION_UP:
                if (sketch.getActivePath() != null) {
                    sketch.finishPath();
                    invalidate();
                }
                return true;
        }
        return false;
    }
    
    private boolean handleErase(MotionEvent event) {
        if (crossSection == null) return false;
        
        Sketch sketch = crossSection.getSketch();
        Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        Coord2D surveyCoords = viewCoordsToSurveyCoords(touchPointOnView);
        
        float deleteDistance = GraphView.DELETE_PATHS_WITHIN_N_PIXELS / surveyToViewScale;
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                PathDetail toDelete = findNearestPath(sketch, surveyCoords, deleteDistance);
                if (toDelete != null) {
                    sketch.deleteDetail(toDelete);
                    invalidate();
                }
                return true;
        }
        return false;
    }
    
    private PathDetail findNearestPath(Sketch sketch, Coord2D point, float maxDistance) {
        PathDetail closest = null;
        float minDistance = Float.MAX_VALUE;
        
        for (PathDetail path : sketch.getPathDetails()) {
            float distance = path.getDistanceFrom(point);
            if (distance < maxDistance && distance < minDistance) {
                closest = path;
                minDistance = distance;
            }
        }
        return closest;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (crossSection == null) {
            return;
        }

        drawGrid(canvas);
        drawSplays(canvas);
        drawSketch(canvas);
        drawStation(canvas);
    }

    private void drawGrid(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        float gridSpacing = surveyToViewScale; // 1 metre grid
        
        float startX = (-viewpointOffset.x * surveyToViewScale) % gridSpacing;
        for (float x = startX; x < width; x += gridSpacing) {
            canvas.drawLine(x, 0, x, height, gridPaint);
        }
        
        float startY = (-viewpointOffset.y * surveyToViewScale) % gridSpacing;
        for (float y = startY; y < height; y += gridSpacing) {
            canvas.drawLine(0, y, width, y, gridPaint);
        }
    }

    private void drawSplays(Canvas canvas) {
        Space<Coord2D> projection = crossSection.getProjection();
        Map<Leg, Line<Coord2D>> legMap = projection.getLegMap();

        for (Line<Coord2D> line : legMap.values()) {
            Coord2D start = surveyCoordsToViewCoords(line.getStart());
            Coord2D end = surveyCoordsToViewCoords(line.getEnd());
            canvas.drawLine(start.x, start.y, end.x, end.y, splayPaint);
        }
    }
    
    private void drawSketch(Canvas canvas) {
        Sketch sketch = crossSection.getSketch();
        
        for (PathDetail pathDetail : sketch.getPathDetails()) {
            drawPath(canvas, pathDetail);
        }
    }
    
    private void drawPath(Canvas canvas, PathDetail pathDetail) {
        List<Coord2D> points = pathDetail.getPath();
        if (points.size() < 2) return;
        
        drawPaint.setColor(pathDetail.getColour().intValue);
        
        Path path = new Path();
        Coord2D first = surveyCoordsToViewCoords(points.get(0));
        path.moveTo(first.x, first.y);
        
        for (int i = 1; i < points.size(); i++) {
            Coord2D point = surveyCoordsToViewCoords(points.get(i));
            path.lineTo(point.x, point.y);
        }
        
        canvas.drawPath(path, drawPaint);
    }

    private void drawStation(Canvas canvas) {
        Coord2D stationOnView = surveyCoordsToViewCoords(Coord2D.ORIGIN);
        float halfCross = STATION_DIAMETER / 2f;
        canvas.drawLine(stationOnView.x, stationOnView.y - halfCross, 
                       stationOnView.x, stationOnView.y + halfCross, stationPaint);
        canvas.drawLine(stationOnView.x - halfCross, stationOnView.y, 
                       stationOnView.x + halfCross, stationOnView.y, stationPaint);
    }

    private Coord2D surveyCoordsToViewCoords(Coord2D coords) {
        return new Coord2D(
            (coords.x - viewpointOffset.x) * surveyToViewScale,
            (coords.y - viewpointOffset.y) * surveyToViewScale);
    }
    
    private Coord2D viewCoordsToSurveyCoords(Coord2D coords) {
        return new Coord2D(
            (coords.x / surveyToViewScale) + viewpointOffset.x,
            (coords.y / surveyToViewScale) + viewpointOffset.y);
    }

    public void adjustZoomBy(float factor) {
        float newScale = surveyToViewScale * factor;
        if (newScale >= MIN_ZOOM && newScale <= MAX_ZOOM) {
            surveyToViewScale = newScale;
            invalidate();
        }
    }
    
    public void undo() {
        if (crossSection != null) {
            crossSection.getSketch().undo();
            invalidate();
        }
    }
    
    public void redo() {
        if (crossSection != null) {
            crossSection.getSketch().redo();
            invalidate();
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            adjustZoomBy(detector.getScaleFactor());
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            centreView();
            return true;
        }
    }
}

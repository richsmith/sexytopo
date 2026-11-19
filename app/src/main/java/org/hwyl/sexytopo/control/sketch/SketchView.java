package org.hwyl.sexytopo.control.sketch;

import static org.hwyl.sexytopo.control.sketch.SketchPaints.FADED_ALPHA;
import static org.hwyl.sexytopo.control.sketch.SketchPaints.SOLID_ALPHA;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.SurveyManager;
import org.hwyl.sexytopo.control.activity.SketchActivity;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.control.util.SketchPreferences;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.geometry.Projection2D;
import org.hwyl.sexytopo.model.geometry.Space;
import org.hwyl.sexytopo.model.sketch.AreaType;
import org.hwyl.sexytopo.model.sketch.BrushColour;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.sketch.CrossSectionDetail;
import org.hwyl.sexytopo.model.sketch.LineType;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.SketchTool;
import org.hwyl.sexytopo.model.sketch.Symbol;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;
import org.hwyl.sexytopo.model.survey.SurveyConnection;

public class SketchView extends View {

    private final ScaleGestureDetector scaleGestureDetector;
    private final GestureDetector longPressDetector;

    // Which part of the survey is on screen, and at what zoom
    protected final Viewport viewport = new Viewport();

    // What's being shown, and which tool is in use
    protected final SketchState state = new SketchState();

    private SketchActivity activity;

    private Map<Survey, Space<Coord2D>> translatedConnectedSurveys = new HashMap<>();

    private boolean surveyChanged;

    // cached preference for performance
    private boolean isTwoFingerModeActive = true;

    // Flag to prevent double menu opening during this touch sequence
    private boolean menuShownInThisTouch = false;

    // ********** Renderers **********

    private final SketchRenderer sketchRenderer =
            new SketchRenderer(viewport, () -> activity.isDebugMode());

    private final SurveyDataRenderer surveyDataRenderer = new SurveyDataRenderer(viewport, state);

    private final OverlayRenderer overlays = new OverlayRenderer(viewport, state);

    private final HotCorners hotCorners = new HotCorners();

    // ********** Cross-section layer and tool handlers **********

    private final ViewHost host = new ViewHost();

    private final CrossSectionLayer crossSections =
            new CrossSectionLayer(viewport, state, surveyDataRenderer, sketchRenderer, host);

    private final SketchSelector selector = new SketchSelector(viewport, state);

    private final MoveTool moveTool = new MoveTool(viewport, host);
    private final LineTool lineTool = new LineTool(viewport, state, host);
    private final AreaTool areaTool = new AreaTool(viewport, state, host);
    private final EraseTool eraseTool = new EraseTool(viewport, state, crossSections, host);
    private final TextTool textTool = new TextTool(viewport, state, host);
    private final SymbolTool symbolTool = new SymbolTool(viewport, state, host, textTool);
    private final SelectTool selectTool = new SelectTool(state, selector, host);

    // what each tool does with a touch; PINCH_TO_ZOOM has no entry, as the scale detector drives it
    private final Map<SketchTool, ToolHandler> sketchToolsToHandlers = createSketchToolMap();

    private Map<SketchTool, ToolHandler> createSketchToolMap() {
        Map<SketchTool, ToolHandler> handlers = new EnumMap<>(SketchTool.class);
        handlers.put(SketchTool.MOVE, moveTool);
        handlers.put(SketchTool.MODAL_MOVE, moveTool);
        handlers.put(SketchTool.LINE, lineTool);
        handlers.put(SketchTool.AREA, areaTool);
        handlers.put(SketchTool.ERASE, eraseTool);
        handlers.put(SketchTool.SYMBOL, symbolTool);
        handlers.put(SketchTool.TEXT, textTool);
        handlers.put(SketchTool.SELECT, selectTool);
        handlers.put(SketchTool.POSITION_CROSS_SECTION, crossSections.positionTool);
        handlers.put(SketchTool.ROTATE_CROSS_SECTION, crossSections.rotateTool);
        handlers.put(SketchTool.MOVE_CROSS_SECTION, crossSections.moveTool);
        return handlers;
    }

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

    // ********** Set-up **********

    public void initialisePaint() {
        sketchRenderer.initialise(getContext());
        surveyDataRenderer.initialise(getContext());
        overlays.initialise(getContext());
        hotCorners.initialise(getContext());
        crossSections.initialisePaints(getContext());

        isTwoFingerModeActive = GeneralPreferences.isTwoFingerModeActive();
    }

    public void setActivity(SketchActivity sketchActivity) {
        this.activity = sketchActivity;
    }

    // ********** Survey and display **********

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
        sketchRenderer.setDarkModeActive(isDarkModeActive);
    }

    public void setCachedStats(float surveyLength, float surveyHeight) {
        overlays.setStats(surveyLength, surveyHeight);
    }

    public void setCompassAzimuth(float degrees) {
        overlays.setCompassAzimuth(degrees);
        if (SketchPreferences.Toggle.SHOW_COMPASS.isOn()
                && state.getProjectionType() == Projection2D.PLAN) {
            invalidate();
        }
    }

    // ********** Tools **********

    public void setSketchTool(SketchTool sketchTool) {
        state.setSketchTool(sketchTool);
    }

    public SketchTool getSketchTool() {
        return state.getSketchTool();
    }

    public void setBrushColour(BrushColour brushColour) {
        state.setColour(brushColour.getColour());
    }

    public void setLineType(LineType lineType) {
        lineTool.setLineType(lineType);
    }

    public void setAreaType(AreaType areaType) {
        areaTool.setAreaType(areaType);
    }

    public void setCurrentSymbol(Symbol symbol) {
        symbolTool.setSymbol(symbol);
    }

    // ********** Cross-sections **********

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

    // ********** Panning and zooming **********

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

    // ********** Undo and redo **********

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

    // ********** Touch **********

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
            eraseTool.cancel();
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

        ToolHandler handler = sketchToolsToHandlers.get(state.getSketchTool());
        return handler != null && handler.onTouch(event);
    }

    private boolean isModalMoveSelection(MotionEvent event) {

        if (state.getSketchTool() == SketchTool.MODAL_MOVE) {
            return false;
        }

        if (isTwoFingerModeActive && event.getPointerCount() >= 2) {
            return true;
        }

        if (hotCorners.isHit(event, getWidth(), getHeight())) {
            return true;
        }

        return false;
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

    private void broadcastSurveyUpdated() {
        SurveyManager.getInstance(getContext().getApplicationContext()).broadcastSurveyUpdated();
    }

    // ********** Drawing **********

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
        hotCorners.draw(
                canvas, getWidth(), getHeight(), state.getSketchTool() == SketchTool.MODAL_MOVE);
        if (activity.isDebugMode()) {
            overlays.drawDebuggingInfo(canvas, getWidth());
        }
    }

    protected void drawSurvey(Canvas canvas, Survey survey, Space<Coord2D> projection, int alpha) {
        sketchRenderer.draw(canvas, activity.getSketch(survey), alpha);
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
        overlays.drawGrid(canvas, getWidth(), getHeight());
    }

    private void drawSurveyData(Survey survey, Canvas canvas, Space<Coord2D> space, int alpha) {
        surveyDataRenderer.drawLegs(canvas, space, alpha, this::isLegInPlane);
        surveyDataRenderer.drawStations(canvas, survey, space, alpha, crossSections::drawIndicator);
    }

    /**
     * Whether a leg lies in the plane being drawn. Legs that don't are drawn dashed, to show that
     * they point into or out of the page.
     */
    protected boolean isLegInPlane(Leg leg) {
        return state.getProjectionType().isLegInPlane(leg);
    }

    protected void drawLegend(Canvas canvas) {
        overlays.drawLegend(canvas, getHeight());
    }

    /**
     * Whether to draw the compass, which turns to show which way the device is facing. That only
     * means something in a view drawn with north at the top.
     */
    protected boolean isCompassShown() {
        return state.getProjectionType() == Projection2D.PLAN;
    }

    protected void drawCompass(Canvas canvas) {
        if (isCompassShown()) {
            overlays.drawCompass(canvas, getHeight());
        }
    }

    /**
     * Gives the tools and the cross-section layer what they need from this view, without making it
     * public.
     */
    private class ViewHost implements CrossSectionLayer.Host {
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
        public void broadcastSurveyUpdated() {
            SketchView.this.broadcastSurveyUpdated();
        }

        @Override
        public void openStationMenu(MotionEvent event, Station station) {
            if (!menuShownInThisTouch) {
                showContextMenu(event, station);
                menuShownInThisTouch = true;
            }
        }

        @Override
        public void invalidate() {
            SketchView.this.invalidate();
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
            Station matchedStation = selector.findStationAt(touchPointOnView);
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
            PathDetail matchedPath = selector.findPathAt(touchPointOnView);
            if (matchedPath != null && !menuShownInThisTouch) {
                state.getSketch().abandonActivePath();
                LineContextMenu.show(
                        SketchView.this,
                        (int) motionEvent.getX(),
                        (int) motionEvent.getY(),
                        state.getSketch(),
                        matchedPath,
                        SketchView.this::invalidate);
                menuShownInThisTouch = true;
            }
        }
    }
}

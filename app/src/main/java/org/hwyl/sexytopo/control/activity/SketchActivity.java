package org.hwyl.sexytopo.control.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import org.apache.commons.lang3.ArrayUtils;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.sketch.SketchView;
import org.hwyl.sexytopo.control.table.LegDialogs;
import org.hwyl.sexytopo.control.util.SketchPreferences;
import org.hwyl.sexytopo.control.util.SurveyStats;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.geometry.Projection2D;
import org.hwyl.sexytopo.model.geometry.Space;
import org.hwyl.sexytopo.model.sketch.AreaType;
import org.hwyl.sexytopo.model.sketch.BrushColour;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.sketch.CrossSectionDetail;
import org.hwyl.sexytopo.model.sketch.LineType;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.SketchTool;
import org.hwyl.sexytopo.model.sketch.Symbol;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

public abstract class SketchActivity extends SurveyEditorActivity
        implements View.OnClickListener, PopupMenu.OnMenuItemClickListener, SensorEventListener {

    private static final float ZOOM_INCREMENT = 1.1f;
    private static final float ZOOM_DECREMENT = 0.9f;

    private static final int[] SKETCH_BUTTON_IDS =
            new int[] {
                R.id.buttonDraw,
                R.id.buttonArea,
                R.id.buttonErase,
                R.id.buttonSymbol,
                R.id.buttonUndo,
                R.id.buttonBlack,
                R.id.buttonBrown,
                R.id.buttonGrey,
                R.id.buttonRed,
                R.id.buttonOrange,
                R.id.buttonBlue,
                R.id.buttonGreen,
                R.id.buttonPurple
            };

    private static final int[] CONTROL_BUTTON_IDS =
            new int[] {
                R.id.buttonMove,
                R.id.buttonSelect,
                R.id.buttonZoomIn,
                R.id.buttonZoomOut,
                R.id.buttonMenu
            };

    private static final int[] BUTTON_IDS =
            ArrayUtils.addAll(SKETCH_BUTTON_IDS, CONTROL_BUTTON_IDS);

    private SketchView sketchView;

    private SensorManager sensorManager;
    private Sensor rotationSensor;

    private BroadcastReceiver updatedReceiver;
    private BroadcastReceiver createdReceiver;

    private int buttonHighlightColour = Colour.RED.intValue;

    private boolean symbolToolbarOpenedOnce = false;
    private boolean lineTypeToolbarOpenedOnce = false;
    private boolean areaTypeToolbarOpenedOnce = false;

    // the pop-up toolbars above the sketch toolbar; at most one is open at a time
    private static final int[] POP_UP_TOOLBAR_IDS =
            new int[] {R.id.symbolToolbar, R.id.lineTypeToolbar, R.id.areaTypeToolbar};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updatedReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        syncWithSurvey();
                    }
                };

        createdReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        handleAutoRecentre();
                    }
                };

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }

        setContentView(R.layout.activity_sketch);
        setupMaterialToolbar();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        applyEdgeToEdgeInsets(R.id.rootLayout, true, true);

        for (int id : BUTTON_IDS) {
            View button = findViewById(id);
            button.setOnClickListener(this);
        }

        sketchView = findViewById(R.id.sketchView);
        sketchView.setActivity(this);

        // Needs to be threaded so it is only run once we know height and width
        sketchView.post(this::setViewLocation);
    }

    private void handleAutoRecentre() {
        if (SketchPreferences.Toggle.AUTO_RECENTRE.isOn()) {
            sketchView.centreViewOnActiveStation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (SketchPreferences.Toggle.SHOW_COMPASS.isOn()) {
            registerCompassSensor();
        }

        registerReceivers();
        syncWithSurvey();

        intialiseActivity();
        initialiseSketchView();
        initialiseSymbolToolbar();
        initialiseLineTypeToolbar();
        initialiseAreaTypeToolbar();
        initialiseTools();

        setSketchButtonsStatus();
    }

    public void onPause() {
        super.onPause();
        unregisterCompassSensor();
        unregisterReceivers();
    }

    private void registerCompassSensor() {
        if (sensorManager != null && rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    private void unregisterCompassSensor() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(rotMatrix, event.values);

            int axisX = SensorManager.AXIS_X;
            int axisY = SensorManager.AXIS_Y;
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            switch (rotation) {
                case Surface.ROTATION_90:
                    axisX = SensorManager.AXIS_Y;
                    axisY = SensorManager.AXIS_MINUS_X;
                    break;
                case Surface.ROTATION_180:
                    axisX = SensorManager.AXIS_MINUS_X;
                    axisY = SensorManager.AXIS_MINUS_Y;
                    break;
                case Surface.ROTATION_270:
                    axisX = SensorManager.AXIS_MINUS_Y;
                    axisY = SensorManager.AXIS_X;
                    break;
                default:
                    break;
            }

            float[] remapped = new float[9];
            SensorManager.remapCoordinateSystem(rotMatrix, axisX, axisY, remapped);
            float[] orientation = new float[3];
            SensorManager.getOrientation(remapped, orientation);
            float azimuthDeg = (float) Math.toDegrees(orientation[0]);
            sketchView.setCompassAzimuth(azimuthDeg);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void registerReceivers() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(
                updatedReceiver, new IntentFilter(SexyTopoConstants.SURVEY_UPDATED_EVENT));
        broadcastManager.registerReceiver(
                createdReceiver, new IntentFilter(SexyTopoConstants.NEW_STATION_CREATED_EVENT));
    }

    private void unregisterReceivers() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.unregisterReceiver(updatedReceiver);
        broadcastManager.unregisterReceiver(createdReceiver);
    }

    private void setViewLocation() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.getString(SexyTopoConstants.JUMP_TO_STATION) != null) {
            String requestedStationName = bundle.getString(SexyTopoConstants.JUMP_TO_STATION);
            Station requestedStation = getSurvey().getStationByName(requestedStationName);
            sketchView.centreViewOnStation(requestedStation);
        } else {
            sketchView.centreViewOnActiveStation();
        }
    }

    private void intialiseActivity() {
        buttonHighlightColour = ContextCompat.getColor(this, R.color.buttonHighlight);
    }

    private void initialiseSketchView() {
        sketchView.initialisePaint();
        sketchView.setProjectionType(getProjectionType());

        boolean isDarkModeActive = isDarkModeActive();
        sketchView.setIsDarkModeActive(isDarkModeActive);
    }

    @Override
    public void syncWithSurvey() {
        Survey survey = getSurvey();
        sketchView.setSurvey(survey);
        sketchView.setSketch(getSketch(survey));
        sketchView.setProjection(getProjection(survey));
        sketchView.checkForChangedSurvey();
        float surveyLength = SurveyStats.calcTotalLength(survey);
        float surveyHeight = SurveyStats.calcHeightRange(survey);
        sketchView.setCachedStats(surveyLength, surveyHeight);
        sketchView.invalidate();
    }

    /** The sketch this activity shows, from the survey being worked on. */
    public Sketch getSketch() {
        return getSketch(getSurvey());
    }

    /**
     * The sketch this activity's view shows for the given survey: its plan sketch in the plan, and
     * so on. This takes a survey because linked surveys are drawn with their own sketches.
     */
    public Sketch getSketch(Survey survey) {
        return survey.getSketch(getProjectionType());
    }

    public Space<Coord2D> getProjection(Survey survey) {
        return getProjectionType().project(survey);
    }

    public abstract Projection2D getProjectionType();

    @Override
    public void onClick(View view) {
        int id = view.getId();
        handleAction(id);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem flipLastLineItem = menu.findItem(R.id.action_flip_last_line);
        if (flipLastLineItem != null) {
            flipLastLineItem.setEnabled(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_flip_last_line) {
            flipLastLine();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Reverses the most recently drawn typed line, flipping which side its ticks fall on. Meant for
     * the case where a pit or chimney came out ticked on the wrong side: one menu tap fixes it
     * instead of undoing and redrawing the other way.
     */
    private void flipLastLine() {
        Sketch sketch = getSketch(getSurvey());
        PathDetail toFlip = sketch.getMostRecentSemanticPath();
        if (toFlip == null) {
            showSimpleToast(R.string.tools_flip_last_line_none);
            return;
        }
        sketch.flipPathDetail(toFlip);
        sketchView.invalidate();
    }

    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();

        // this has to be a big hairy if-else chain instead of a switch statement
        // (itemId is no longer a constant in later Android versions)
        if (itemId == R.id.buttonSnapToLines) {
            SketchPreferences.Toggle.SNAP_TO_LINES.set(!item.isChecked());
            return true;
        } else if (itemId == R.id.buttonShowGrid) {
            SketchPreferences.Toggle.SHOW_GRID.set(!item.isChecked());
            sketchView.invalidate();
            return true;
        } else if (itemId == R.id.buttonShowCompass) {
            boolean turningOn = !item.isChecked();
            SketchPreferences.Toggle.SHOW_COMPASS.set(turningOn);
            if (turningOn) {
                registerCompassSensor();
            } else {
                unregisterCompassSensor();
            }
            sketchView.invalidate();
            return true;
        } else if (itemId == R.id.buttonFadeNonActive) {
            SketchPreferences.Toggle.FADE_NON_ACTIVE.set(!item.isChecked());
            sketchView.invalidate();
            return true;
        } else if (itemId == R.id.buttonShowSplays) {
            SketchPreferences.Toggle.SHOW_SPLAYS.set(!item.isChecked());
            sketchView.invalidate();
            return true;
        } else if (itemId == R.id.buttonShowXSections) {
            SketchPreferences.Toggle.SHOW_X_SECTIONS.set(!item.isChecked());
            sketchView.invalidate();
            return true;
        } else if (itemId == R.id.buttonShowSketch) {
            SketchPreferences.Toggle.SHOW_SKETCH.set(!item.isChecked());
            setSketchButtonsStatus();
            sketchView.invalidate();
            return true;
        } else if (itemId == R.id.buttonShowStationLabels) {
            SketchPreferences.Toggle.SHOW_STATION_LABELS.set(!item.isChecked());
            sketchView.invalidate();
            return true;
        } else if (itemId == R.id.buttonShowWallSides) {
            SketchPreferences.Toggle.SHOW_WALL_SIDES.set(!item.isChecked());
            sketchView.invalidate();
            return true;
        } else if (itemId == R.id.buttonShowConnections) {
            SketchPreferences.Toggle.SHOW_CONNECTIONS.set(!item.isChecked());
            sketchView.invalidate();
            return true;
        } else if (itemId == R.id.buttonAutoRecentre) {
            SketchPreferences.Toggle.AUTO_RECENTRE.set(!item.isChecked());
            sketchView.invalidate();
            return true;
        } else if (itemId == R.id.buttonBlueWater) {
            SketchPreferences.Toggle.BLUE_WATER.set(!item.isChecked());
            // the water area icons follow the setting
            initialiseAreaTypeToolbar();
            selectAreaType(SketchPreferences.getSelectedAreaType());
            return true;
        } else if (itemId == R.id.buttonPinchToZoom) {
            SketchPreferences.Toggle.PINCH_TO_ZOOM.set(!item.isChecked());
            return true;
        } else {
            return handleAction(itemId);
        }
    }

    private void setSketchButtonsStatus() {

        if (isDarkModeActive()) {
            ImageButton blackButton = findViewById(R.id.buttonBlack);
            blackButton.setImageResource(R.drawable.white);
        }

        boolean isEnabled = SketchPreferences.Toggle.SHOW_SKETCH.isOn();
        for (int id : SKETCH_BUTTON_IDS) {
            ImageButton button = findViewById(id);
            button.setEnabled(isEnabled);
        }

        if (!isEnabled) {
            sketchView.setSketchTool(SketchTool.MOVE);
        }
    }

    public boolean handleAction(int itemId) {

        SketchView sketchView = findViewById(R.id.sketchView);
        sketchView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        SketchTool alreadySelectedTool = sketchView.getSketchTool();

        // ********** Handle special commands **********

        if (itemId == R.id.buttonZoomIn) {
            sketchView.adjustZoomBy(ZOOM_INCREMENT);
            sketchView.invalidate();
            return true;
        } else if (itemId == R.id.buttonZoomOut) {
            sketchView.adjustZoomBy(ZOOM_DECREMENT);
            sketchView.invalidate();
            return true;
        } else if (itemId == R.id.buttonUndo) {
            sketchView.undo();
            return true;
        } else if (itemId == R.id.buttonRedo) {
            sketchView.redo();
            return true;
        } else if (itemId == R.id.buttonMenu) {
            openDisplayMenu();
            return true;
        } else if (itemId == R.id.buttonCentreView) {
            sketchView.centreViewOnActiveStation();
            sketchView.invalidate();
            return true;
        } else if (itemId == R.id.buttonDeleteLastLeg) {
            getSurvey().undoAddLeg();
            syncWithSurvey();
            return true;
        }

        // ********** General colour selection **********

        for (BrushColour brushColour : BrushColour.values()) {
            if (brushColour.getId() == itemId) {
                selectBrushColour(brushColour);
                if (!sketchView.getSketchTool().usesColour()) {
                    selectSketchTool(SketchTool.DRAW);
                }
                return true;
            }
        }

        // ********** Handle special symbol logic **********

        if (itemId == R.id.buttonSymbol) {
            boolean wasAlreadyInSymbolMode = alreadySelectedTool == SketchTool.SYMBOL;
            selectSketchTool(SketchTool.SYMBOL);
            // Open the symbol toolbar the first time ever (to teach the user it's there)
            // and toggle it whenever the symbol tool is tapped while already active.
            if (!symbolToolbarOpenedOnce || wasAlreadyInSymbolMode) {
                symbolToolbarOpenedOnce = true;
                togglePopUpToolbar(R.id.symbolToolbar);
            }
            return true;
        }

        for (Symbol symbol : Symbol.values()) {
            if (itemId == symbol.getButtonViewId()) {
                selectSketchTool(SketchTool.SYMBOL);
                selectSymbol(symbol);
                return true;
            }
        }

        // ********** Handle special line type logic **********

        if (itemId == R.id.buttonDraw) {
            boolean wasAlreadyInDrawMode = alreadySelectedTool == SketchTool.DRAW;
            selectSketchTool(SketchTool.DRAW);
            // Mirrors the symbol toolbar: open the first time ever, then toggle whenever the
            // draw tool is tapped while already active.
            if (!lineTypeToolbarOpenedOnce || wasAlreadyInDrawMode) {
                lineTypeToolbarOpenedOnce = true;
                togglePopUpToolbar(R.id.lineTypeToolbar);
            }
            return true;
        }

        for (LineType lineType : LineType.values()) {
            if (itemId == lineType.getButtonViewId()) {
                selectSketchTool(SketchTool.DRAW);
                selectLineType(lineType);
                return true;
            }
        }

        // ********** Handle special area type logic **********

        if (itemId == R.id.buttonArea) {
            boolean wasAlreadyInAreaMode = alreadySelectedTool == SketchTool.AREA;
            selectSketchTool(SketchTool.AREA);
            // Mirrors the symbol toolbar: open the first time ever, then toggle whenever the
            // area tool is tapped while already active.
            if (!areaTypeToolbarOpenedOnce || wasAlreadyInAreaMode) {
                areaTypeToolbarOpenedOnce = true;
                togglePopUpToolbar(R.id.areaTypeToolbar);
            }
            return true;
        }

        for (AreaType areaType : AreaType.values()) {
            if (itemId == areaType.getButtonViewId()) {
                selectSketchTool(SketchTool.AREA);
                selectAreaType(areaType);
                return true;
            }
        }

        // ********** Handle generic sketch tools **********

        for (SketchTool sketchTool : SketchTool.values()) {
            if (sketchTool.getId() == itemId) {
                selectSketchTool(sketchTool);
                return true;
            }
        }

        return false;
    }

    private void openDisplayMenu() {

        View view = findViewById(R.id.buttonMenu);
        PopupMenu popup = new PopupMenu(this, view);
        Menu menu = popup.getMenu();
        popup.getMenuInflater().inflate(R.menu.drawing, menu);
        popup.setOnMenuItemClickListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            menu.setGroupDividerEnabled(true);
        }

        for (SketchPreferences.Toggle toggle : SketchPreferences.Toggle.values()) {
            int controlId = toggle.getControlId();
            MenuItem menuItem = menu.findItem(controlId);
            menuItem.setChecked(toggle.isOn());
        }

        popup.show();
    }

    private void initialiseSymbolToolbar() {
        GridLayout buttonPanel =
                preparePickerPanel(R.id.symbolToolbarButtonPanel, Symbol.values().length);
        for (Symbol symbol : Symbol.values()) {
            buttonPanel.addView(
                    createPickerButton(
                            symbol.getButtonViewId(),
                            tintPickerIcon(symbol.createDrawable()),
                            symbol.getName()));
        }
    }

    private void initialiseLineTypeToolbar() {
        GridLayout buttonPanel =
                preparePickerPanel(R.id.lineTypeToolbarButtonPanel, LineType.values().length);
        for (LineType lineType : LineType.values()) {
            buttonPanel.addView(
                    createPickerButton(
                            lineType.getButtonViewId(),
                            tintPickerIcon(
                                    AppCompatResources.getDrawable(this, lineType.getDrawableId())),
                            getString(lineType.getStringId())));
        }
    }

    /**
     * Empties one of the pop-up pickers and sets it up as a grid with as many columns as fit across
     * the screen, so that every item can be seen at once without scrolling. The columns are then
     * evened out so the last row isn't left nearly empty.
     */
    private GridLayout preparePickerPanel(int panelId, int itemCount) {
        GridLayout buttonPanel = findViewById(panelId);
        buttonPanel.removeAllViews();

        int buttonSize = getResources().getDimensionPixelSize(R.dimen.picker_button_size);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int maxColumns = Math.max(1, screenWidth / buttonSize);
        int rows = (itemCount + maxColumns - 1) / maxColumns;
        int columns = (itemCount + rows - 1) / rows;
        buttonPanel.setColumnCount(columns);
        return buttonPanel;
    }

    private ImageButton createPickerButton(int viewId, Drawable drawable, String name) {
        int buttonSize = getResources().getDimensionPixelSize(R.dimen.picker_button_size);
        int padding = Math.round(buttonSize * 0.12f);

        ImageButton button = new ImageButton(this);
        button.setId(viewId);
        button.setLayoutParams(
                new GridLayout.LayoutParams(new ViewGroup.LayoutParams(buttonSize, buttonSize)));
        button.setBackgroundResource(R.drawable.picker_button_background);
        button.setPadding(padding, padding, padding, padding);
        button.setScaleType(ImageView.ScaleType.FIT_CENTER);
        button.setImageDrawable(drawable);
        button.setContentDescription(name);
        TooltipCompat.setTooltipText(button, name);
        button.setOnClickListener(this);
        return button;
    }

    private void selectLineType(LineType lineType) {
        SketchPreferences.setSelectedLineType(lineType);

        for (LineType l : LineType.values()) {
            findViewById(l.getButtonViewId()).setSelected(l == lineType);
        }

        ImageButton drawButton = findViewById(R.id.buttonDraw);
        drawButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
        drawButton.setImageDrawable(
                addSelectionBorder(AppCompatResources.getDrawable(this, lineType.getDrawableId())));

        sketchView.setLineType(lineType);
    }

    private void initialiseAreaTypeToolbar() {
        GridLayout buttonPanel =
                preparePickerPanel(R.id.areaTypeToolbarButtonPanel, AreaType.values().length);
        for (AreaType areaType : AreaType.values()) {
            Drawable icon = getAreaTypeIcon(areaType, areaType.getFillDrawableId());
            if (!isDrawnBlue(areaType)) {
                tintPickerIcon(icon);
            }
            buttonPanel.addView(
                    createPickerButton(
                            areaType.getButtonViewId(), icon, getString(areaType.getStringId())));
        }
    }

    private void selectAreaType(AreaType areaType) {
        SketchPreferences.setSelectedAreaType(areaType);

        for (AreaType a : AreaType.values()) {
            findViewById(a.getButtonViewId()).setSelected(a == areaType);
        }

        ImageButton areaButton = findViewById(R.id.buttonArea);
        areaButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
        areaButton.setImageDrawable(
                new LayerDrawable(
                        new Drawable[] {
                            getAreaTypeIcon(areaType, areaType.getDrawableId()),
                            AppCompatResources.getDrawable(this, R.drawable.area_outline)
                        }));

        sketchView.setAreaType(areaType);
    }

    /**
     * One of an area type's icons, in blue for water types when blue water is on, to match how the
     * area will be drawn.
     */
    private Drawable getAreaTypeIcon(AreaType areaType, int drawableId) {
        Drawable drawable = AppCompatResources.getDrawable(this, drawableId).mutate();
        if (isDrawnBlue(areaType)) {
            drawable.setColorFilter(
                    new PorterDuffColorFilter(
                            Colour.WATER.getDrawColour(isDarkModeActive()).intValue,
                            PorterDuff.Mode.SRC_IN));
        }
        return drawable;
    }

    private boolean isDrawnBlue(AreaType areaType) {
        return areaType.isWater() && SketchPreferences.Toggle.BLUE_WATER.isOn();
    }

    /**
     * Colours a picker icon to suit the theme: the icons are drawn in black, which would vanish
     * against the dark picker background in dark mode.
     */
    private Drawable tintPickerIcon(Drawable drawable) {
        Drawable tinted = drawable.mutate();
        tinted.setColorFilter(
                new PorterDuffColorFilter(
                        ContextCompat.getColor(this, R.color.pickerIcon), PorterDuff.Mode.SRC_IN));
        return tinted;
    }

    /** Opens or closes one of the pop-up toolbars; opening it closes any other that is open. */
    private void togglePopUpToolbar(int toolbarId) {
        boolean open = findViewById(toolbarId).getVisibility() != View.VISIBLE;
        for (int id : POP_UP_TOOLBAR_IDS) {
            View toolbar = findViewById(id);
            toolbar.setVisibility(id == toolbarId && open ? View.VISIBLE : View.GONE);
        }
    }

    private void selectSymbol(Symbol symbol) {
        SketchPreferences.setSelectedSymbol(symbol);

        for (Symbol s : Symbol.values()) {
            findViewById(s.getButtonViewId()).setSelected(s == symbol);
        }

        ImageButton symbolButton = findViewById(R.id.buttonSymbol);
        symbolButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
        symbolButton.setImageDrawable(addSelectionBorder(symbol.createDrawable()));

        sketchView.setCurrentSymbol(symbol);
    }

    /**
     * Wraps a toolbar drawable in the border used to show what is currently selected on the draw,
     * area and symbol buttons.
     *
     * <p>The border is given an explicit size rather than being allowed to take the size of the
     * drawable it wraps, so that the box stays the same whatever is selected.
     */
    private Drawable addSelectionBorder(Drawable drawable) {
        ShapeDrawable border =
                new ShapeDrawable(
                        new RoundRectShape(new float[] {5, 5, 5, 5, 5, 5, 5, 5}, null, null));
        border.getPaint().setColor(Color.BLACK);
        border.getPaint().setStyle(Paint.Style.STROKE);
        border.getPaint().setStrokeWidth(10);

        int size = Math.round(getResources().getDimension(R.dimen.toolbar_button_height));
        border.setIntrinsicWidth(size);
        border.setIntrinsicHeight(size);

        LayerDrawable layers = new LayerDrawable(new Drawable[] {drawable, border});
        layers.setLayerSize(0, size, size);
        layers.setLayerSize(1, size, size);
        return layers;
    }

    private void initialiseTools() {
        SketchTool selected = SketchPreferences.getSelectedSketchTool();
        selectSketchTool(selected);

        BrushColour brushColour = SketchPreferences.getSelectedBrushColour();
        selectBrushColour(brushColour);

        Symbol.setResources(getResources());
        Symbol selectedSymbol = SketchPreferences.getSelectedSymbol();
        selectSymbol(selectedSymbol);

        LineType selectedLineType = SketchPreferences.getSelectedLineType();
        selectLineType(selectedLineType);

        AreaType selectedAreaType = SketchPreferences.getSelectedAreaType();
        selectAreaType(selectedAreaType);
    }

    private void selectSketchTool(SketchTool toSelect) {

        SketchPreferences.setSelectedSketchTool(toSelect);
        sketchView.setSketchTool(toSelect);

        for (SketchTool sketchTool : SketchTool.values()) {

            View button = findViewById(sketchTool.getId());
            if (button == null) {
                continue; // some tools are not linked to a toolbar button
            }

            if (sketchTool == toSelect) {
                button.getBackground()
                        .setColorFilter(buttonHighlightColour, PorterDuff.Mode.SRC_ATOP);
            } else {
                button.getBackground().clearColorFilter();
            }
        }
    }

    private void selectBrushColour(BrushColour toSelect) {

        SketchPreferences.setSelectedBrushColour(toSelect);
        sketchView.setBrushColour(toSelect);

        for (BrushColour brushColour : BrushColour.values()) {

            View button = findViewById(brushColour.getId());
            if (brushColour == toSelect) {
                button.getBackground()
                        .setColorFilter(buttonHighlightColour, PorterDuff.Mode.SRC_ATOP);
                button.invalidate();
            } else {
                button.getBackground().clearColorFilter();
            }
        }
    }

    @Override
    protected void invalidateView() {
        sketchView.invalidate();
    }

    @Override
    public void onNewCrossSection(Station station) {
        sketchView.handleNewCrossSection(station);
    }

    @Override
    public boolean hasCrossSection(Station station) {
        return getSketch().getCrossSectionDetail(station) != null;
    }

    @Override
    public boolean hasRotatableCrossSection(Station station) {
        CrossSectionDetail detail = getSketch().getCrossSectionDetail(station);
        return detail != null && detail.getCrossSection().isRotatable();
    }

    @Override
    public void onNewHorizontalCrossSection(Station station) {
        sketchView.handleNewCrossSection(station, CrossSection.Orientation.HORIZONTAL);
    }

    @Override
    public void onDeleteCrossSection(Station station) {
        Sketch sketch = getSketch();
        CrossSectionDetail detail = sketch.getCrossSectionDetail(station);
        if (detail == null) {
            return;
        }
        sketch.deleteDetail(detail);
        getSurveyManager().broadcastSurveyUpdated();
        invalidateView();
    }

    @Override
    public void onRotateCrossSection(Station station) {
        sketchView.handleRotateCrossSection(station);
    }

    @Override
    public void onEditCrossSection(Station station) {
        CrossSectionDetail detail = getSketch().getCrossSectionDetail(station);
        if (detail == null) {
            return;
        }
        sketchView.launchCrossSectionEditor(detail);
    }

    @Override
    public void onRenameStation(Station station) {
        LegDialogs.renameStation(this, getSurvey(), station);
    }
}

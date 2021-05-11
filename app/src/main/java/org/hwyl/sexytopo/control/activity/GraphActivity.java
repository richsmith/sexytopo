package org.hwyl.sexytopo.control.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;

import org.apache.commons.lang3.ArrayUtils;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.graph.GraphView;
import org.hwyl.sexytopo.control.graph.StationContextMenu;
import org.hwyl.sexytopo.control.util.SurveyStats;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.BrushColour;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.SketchTool;
import org.hwyl.sexytopo.model.sketch.Symbol;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public abstract class GraphActivity extends SexyTopoActivity
        implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {

    private static final double ZOOM_INCREMENT = 1.1;
    private static final double ZOOM_DECREMENT = 0.9;


    private static final SketchTool DEFAULT_SKETCH_TOOL_SELECTION = SketchTool.MOVE;
    private static final BrushColour DEFAULT_BRUSH_COLOUR_SELECTION = BrushColour.BLACK;

    public static final String DISPLAY_PREFERENCES_KEY = "display";
    private static final String SKETCH_TOOL_PREFERENCE_KEY = "SKETCH_TOOL";
    private static final String BRUSH_COLOUR_PREFERENCE_KEY = "BRUSH_COLOUR";
    private static final String SYMBOL_PREFERENCE_KEY = "SYMBOL";

    private static final int[] SKETCH_BUTTON_IDS = new int[] {
            R.id.buttonDraw,
            R.id.buttonErase,
            R.id.buttonText,
            R.id.buttonSymbol,
            R.id.buttonUndo,
            R.id.buttonRedo,
            R.id.buttonBlack,
            R.id.buttonBrown,
            R.id.buttonGrey,
            R.id.buttonRed,
            R.id.buttonOrange,
            R.id.buttonBlue,
            R.id.buttonGreen,
            R.id.buttonPurple
    };

    private static final int[] CONTROL_BUTTON_IDS = new int[] {
            R.id.buttonMove,
            R.id.buttonSelect,
            R.id.buttonZoomIn,
            R.id.buttonZoomOut,
            R.id.buttonMenu
    };

    private static final int[] BUTTON_IDS =
            ArrayUtils.addAll(SKETCH_BUTTON_IDS, CONTROL_BUTTON_IDS);

    public enum DisplayPreference {
        AUTO_RECENTRE(R.id.buttonAutoRecentre, false),
        SNAP_TO_LINES(R.id.buttonSnapToLines, false),
        FADE_NON_ACTIVE(R.id.buttonFadeNonActive, false),
        SHOW_GRID(R.id.buttonShowGrid, true),
        SHOW_SPLAYS(R.id.buttonShowSplays, true),
        SHOW_SKETCH(R.id.buttonShowSketch, true),
        SHOW_STATION_LABELS(R.id.buttonShowStationLabels, true),
        SHOW_CONNECTIONS(R.id.buttonShowConnections, true);

        private int controlId;
        private boolean defaultValue;

        DisplayPreference(int id, boolean defaultValue) {
            this.controlId = id;
            this.defaultValue = defaultValue;
        }

        public boolean getDefault() {
            return defaultValue;
        }
    }

    private GraphView graphView;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BroadcastReceiver updatedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                syncGraphWithSurvey();
            }
        };
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(updatedReceiver,
                new IntentFilter(SexyTopo.SURVEY_UPDATED_EVENT));

        BroadcastReceiver createdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleAutoRecentre();
            }
        };
        broadcastManager.registerReceiver(createdReceiver,
                new IntentFilter(SexyTopo.NEW_STATION_CREATED_EVENT));

        setContentView(R.layout.activity_graph);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        for (int id : BUTTON_IDS) {
            View button = findViewById(id);
            button.setOnClickListener(this);
        }

        View button = findViewById(R.id.buttonSymbolToolbarClose);
        button.setOnClickListener(this);

        graphView = findViewById(R.id.graphView);
        graphView.setActivity(this);

        preferences = getSharedPreferences("display", Context.MODE_PRIVATE);

        graphView.post(new Runnable() {
            @Override // Needs to be threaded so it is only run once we know height and width
            public void run() {
                setViewLocation();
            }
        });
    }


    private void handleAutoRecentre() {
        if (graphView.getDisplayPreference(DisplayPreference.AUTO_RECENTRE)) {
            graphView.centreViewOnActiveStation();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        syncGraphWithSurvey();
        initialiseSketchTool();
        initialiseBrushColour();
        // initialiseSymbolToolbar();
        // initialiseSymbolTool();

        setSketchButtonsStatus();
        setViewLocation();
    }

    private void setViewLocation() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.getString(SexyTopo.JUMP_TO_STATION) != null) {
            String requestedStationName = bundle.getString(SexyTopo.JUMP_TO_STATION);
            Station requestedStation = getSurvey().getStationByName(requestedStationName);
            graphView.centreViewOnStation(requestedStation);
        } else {
            graphView.centreViewOnActiveStation();
        }
    }


    private void syncGraphWithSurvey() {
        Survey survey = getSurvey();
        graphView.initialisePaint();
        graphView.setProjectionType(getProjectionType());
        graphView.setProjection(getProjection(survey));
        graphView.setSurvey(survey);
        graphView.setSketch(getSketch(survey));

        double surveyLength = SurveyStats.calcTotalLength(survey);
        double surveyHeight = SurveyStats.calcHeightRange(survey);
        graphView.setCachedStats(surveyLength, surveyHeight);

        graphView.invalidate();
    }

    public abstract Sketch getSketch(Survey survey);

    public Space<Coord2D> getProjection(Survey survey) {
        return getProjectionType().project(survey);
    }

    public abstract Projection2D getProjectionType();


    @Override
    public void onClick(View view) {
        int id = view.getId();
        handleAction(id);
    }


    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();

        switch(id) {
            case R.id.buttonSnapToLines:
                setDisplayPreference(DisplayPreference.SNAP_TO_LINES, !item.isChecked());
                return true;
            case R.id.buttonShowGrid:
                setDisplayPreference(DisplayPreference.SHOW_GRID, !item.isChecked());
                graphView.invalidate();
                return true;
            case R.id.buttonFadeNonActive:
                setDisplayPreference(DisplayPreference.FADE_NON_ACTIVE, !item.isChecked());
                graphView.invalidate();
                return true;
            case R.id.buttonShowSplays:
                setDisplayPreference(DisplayPreference.SHOW_SPLAYS, !item.isChecked());
                graphView.invalidate();
                return true;
            case R.id.buttonShowSketch:
                setDisplayPreference(DisplayPreference.SHOW_SKETCH, !item.isChecked());
                setSketchButtonsStatus();
                graphView.invalidate();
                return true;
            case R.id.buttonShowStationLabels:
                setDisplayPreference(DisplayPreference.SHOW_STATION_LABELS, !item.isChecked());
                graphView.invalidate();
                return true;
            case R.id.buttonShowConnections:
                setDisplayPreference(DisplayPreference.SHOW_CONNECTIONS, !item.isChecked());
                graphView.invalidate();
                return true;
            case R.id.buttonAutoRecentre:
                setDisplayPreference(DisplayPreference.AUTO_RECENTRE, !item.isChecked());
                graphView.invalidate();
                return true;
            default:
                return handleAction(id);
        }

    }


    private void setSketchButtonsStatus() {
       boolean isEnabled = graphView.getDisplayPreference(DisplayPreference.SHOW_SKETCH);
        for (int id : SKETCH_BUTTON_IDS) {
            ImageButton button = findViewById(id);
            button.setEnabled(isEnabled);
        }

        if (!isEnabled) {
            graphView.setSketchTool(SketchTool.MOVE);
        }
    }


    private void setDisplayPreference(DisplayPreference preference, boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(preference.toString(), value);
        editor.apply();
    }


    public boolean handleAction(int id) {

        GraphView graphView = findViewById(R.id.graphView);
        graphView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        for (BrushColour brushColour: BrushColour.values()) {
            if (brushColour.getId() == id) {
                selectBrushColour(brushColour);
                if (!graphView.getSketchTool().usesColour()) {
                    selectSketchTool(SketchTool.DRAW);
                }
                return true;
            }
        }

        SketchTool alreadySelectedTool = graphView.getSketchTool();
        for (SketchTool sketchTool: SketchTool.values()) {
            if (sketchTool.getId() == id) {
                if (alreadySelectedTool == SketchTool.SYMBOL && sketchTool == SketchTool.SYMBOL) {
                    toggleSymbolToolbar();
                } else {
                    selectSketchTool(sketchTool);
                }
                return true;
            }
        }

        for (Symbol symbol : Symbol.values()) {
            if (id == symbol.getBitmapId()) {
                selectSketchTool(SketchTool.SYMBOL);
                selectSymbol(symbol);
            }
        }
        if (id == R.id.buttonSymbolToolbarClose) {
            setSymbolToolbarOpen(false);
        }

        switch(id) {
            case R.id.buttonZoomIn:
                graphView.adjustZoomBy(ZOOM_INCREMENT);
                graphView.invalidate();
                break;
            case R.id.buttonZoomOut:
                graphView.adjustZoomBy(ZOOM_DECREMENT);
                graphView.invalidate();
                break;
            case R.id.buttonUndo:
                graphView.undo();
                break;
            case R.id.buttonRedo:
                graphView.redo();
                break;
            case R.id.buttonMenu:
                openDisplayMenu();
                break;
            case R.id.buttonCentreView:
                graphView.centreViewOnActiveStation();
                graphView.invalidate();
                break;
            case R.id.buttonDeleteLastLeg:
                getSurvey().undoAddLeg();
                syncGraphWithSurvey();
                break;
        }
        return true;
    }


    private void openDisplayMenu() {

        View view = findViewById(R.id.buttonMenu);
        PopupMenu popup = new PopupMenu(this, view);
        Menu menu = popup.getMenu();
        popup.getMenuInflater().inflate(R.menu.drawing, menu);
        popup.setOnMenuItemClickListener(this);

        SharedPreferences preferences = getSharedPreferences("display", Context.MODE_PRIVATE);

        for (DisplayPreference preference : DisplayPreference.values()) {
            MenuItem menuItem = menu.findItem(preference.controlId);
            boolean isSelected =
                    preferences.getBoolean(preference.toString(), preference.defaultValue);
            menuItem.setChecked(isSelected);
        }

        popup.show();
    }


    private void initialiseSymbolToolbar() {
        Symbol.setResources(getResources());

        LinearLayout buttonPanel = findViewById(R.id.symbolToolbarButtonPanel);
        buttonPanel.removeAllViews();

        for (Symbol symbol : Symbol.values()) {
            LayoutInflater inflater = LayoutInflater.from(this);
            LinearLayout linearLayout = (LinearLayout)
                (inflater.inflate(R.layout.tool_button, null));
            ImageButton imageButton = (ImageButton)linearLayout.getChildAt(0);
            imageButton.setId(symbol.getBitmapId()); // bit hacky :P
            imageButton.setImageBitmap(symbol.getButtonBitmap());
            imageButton.setOnClickListener(this);
            buttonPanel.addView(linearLayout);
        }
        buttonPanel.invalidate();
    }


    private void toggleSymbolToolbar() {
        View toolbar = findViewById(R.id.symbolToolbar);
        boolean isVisible = toolbar.getVisibility() == View.VISIBLE;
        setSymbolToolbarOpen(!isVisible);
    }

    private void setSymbolToolbarOpen(boolean setOpen) {
        View toolbar = findViewById(R.id.symbolToolbar);
        toolbar.setVisibility(setOpen? View.VISIBLE : View.GONE);
    }


    private void initialiseSymbolTool() {
        Symbol.setResources(getResources());
        String selected = preferences.getString(SYMBOL_PREFERENCE_KEY, null);

        Symbol selectedSymbol = (selected == null)?
            Symbol.getDefault() : Symbol.valueOf(selected);
        selectSymbol(selectedSymbol);
    }


    private void selectSymbol(Symbol symbol) {
        SharedPreferences preferences =
                getSharedPreferences(SYMBOL_PREFERENCE_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(SYMBOL_PREFERENCE_KEY, symbol.toString());
        editor.apply();

        ImageButton selectedSymbolButton = findViewById(symbol.getBitmapId());
        selectedSymbolButton.getBackground().setColorFilter(0xffffffff, PorterDuff.Mode.SRC_ATOP);

        ImageButton symbolButton = findViewById(R.id.buttonSymbol);
        symbolButton.setScaleType(ImageView.ScaleType.FIT_CENTER);

        Symbol.setResources(this.getResources());
        Bitmap buttonBitmap = symbol.getButtonBitmap();
        symbolButton.setImageBitmap(buttonBitmap);

        graphView.setCurrentSymbol(symbol);

    }


    private void initialiseSketchTool() {
        SharedPreferences preferences =
                getSharedPreferences(DISPLAY_PREFERENCES_KEY, Context.MODE_PRIVATE);
        String selected = preferences.getString(SKETCH_TOOL_PREFERENCE_KEY, null);

        SketchTool sketchTool = (selected == null)?
                DEFAULT_SKETCH_TOOL_SELECTION : SketchTool.valueOf(selected);

        selectSketchTool(sketchTool);
    }


    private void selectSketchTool(SketchTool toSelect) {

        SharedPreferences preferences =
                getSharedPreferences(DISPLAY_PREFERENCES_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(SKETCH_TOOL_PREFERENCE_KEY, toSelect.toString());
        editor.apply();

        graphView.setSketchTool(toSelect);

        for (SketchTool sketchTool : SketchTool.values()) {

            View button = findViewById(sketchTool.getId());
            if (button == null) {
                continue; // some tools are not linked to a toolbar button
            }

            if (sketchTool == toSelect) {
                button.getBackground().setColorFilter(0xffffffff, PorterDuff.Mode.SRC_ATOP);
            } else {
                button.getBackground().clearColorFilter();
            }
        }
    }



    private void initialiseBrushColour() {
        SharedPreferences preferences =
                getSharedPreferences(DISPLAY_PREFERENCES_KEY, Context.MODE_PRIVATE);
        String selected = preferences.getString(BRUSH_COLOUR_PREFERENCE_KEY, null);

        BrushColour brushColour = (selected == null)?
                DEFAULT_BRUSH_COLOUR_SELECTION : BrushColour.valueOf(selected);

        selectBrushColour(brushColour);
    }

    private void selectBrushColour(BrushColour toSelect) {

        SharedPreferences preferences =
                getSharedPreferences(DISPLAY_PREFERENCES_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(BRUSH_COLOUR_PREFERENCE_KEY, toSelect.toString());
        editor.apply();

        graphView.setBrushColour(toSelect);

        for (BrushColour brushColour : BrushColour.values()) {

            View button = findViewById(brushColour.getId());
            if (brushColour == toSelect) {
                button.getBackground().setColorFilter(0xffffffff, PorterDuff.Mode.SRC_ATOP);
                button.invalidate();
            } else {
                button.getBackground().clearColorFilter();
            }
        }
    }

    public PopupWindow getContextMenu(Station station, View.OnClickListener listener) {
        return new StationContextMenu().getFakeStationContextMenu(this, station, listener);
    }

}

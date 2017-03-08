package org.hwyl.sexytopo.control.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupMenu;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.graph.GraphView;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;


public abstract class GraphActivity extends SexyTopoActivity
        implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {

    private static final double ZOOM_INCREMENT = 4.0;

    private static final GraphView.SketchTool DEFAULT_SKETCH_TOOL_SELECTION =
            GraphView.SketchTool.MOVE;
    private static final GraphView.BrushColour DEFAULT_BRUSH_COLOUR_SELECTION =
            GraphView.BrushColour.BLACK;


    public static final String DISPLAY_PREFERENCES_KEY = "display";
    private static final String SKETCH_TOOL_PREFERENCE_KEY = "SKETCH_TOOL";
    private static final String BRUSH_COLOUR_PREFERENCE_KEY = "BRUSH_COLOUR";


    private static final int[] BUTTON_IDS = new int[] {
        R.id.buttonDraw,
        R.id.buttonMove,
        R.id.buttonText,
        R.id.buttonErase,
        R.id.buttonSelect,
        R.id.buttonZoomIn,
        R.id.buttonZoomOut,
        R.id.buttonUndo,
        R.id.buttonRedo,
        R.id.buttonMenu,
        R.id.buttonBlack,
        R.id.buttonBrown,
        R.id.buttonOrange,
        R.id.buttonBlue,
        R.id.buttonGreen,
        R.id.buttonPurple
    };


    public enum DisplayPreference {
        AUTO_RECENTRE(R.id.buttonAutoRecentre, true),
        SNAP_TO_LINES(R.id.buttonSnapToLines, true),
        SHOW_GRID(R.id.buttonShowGrid, true),
        SHOW_SPLAYS(R.id.buttonShowSplays, true),
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

        graphView = (GraphView)(findViewById(R.id.graphView));
        graphView.setActivity(this);

        preferences = getSharedPreferences("display", Context.MODE_PRIVATE);

        graphView.centreViewOnActiveStation();
    }


    private void handleAutoRecentre() {
        if (preferences.getBoolean(DisplayPreference.AUTO_RECENTRE.toString(), false)) {
            graphView.centreViewOnActiveStation();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        syncGraphWithSurvey();
        initialiseSketchTool();
        initialiseBrushColour();
    }


    private void syncGraphWithSurvey() {
        Survey survey = getSurvey();
        graphView.initialise();
        graphView.setProjection(getProjection(survey));
        graphView.setSurvey(survey);
        graphView.setSketch(getSketch(survey));
        graphView.invalidate();
    }

    public abstract Sketch getSketch(Survey survey);

    public abstract Space<Coord2D> getProjection(Survey survey);


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
            case R.id.buttonShowSplays:
                setDisplayPreference(DisplayPreference.SHOW_SPLAYS, !item.isChecked());
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

    private void setDisplayPreference(DisplayPreference preference, boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(preference.toString(), value);
        editor.commit();
    }


    public boolean handleAction(int id) {

        GraphView graphView = (GraphView)(findViewById(R.id.graphView));
        graphView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        for (GraphView.SketchTool sketchTool: GraphView.SketchTool.values()) {
            if (sketchTool.getId() == id) {
                selectSketchTool(sketchTool);
                return true;
            }
        }

        for (GraphView.BrushColour brushColour: GraphView.BrushColour.values()) {
            if (brushColour.getId() == id) {
                selectBrushColour(brushColour);
                if (!graphView.getSketchTool().usesColour()) {
                    selectSketchTool(GraphView.SketchTool.DRAW);
                }
                return true;
            }
        }

        switch(id) {
            case R.id.buttonZoomIn:
                graphView.zoom(ZOOM_INCREMENT);
                graphView.invalidate();
                break;
            case R.id.buttonZoomOut:
                graphView.zoom(-ZOOM_INCREMENT);
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
                getSurvey().undoLeg();
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


    private void initialiseSketchTool() {
        SharedPreferences preferences =
                getSharedPreferences(DISPLAY_PREFERENCES_KEY, Context.MODE_PRIVATE);
        String selected = preferences.getString(SKETCH_TOOL_PREFERENCE_KEY, null);

        GraphView.SketchTool sketchTool = (selected == null)?
                DEFAULT_SKETCH_TOOL_SELECTION :
                GraphView.SketchTool.valueOf(selected);

        selectSketchTool(sketchTool);
    }


    private void selectSketchTool(GraphView.SketchTool toSelect) {

        SharedPreferences preferences =
                getSharedPreferences(DISPLAY_PREFERENCES_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(SKETCH_TOOL_PREFERENCE_KEY, toSelect.toString());
        editor.commit();

        graphView.setSketchTool(toSelect);

        for (GraphView.SketchTool sketchTool : GraphView.SketchTool.values()) {

            View button = findViewById(sketchTool.getId());
            if (button == null) {
                continue; // some tools are not linked to a toolbar button
            }

            if (sketchTool == toSelect) {
                button.setHovered(true);
                button.setHapticFeedbackEnabled(true);
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

        GraphView.BrushColour brushColour = (selected == null)?
                DEFAULT_BRUSH_COLOUR_SELECTION :
                GraphView.BrushColour.valueOf(selected);

        selectBrushColour(brushColour);
    }

    private void selectBrushColour(GraphView.BrushColour toSelect) {


        SharedPreferences preferences =
                getSharedPreferences(DISPLAY_PREFERENCES_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(BRUSH_COLOUR_PREFERENCE_KEY, toSelect.toString());
        editor.commit();

        graphView.setBrushColour(toSelect);

        for (GraphView.BrushColour brushColour : GraphView.BrushColour.values()) {

            View button = findViewById(brushColour.getId());
            ColorFilter filter = null;
            if (brushColour == toSelect) {
                button.getBackground().setColorFilter(0xffffffff, PorterDuff.Mode.SRC_ATOP);
                button.invalidate();
            } else {
                button.getBackground().clearColorFilter();
            }
        }
    }

}

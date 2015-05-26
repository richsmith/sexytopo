package org.hwyl.sexytopo.control.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.LightingColorFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
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

import java.util.HashMap;
import java.util.Map;

/**
 * Created by rls on 13/10/14.
 */
public abstract class GraphActivity extends SexyTopoActivity
        implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {

    private static final double ZOOM_INCREMENT = 4.0;

    private static final int DEFAULT_SKETCH_TOOL_SELECTION = R.id.buttonMove;
    private static final int DEFAULT_BRUSH_COLOUR_SELECTION = R.id.buttonBlack;


    private static final int[] BUTTON_IDS = new int[] {
        R.id.buttonDraw,
        R.id.buttonMove,
        R.id.buttonErase,
        R.id.buttonSelect,
        R.id.buttonZoomIn,
        R.id.buttonZoomOut,
        R.id.buttonUndo,
        R.id.buttonRedo,
        R.id.buttonMenu,
        R.id.buttonBlack,
        R.id.buttonOrange,
        R.id.buttonBlue,
        R.id.buttonGreen,
        R.id.buttonPurple
    };

    private GraphView graphView;

    private static final Map<Integer, GraphView.SketchTool> BUTTON_ID_TO_SKETCH_TOOL
            = new HashMap<Integer, GraphView.SketchTool>() {{
        put(R.id.buttonMove, GraphView.SketchTool.MOVE);
        put(R.id.buttonDraw, GraphView.SketchTool.DRAW);
        put(R.id.buttonErase, GraphView.SketchTool.ERASE);
        put(R.id.buttonSelect, GraphView.SketchTool.SELECT);
    }};

    private static final Map<Integer, GraphView.BrushColour> BUTTON_ID_TO_BRUSH_COLOUR
            = new HashMap<Integer, GraphView.BrushColour>() {{
        put(R.id.buttonBlack, GraphView.BrushColour.BLACK);
        put(R.id.buttonOrange, GraphView.BrushColour.ORANGE);
        put(R.id.buttonBlue, GraphView.BrushColour.BLUE);
        put(R.id.buttonGreen, GraphView.BrushColour.GREEN);
        put(R.id.buttonPurple, GraphView.BrushColour.PURPLE);
    }};

    public enum DisplayPreference {
        SHOW_GRID(R.id.buttonShowGrid, true),
        SHOW_SPLAYS(R.id.buttonShowSplays, true),
        SHOW_STATION_LABELS(R.id.buttonShowStationLabels, true);

        private int controlId;
        private boolean defaultValue;

        private DisplayPreference(int id, boolean defaultValue) {
            this.controlId = id;
            this.defaultValue = defaultValue;
        }

        public boolean getDefault() {
            return defaultValue;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                syncGraphWithSurvey();
            }
        };
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(receiver, new IntentFilter(SexyTopo.SURVEY_UPDATED_EVENT));

        setContentView(R.layout.activity_graph);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        for (int id : BUTTON_IDS) {
            View button = findViewById(id);
            button.setOnClickListener(this);
        }

        graphView = (GraphView)(findViewById(R.id.graphView));
        syncGraphWithSurvey();

        selectSketchTool(DEFAULT_SKETCH_TOOL_SELECTION);
        selectBrushColour(DEFAULT_BRUSH_COLOUR_SELECTION);
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    private void syncGraphWithSurvey() {
        Survey survey = getSurvey();
        graphView.setSurvey(survey);
        graphView.setProjection(getProjection(survey));
        graphView.setSketch(getSketch(survey));
        graphView.invalidate();
    }

    protected abstract Sketch getSketch(Survey survey);
    protected abstract Space<Coord2D> getProjection(Survey survey);


    @Override
    public void onClick(View view) {
        int id = view.getId();
        handleAction(id);
    }


    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();

        switch(id) {

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
            default:
                return handleAction(id);
        }

    }

    private void setDisplayPreference(DisplayPreference preference, boolean value) {
        SharedPreferences preferences = getSharedPreferences("display", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(preference.toString(), value);
        editor.commit();
    }

    public boolean handleAction(int id) {

        GraphView graphView = (GraphView)(findViewById(R.id.graphView));

        switch(id) {
            case R.id.buttonDraw:
            case R.id.buttonMove:
            case R.id.buttonErase:
            case R.id.buttonSelect:
                selectSketchTool(id);
                break;
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
            case R.id.buttonBlack:
            case R.id.buttonOrange:
            case R.id.buttonBlue:
            case R.id.buttonGreen:
            case R.id.buttonPurple:
                selectBrushColour(id);
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


    private void selectBrushColour(int selectedId) {

        GraphView.BrushColour brushColour = BUTTON_ID_TO_BRUSH_COLOUR.get(selectedId);
        graphView.setBrushColour(brushColour);

        for (int id : BUTTON_ID_TO_BRUSH_COLOUR.keySet()) {
            View button = findViewById(id);

            if (id == selectedId) {
                button.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, 0xFF004400));
            } else {
                button.getBackground().setColorFilter(null);
            }
        }
    }


    private void selectSketchTool(int selectedId) {

        GraphView.SketchTool sketchTool = BUTTON_ID_TO_SKETCH_TOOL.get(selectedId);
        graphView.setSketchTool(sketchTool);

        for (int id : BUTTON_ID_TO_SKETCH_TOOL.keySet()) {
            View button = findViewById(id);

            if (id == selectedId) {
                button.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, 0xFF440000));
            } else {
                button.getBackground().setColorFilter(null);
            }
        }


    }

}

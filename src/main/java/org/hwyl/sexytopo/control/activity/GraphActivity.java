package org.hwyl.sexytopo.control.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupMenu;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopo;
import org.hwyl.sexytopo.control.GraphView;
import org.hwyl.sexytopo.model.Survey;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.Sketch;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by rls on 13/10/14.
 */
public abstract class GraphActivity extends SexyTopoActivity implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {

    private static final double ZOOM_INCREMENT = 1.0;


    private static final int[] BUTTON_IDS = new int[] {
        R.id.buttonDraw,
        R.id.buttonMove,
        R.id.buttonErase,
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

    private Map<Integer, GraphView.SketchTool> buttonIdToSketchTool
            = new HashMap<Integer, GraphView.SketchTool>() {{
        put(R.id.buttonMove, GraphView.SketchTool.MOVE);
        put(R.id.buttonDraw, GraphView.SketchTool.DRAW);
        put(R.id.buttonErase, GraphView.SketchTool.ERASE);
    }};

    private Map<Integer, GraphView.BrushColour> buttonIdToBrushColour
            = new HashMap<Integer, GraphView.BrushColour>() {{
        put(R.id.buttonBlack, GraphView.BrushColour.BLACK);
        put(R.id.buttonOrange, GraphView.BrushColour.ORANGE);
        put(R.id.buttonBlue, GraphView.BrushColour.BLUE);
        put(R.id.buttonGreen, GraphView.BrushColour.GREEN);
        put(R.id.buttonPurple, GraphView.BrushColour.PURPLE);
    }};

    private int DEFAULT_SKETCH_TOOL_SELECTION = R.id.buttonMove;
    private int DEFAULT_BRUSH_COLOUR_SELECTION = R.id.buttonBlack;

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

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_graph);

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
        return handleAction(id);
    }

    public boolean handleAction(int id) {
        GraphView graphView = (GraphView)(findViewById(R.id.graphView));
        switch(id) {

            case R.id.buttonColour:
                Dialog dialog = createColourSelectionDialog();
                dialog.show();
                break;
            case R.id.buttonDraw:
            case R.id.buttonMove:
            case R.id.buttonErase:
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
                View v = findViewById(R.id.buttonMenu);
                PopupMenu popup = new PopupMenu(this, v);
                popup.getMenuInflater().inflate(R.menu.drawing, popup.getMenu());
                popup.setOnMenuItemClickListener(this);
                popup.show();
                break;
        }
        return true;
    }


    private void selectBrushColour(int selectedId) {

        GraphView.BrushColour brushColour = buttonIdToBrushColour.get(selectedId);
        graphView.setBrushColour(brushColour);

        for (int id : buttonIdToBrushColour.keySet()) {
            View button = findViewById(id);

            if (id == selectedId) {
                button.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, 0xFF004400));
            } else {
                button.getBackground().setColorFilter(null);
            }
        }
    }


    private void selectSketchTool(int selectedId) {

        GraphView.SketchTool sketchTool = buttonIdToSketchTool.get(selectedId);
        graphView.setSketchTool(sketchTool);

        for (int id : buttonIdToSketchTool.keySet()) {
            View button = findViewById(id);

            if (id == selectedId) {
                button.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, 0xFF440000));
            } else {
                button.getBackground().setColorFilter(null);
            }
        }


    }


    private Dialog createColourSelectionDialog() {

        final Map<String, Integer> coloursToId = new HashMap<String, Integer>() {{
            put("Black", Color.BLACK);
            put("Cyan", Color.CYAN);
            put("Green", Color.GREEN);
            put("Blue", Color.BLUE);
            put("Magenta", Color.MAGENTA);
        }};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Pick colour") //R.string.pick_color)
           .setItems(coloursToId.keySet().toArray(new String[]{}), new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int which) {

                   Sketch sketch = getSketch(getSurvey());
                   String selectedColour = coloursToId.keySet().toArray(new String[]{})[which];
                   sketch.setActiveColour(coloursToId.get(selectedColour));

               }
           });

        return builder.create();

    }


}

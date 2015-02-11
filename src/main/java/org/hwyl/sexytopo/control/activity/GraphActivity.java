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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by rls on 13/10/14.
 */
public abstract class GraphActivity extends SexyTopoActivity implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {
    private static final double ZOOM_INCREMENT = 1.0;

    private GraphView graphView;

    private Map<GraphView.SketchTool, Integer> sketchToolToButtonId
            = new EnumMap<GraphView.SketchTool, Integer>(GraphView.SketchTool.class) {{
        put(GraphView.SketchTool.MOVE, R.id.buttonMove);
        put(GraphView.SketchTool.DRAW, R.id.buttonDraw);
        put(GraphView.SketchTool.ERASE, R.id.buttonErase);
    }};

    private GraphView.SketchTool DEFAULT_SKETCH_TOOL = GraphView.SketchTool.MOVE;

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

        View drawButton = findViewById(R.id.buttonDraw);
        drawButton.setOnClickListener(this);
        View moveButton = findViewById(R.id.buttonMove);
        moveButton.setOnClickListener(this);
        View eraseButton = findViewById(R.id.buttonErase);
        eraseButton.setOnClickListener(this);
        View zoomInButton = findViewById(R.id.buttonZoomIn);
        zoomInButton.setOnClickListener(this);
        View zoomOutButton = findViewById(R.id.buttonZoomOut);
        zoomOutButton.setOnClickListener(this);
        View undoButton = findViewById(R.id.buttonUndo);
        undoButton.setOnClickListener(this);
        View redoButton = findViewById(R.id.buttonRedo);
        redoButton.setOnClickListener(this);
        View menuButton = findViewById(R.id.buttonMenu);
        menuButton.setOnClickListener(this);

        graphView = (GraphView)(findViewById(R.id.graphView));
        syncGraphWithSurvey();

        setSketchTool(DEFAULT_SKETCH_TOOL);
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
                setSketchTool(GraphView.SketchTool.DRAW);
                break;
            case R.id.buttonMove:
                setSketchTool(GraphView.SketchTool.MOVE);
                break;
            case R.id.buttonErase:
                setSketchTool(GraphView.SketchTool.ERASE);
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

    private void setSketchTool(GraphView.SketchTool selectedTool) {
        graphView.currentSketchTool = selectedTool;


        for (GraphView.SketchTool sketchTool : sketchToolToButtonId.keySet()) {

            int id = sketchToolToButtonId.get(sketchTool);
            View button = findViewById(id);

            if (sketchTool == selectedTool) {
                button.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, 0xFFAA0000));
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

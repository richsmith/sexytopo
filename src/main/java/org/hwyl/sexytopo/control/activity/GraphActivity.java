package org.hwyl.sexytopo.control.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
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

    private GraphView graphView;

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
        View zoomInButton = findViewById(R.id.buttonZoomIn);
        zoomInButton.setOnClickListener(this);
        View zoomOutButton = findViewById(R.id.buttonZoomOut);
        zoomOutButton.setOnClickListener(this);
        View undoButton = findViewById(R.id.buttonUndo);
        undoButton.setOnClickListener(this);
        //View redoButton = findViewById(R.id.buttonRedo);
        //redoButton.setOnClickListener(this);
        View menuButton = findViewById(R.id.buttonMenu);
        menuButton.setOnClickListener(this);

        graphView = (GraphView)(findViewById(R.id.graphView));
        syncGraphWithSurvey();
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
                //DialogFragment dialog = new ColourDialog();
                Dialog d = x();
                d.show();
                //dialog.show();
                break;
            case R.id.buttonDraw:
                graphView.currentSketchTool = GraphView.SketchTool.DRAW;
                break;
            case R.id.buttonMove:
                graphView.currentSketchTool = GraphView.SketchTool.MOVE;
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


    private Dialog x() {
        //final String[] colours = new String[]{"Black", "Blue", "Green", "Orange"};
        final Map<String, Integer> coloursToId = new HashMap<String, Integer>() {{
            put("Black", Color.BLACK);
            put("Cyan", Color.CYAN);
            put("Green", Color.GREEN);
            put("Blue", Color.BLUE);
            put("Magenta", Color.MAGENTA);
        }};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Pick colour") //R.string.pick_color)
           .setItems(coloursToId.keySet().toArray(new String[]{}) /*R.array.colors_array*/, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int which) {

                   Sketch sketch = getSketch(getSurvey());
                   String selectedColour = coloursToId.keySet().toArray(new String[]{})[which];
                   sketch.setActiveColour(coloursToId.get(selectedColour));

               }
           });

        return builder.create();

    }


/*
    public class ColourDialog extends DialogFragment {
        public ColourDialog() {
            //this
            //
            // .
        }
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("pick a colour")
                    .setItems(new String[]{"orange", "black", "fucking Google", "blue", "pink", "red", "green"}, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // The 'which' argument contains the index position
                            // of the selected item
                        }
                    });
            return builder.create();
        }
    }*/

}

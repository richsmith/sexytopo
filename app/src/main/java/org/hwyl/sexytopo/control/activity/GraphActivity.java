package org.hwyl.sexytopo.control.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.apache.commons.lang3.ArrayUtils;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.graph.GraphView;
import org.hwyl.sexytopo.control.graph.StationContextMenu;
import org.hwyl.sexytopo.control.util.SketchPreferences;
import org.hwyl.sexytopo.control.util.SurveyStats;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.BrushColour;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.SketchTool;
import org.hwyl.sexytopo.model.sketch.Symbol;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;


public abstract class GraphActivity extends SexyTopoActivity
        implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {

    private static final float ZOOM_INCREMENT = 1.1f;
    private static final float ZOOM_DECREMENT = 0.9f;


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


    private GraphView graphView;

    private BroadcastReceiver updatedReceiver;
    private BroadcastReceiver createdReceiver;

    private int buttonHighlightColour = Colour.WHITE.intValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updatedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                syncGraphWithSurvey();
            }
        };

        createdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleAutoRecentre();
            }
        };

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

        // Needs to be threaded so it is only run once we know height and width
        graphView.post(this::setViewLocation);
    }


    private void handleAutoRecentre() {
        if (graphView.getDisplayPreference(SketchPreferences.Toggle.AUTO_RECENTRE)) {
            graphView.centreViewOnActiveStation();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        registerReceivers();
        syncGraphWithSurvey();

        intialiseActivity();
        initialiseGraphView();
        initialiseTools();
        initialiseSymbolToolbar();

        setSketchButtonsStatus();
    }

    public void onPause() {
        super.onPause();
        unregisterReceivers();
    }


    private void registerReceivers() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(updatedReceiver,
                new IntentFilter(SexyTopoConstants.SURVEY_UPDATED_EVENT));
        broadcastManager.registerReceiver(createdReceiver,
                new IntentFilter(SexyTopoConstants.NEW_STATION_CREATED_EVENT));
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
            graphView.centreViewOnStation(requestedStation);
        } else {
            graphView.centreViewOnActiveStation();
        }
    }

    private void intialiseActivity() {
        buttonHighlightColour = ContextCompat.getColor(this, R.color.buttonHighlight);
    }

    private void initialiseGraphView() {
        graphView.initialisePaint();
        graphView.setProjectionType(getProjectionType());

        boolean isDarkModeActive = isDarkModeActive();
        graphView.setIsDarkModeActive(isDarkModeActive);
    }


    private void syncGraphWithSurvey() {
        Survey survey = getSurvey();
        graphView.setSurvey(survey);
        graphView.setSketch(getSketch(survey));
        graphView.setProjection(getProjection(survey));
        graphView.checkForChangedSurvey();
        float surveyLength = SurveyStats.calcTotalLength(survey);
        float surveyHeight = SurveyStats.calcHeightRange(survey);
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
        int itemId = item.getItemId();

        // this has to be a big hairy if-else chain instead of a switch statement
        // (itemId is no longer a constant in later Android versions)
        if (itemId == R.id.buttonSnapToLines) {
            SketchPreferences.Toggle.SNAP_TO_LINES.set(!item.isChecked());
            return true;
        } else if (itemId == R.id.buttonShowGrid) {
            SketchPreferences.Toggle.SHOW_GRID.set(!item.isChecked());
            graphView.invalidate();
            return true;
        } else if (itemId == R.id.buttonFadeNonActive) {
            SketchPreferences.Toggle.FADE_NON_ACTIVE.set(!item.isChecked());
            graphView.invalidate();
            return true;
        } else if (itemId == R.id.buttonShowSplays) {
            SketchPreferences.Toggle.SHOW_SPLAYS.set(!item.isChecked());
            graphView.invalidate();
            return true;
        } else if (itemId == R.id.buttonShowSketch) {
            SketchPreferences.Toggle.SHOW_SKETCH.set(!item.isChecked());
            setSketchButtonsStatus();
            graphView.invalidate();
            return true;
        } else if (itemId == R.id.buttonShowStationLabels) {
            SketchPreferences.Toggle.SHOW_STATION_LABELS.set(!item.isChecked());
            graphView.invalidate();
            return true;
        } else if (itemId == R.id.buttonShowConnections) {
            SketchPreferences.Toggle.SHOW_CONNECTIONS.set(!item.isChecked());
            graphView.invalidate();
            return true;
        } else if (itemId == R.id.buttonAutoRecentre) {
            SketchPreferences.Toggle.AUTO_RECENTRE.set(!item.isChecked());
            graphView.invalidate();
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
            graphView.setSketchTool(SketchTool.MOVE);
        }
    }


    public boolean handleAction(int itemId) {

        GraphView graphView = findViewById(R.id.graphView);
        graphView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        for (BrushColour brushColour: BrushColour.values()) {
            if (brushColour.getId() == itemId) {
                selectBrushColour(brushColour);
                if (!graphView.getSketchTool().usesColour()) {
                    selectSketchTool(SketchTool.DRAW);
                }
                return true;
            }
        }

        SketchTool alreadySelectedTool = graphView.getSketchTool();
        for (SketchTool sketchTool: SketchTool.values()) {
            if (sketchTool.getId() == itemId) {
                if (alreadySelectedTool == SketchTool.SYMBOL && sketchTool == SketchTool.SYMBOL) {
                    toggleSymbolToolbar();
                } else {
                    selectSketchTool(sketchTool);
                }
                return true;
            }
        }

        for (Symbol symbol : Symbol.values()) {
            if (itemId == symbol.getBitmapId()) {
                selectSketchTool(SketchTool.SYMBOL);
                selectSymbol(symbol);
                return true;
            }
        }

        if (itemId == R.id.buttonSymbolToolbarClose) {
            setSymbolToolbarOpen(false);
            return true;
        } else if (itemId == R.id.buttonZoomIn) {
            graphView.adjustZoomBy(ZOOM_INCREMENT);
            graphView.invalidate();
            return true;
        } else if (itemId == R.id.buttonZoomOut) {
            graphView.adjustZoomBy(ZOOM_DECREMENT);
            graphView.invalidate();
            return true;
        } else if (itemId == R.id.buttonUndo) {
            graphView.undo();
            return true;
        } else if (itemId == R.id.buttonRedo) {
            graphView.redo();
            return true;
        } else if (itemId == R.id.buttonMenu) {
            openDisplayMenu();
            return true;
        } else if (itemId == R.id.buttonCentreView) {
            graphView.centreViewOnActiveStation();
            graphView.invalidate();
            return true;
        } else if (itemId == R.id.buttonDeleteLastLeg) {
            getSurvey().undoAddLeg();
            syncGraphWithSurvey();
            return true;
        }

        return false;
    }


    private void openDisplayMenu() {

        View view = findViewById(R.id.buttonMenu);
        PopupMenu popup = new PopupMenu(this, view);
        Menu menu = popup.getMenu();
        popup.getMenuInflater().inflate(R.menu.drawing, menu);
        popup.setOnMenuItemClickListener(this);

        for (SketchPreferences.Toggle toggle : SketchPreferences.Toggle.values()) {
            int controlId = toggle.getControlId();
            MenuItem menuItem = menu.findItem(controlId);
            menuItem.setChecked(toggle.isOn());
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
                (inflater.inflate(R.layout.tool_button, buttonPanel, false));
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


    private void selectSymbol(Symbol symbol) {
        SketchPreferences.setSelectedSymbol(symbol);

        ImageButton selectedSymbolButton = findViewById(symbol.getBitmapId());
        selectedSymbolButton.getBackground().setColorFilter(buttonHighlightColour, PorterDuff.Mode.SRC_ATOP);

        ImageButton symbolButton = findViewById(R.id.buttonSymbol);
        symbolButton.setScaleType(ImageView.ScaleType.FIT_CENTER);

        Symbol.setResources(this.getResources());
        Bitmap buttonBitmap = symbol.getButtonBitmap();
        symbolButton.setImageBitmap(buttonBitmap);

        graphView.setCurrentSymbol(symbol);

    }


    private void initialiseTools() {
        SketchTool selected = SketchPreferences.getSelectedSketchTool();
        selectSketchTool(selected);

        BrushColour brushColour = SketchPreferences.getSelectedBrushColour();
        selectBrushColour(brushColour);

        Symbol.setResources(getResources());
        Symbol selectedSymbol = SketchPreferences.getSelectedSymbol();
        selectSymbol(selectedSymbol);
    }


    private void selectSketchTool(SketchTool toSelect) {

        SketchPreferences.setSelectedSketchTool(toSelect);
        graphView.setSketchTool(toSelect);

        for (SketchTool sketchTool : SketchTool.values()) {

            View button = findViewById(sketchTool.getId());
            if (button == null) {
                continue; // some tools are not linked to a toolbar button
            }

            if (sketchTool == toSelect) {
                button.getBackground().setColorFilter(buttonHighlightColour, PorterDuff.Mode.SRC_ATOP);
            } else {
                button.getBackground().clearColorFilter();
            }
        }
    }

    private void selectBrushColour(BrushColour toSelect) {

        SketchPreferences.setSelectedBrushColour(toSelect);
        graphView.setBrushColour(toSelect);

        for (BrushColour brushColour : BrushColour.values()) {

            View button = findViewById(brushColour.getId());
            if (brushColour == toSelect) {
                button.getBackground().setColorFilter(buttonHighlightColour, PorterDuff.Mode.SRC_ATOP);
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

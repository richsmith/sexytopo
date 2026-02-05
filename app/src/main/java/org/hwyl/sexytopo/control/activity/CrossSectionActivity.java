package org.hwyl.sexytopo.control.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import androidx.core.content.ContextCompat;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.graph.CrossSectionView;
import org.hwyl.sexytopo.model.sketch.BrushColour;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.sketch.SketchTool;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

public class CrossSectionActivity extends SexyTopoActivity implements View.OnClickListener {

    public static final String EXTRA_STATION_NAME = "station_name";
    
    private CrossSectionView crossSectionView;
    private String stationName;
    private int buttonHighlightColour;

    public static Intent getIntent(Context context, String stationName) {
        Intent intent = new Intent(context, CrossSectionActivity.class);
        intent.putExtra(EXTRA_STATION_NAME, stationName);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cross_section);
        setupMaterialToolbar();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        applyEdgeToEdgeInsets(R.id.crossSectionRootLayout, true, true);

        stationName = getIntent().getStringExtra(EXTRA_STATION_NAME);
        buttonHighlightColour = ContextCompat.getColor(this, R.color.buttonHighlight);
        
        crossSectionView = findViewById(R.id.crossSectionView);
        crossSectionView.setActivity(this);
        
        // Set up toolbar buttons
        int[] buttonIds = {
            R.id.buttonMove,
            R.id.buttonDraw,
            R.id.buttonErase,
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
        
        for (int id : buttonIds) {
            View button = findViewById(id);
            if (button != null) {
                button.setOnClickListener(this);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        Survey survey = getSurvey();
        Station station = survey.getStationByName(stationName);
        
        if (station != null) {
            CrossSection crossSection = findCrossSectionForStation(survey, station);
            if (crossSection != null) {
                crossSectionView.setCrossSection(crossSection);
                setTitle(getString(R.string.cross_section_title, stationName));
            }
        }
        
        selectSketchTool(SketchTool.DRAW);
        selectBrushColour(BrushColour.BLACK);
        crossSectionView.invalidate();
    }
    
    private CrossSection findCrossSectionForStation(Survey survey, Station station) {
        var planDetail = survey.getPlanSketch().getCrossSectionDetail(station);
        if (planDetail != null) {
            return planDetail.getCrossSection();
        }
        
        var elevDetail = survey.getElevationSketch().getCrossSectionDetail(station);
        if (elevDetail != null) {
            return elevDetail.getCrossSection();
        }
        
        return null;
    }
    
    @Override
    public void onClick(View view) {
        int id = view.getId();
        
        if (id == R.id.buttonMove) {
            selectSketchTool(SketchTool.MOVE);
        } else if (id == R.id.buttonDraw) {
            selectSketchTool(SketchTool.DRAW);
        } else if (id == R.id.buttonErase) {
            selectSketchTool(SketchTool.ERASE);
        } else if (id == R.id.buttonUndo) {
            crossSectionView.undo();
        } else if (id == R.id.buttonRedo) {
            crossSectionView.redo();
        } else {
            // Check colour buttons
            for (BrushColour colour : BrushColour.values()) {
                if (colour.getId() == id) {
                    selectBrushColour(colour);
                    if (crossSectionView.getSketchTool() != SketchTool.DRAW) {
                        selectSketchTool(SketchTool.DRAW);
                    }
                    return;
                }
            }
        }
    }
    
    private void selectSketchTool(SketchTool tool) {
        crossSectionView.setSketchTool(tool);
        
        // Update button highlights
        for (SketchTool t : new SketchTool[]{SketchTool.MOVE, SketchTool.DRAW, SketchTool.ERASE}) {
            View button = findViewById(t.getId());
            if (button != null) {
                if (t == tool) {
                    button.getBackground().setColorFilter(buttonHighlightColour, PorterDuff.Mode.SRC_ATOP);
                } else {
                    button.getBackground().clearColorFilter();
                }
            }
        }
    }
    
    private void selectBrushColour(BrushColour colour) {
        crossSectionView.setBrushColour(colour);
        
        for (BrushColour c : BrushColour.values()) {
            View button = findViewById(c.getId());
            if (button != null) {
                if (c == colour) {
                    button.getBackground().setColorFilter(buttonHighlightColour, PorterDuff.Mode.SRC_ATOP);
                } else {
                    button.getBackground().clearColorFilter();
                }
            }
        }
    }
}

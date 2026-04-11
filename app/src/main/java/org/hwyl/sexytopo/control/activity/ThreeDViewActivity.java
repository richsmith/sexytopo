package org.hwyl.sexytopo.control.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.TypedValue;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopoConstants;
import org.hwyl.sexytopo.control.threed.SurveyRenderer;
import org.hwyl.sexytopo.control.threed.SurveyView3D;
import org.hwyl.sexytopo.control.util.Space3DTransformer;
import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.survey.Survey;


public class ThreeDViewActivity extends SexyTopoActivity {

    private SurveyView3D surveyView3D;
    private SurveyRenderer renderer;
    private final Space3DTransformer transformer = new Space3DTransformer();

    private final BroadcastReceiver surveyUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateSurveyData();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_3d_view);
        setupMaterialToolbar();
        applyEdgeToEdgeInsets(R.id.rootLayout, true, false);

        surveyView3D = findViewById(R.id.survey_view_3d);
        renderer = surveyView3D.getSurveyRenderer();

        applyThemeBackgroundColour();
        updateSurveyData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        surveyView3D.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
            surveyUpdateReceiver,
            new IntentFilter(SexyTopoConstants.SURVEY_UPDATED_EVENT));
        updateSurveyData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        surveyView3D.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(surveyUpdateReceiver);
    }

    private void applyThemeBackgroundColour() {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true);
        int colour = typedValue.data;
        float r = ((colour >> 16) & 0xFF) / 255f;
        float g = ((colour >> 8) & 0xFF) / 255f;
        float b = (colour & 0xFF) / 255f;
        renderer.setBackgroundColour(r, g, b);
    }

    private void updateSurveyData() {
        Survey survey = getSurvey();
        if (survey == null) {
            return;
        }
        Space<Coord3D> space = transformer.transformTo3D(survey);
        renderer.setSurveyData(space);
        surveyView3D.requestRender();
    }
}

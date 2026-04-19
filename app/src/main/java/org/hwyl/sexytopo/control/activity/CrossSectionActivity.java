package org.hwyl.sexytopo.control.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.graph.CrossSectionView;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.sketch.CrossSectionDetail;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

public class CrossSectionActivity extends GraphActivity {

    public static final String EXTRA_STATION_NAME = "crossSectionStationName";

    private Station station;
    private CrossSectionDetail originalDetail;
    private Sketch workingSketch;

    @Override
    public void setContentView(int layoutResID) {
        // Intercept the base-class content view so the cross-section editor uses a layout
        // wired to a CrossSectionView instead of the full GraphView.
        if (layoutResID == R.layout.activity_graph) {
            super.setContentView(R.layout.activity_cross_section);
        } else {
            super.setContentView(layoutResID);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        String stationName = (extras == null) ? null : extras.getString(EXTRA_STATION_NAME);

        Survey survey = getSurvey();
        if (stationName == null || survey == null) {
            finish();
            return;
        }

        station = survey.getStationByName(stationName);
        if (station == null) {
            finish();
            return;
        }

        originalDetail = survey.getPlanSketch().getCrossSectionDetail(station);
        if (originalDetail == null) {
            finish();
            return;
        }

        workingSketch = buildWorkingSketch(originalDetail.getSketch());

        disableUnsupportedTools();

        CrossSectionView graphView = findViewById(R.id.graphView);
        Space<Coord2D> projection = originalDetail.getCrossSection().getProjection();
        graphView.setProjection(projection);
    }

    private static Sketch buildWorkingSketch(Sketch source) {
        Sketch working = new Sketch();
        working.setPathDetails(new ArrayList<>(source.getPathDetails()));
        return working;
    }

    private void disableUnsupportedTools() {
        int[] toolsToHide = new int[] {R.id.buttonSelect};
        for (int id : toolsToHide) {
            View view = findViewById(id);
            if (view != null) {
                view.setEnabled(false);
            }
        }
    }

    @Override
    public Sketch getSketch(Survey survey) {
        return workingSketch != null ? workingSketch : new Sketch();
    }

    @Override
    public Space<Coord2D> getProjection(Survey survey) {
        if (originalDetail != null) {
            return originalDetail.getCrossSection().getProjection();
        }
        return super.getProjection(survey);
    }

    @Override
    public Projection2D getProjectionType() {
        return Projection2D.CROSS_SECTION;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cross_section, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // The cross-section menu is a minimal Done/Cancel menu; skip the base class's
        // preparation which assumes the full survey menu (device submenu, dev tools, etc.).
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_cross_section_done) {
            commitAndFinish();
            return true;
        } else if (itemId == R.id.action_cross_section_cancel) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void commitAndFinish() {
        if (originalDetail == null || workingSketch == null) {
            finish();
            return;
        }

        Sketch persistedSubSketch = new Sketch();
        persistedSubSketch.setPathDetails(
                new ArrayList<PathDetail>(workingSketch.getPathDetails()));

        CrossSection crossSection = originalDetail.getCrossSection();
        Coord2D position = originalDetail.getPosition();
        CrossSectionDetail updated =
                new CrossSectionDetail(crossSection, position, persistedSubSketch);

        getSurvey().getPlanSketch().replaceCrossSectionDetail(originalDetail, updated);
        finish();
    }

    @Override
    public void onNewCrossSection(Station station) {
        // No-op: cross-section editor does not allow nesting cross-sections.
    }
}

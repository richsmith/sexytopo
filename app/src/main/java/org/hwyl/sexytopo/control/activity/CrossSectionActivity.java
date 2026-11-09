package org.hwyl.sexytopo.control.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.sketch.CrossSectionLabels;
import org.hwyl.sexytopo.control.sketch.CrossSectionView;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.geometry.Projection2D;
import org.hwyl.sexytopo.model.geometry.Space;
import org.hwyl.sexytopo.model.sketch.CrossSectionDetail;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

public class CrossSectionActivity extends SketchActivity {

    public static final String EXTRA_STATION_NAME = "crossSectionStationName";

    /**
     * Abbreviation of the projection whose sketch holds the cross-section being edited. Optional:
     * if it is missing the plan sketch is assumed.
     */
    public static final String EXTRA_PROJECTION = "crossSectionProjection";

    private Station station;
    private Sketch parentSketch;
    private CrossSectionDetail originalDetail;
    private Sketch workingSketch;

    @Override
    public void setContentView(int layoutResID) {
        // Intercept the base-class content view so the cross-section editor uses a layout
        // wired to a CrossSectionView instead of the full SketchView.
        if (layoutResID == R.layout.activity_sketch) {
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
        String projectionAbbreviation =
                (extras == null) ? null : extras.getString(EXTRA_PROJECTION);

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

        parentSketch = getParentSketch(survey, projectionAbbreviation);
        originalDetail = parentSketch.getCrossSectionDetail(station);
        if (originalDetail == null) {
            finish();
            return;
        }

        workingSketch = buildWorkingSketch(originalDetail.getSketch());

        disableUnsupportedTools();

        CrossSectionView sketchView = findViewById(R.id.sketchView);
        Space<Coord2D> projection = originalDetail.getCrossSection().getProjection();
        sketchView.setProjection(projection);
        sketchView.setCrossSection(originalDetail.getCrossSection());

        setTitle(
                CrossSectionLabels.getTitleResource(
                        originalDetail.getCrossSection().getOrientation()));
    }

    /** The sketch the cross-section lives in: elevation if asked for, otherwise the plan. */
    private static Sketch getParentSketch(Survey survey, String projectionAbbreviation) {
        Projection2D projection = Projection2D.fromAbbreviation(projectionAbbreviation);
        if (projection == Projection2D.EXTENDED_ELEVATION) {
            return survey.getElevationSketch();
        }
        return survey.getPlanSketch();
    }

    private static Sketch buildWorkingSketch(Sketch source) {
        Sketch working = new Sketch(source);
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

        // Mutate the live detail in place rather than swapping in a new instance: this keeps the
        // detail's identity stable so the plan's undo/redo stacks don't end up referencing a
        // stale copy. The editor owns its own undo, so committing is not undoable in the parent.
        originalDetail.setSketch(persistedSubSketch);
        parentSketch.setSaved(false);
        finish();
    }

    @Override
    public void onNewCrossSection(Station station) {
        // No-op: cross-section editor does not allow nesting cross-sections.
    }

    @Override
    public void onNewHorizontalCrossSection(Station station) {
        // No-op: cross-section editor does not allow nesting cross-sections.
    }
}

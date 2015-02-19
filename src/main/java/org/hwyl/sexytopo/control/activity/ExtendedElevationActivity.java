package org.hwyl.sexytopo.control.activity;


import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;

public class ExtendedElevationActivity extends GraphActivity {

    @Override
    protected Sketch getSketch(Survey survey) {
        return survey.getElevationSketch();
    }

    @Override
    protected Space<Coord2D> getProjection(Survey survey) {
        Space<Coord2D> projection = Projection2D.EXTENDED_ELEVATION.project(survey);
        return projection;
    }

}

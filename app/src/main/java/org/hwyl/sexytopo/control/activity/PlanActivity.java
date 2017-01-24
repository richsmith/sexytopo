package org.hwyl.sexytopo.control.activity;

import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;

public class PlanActivity extends GraphActivity {

    @Override
    public Sketch getSketch(Survey survey) {
        return survey.getPlanSketch();
    }

    @Override
    public Space<Coord2D> getProjection(Survey survey) {
        Space<Coord2D> projection = Projection2D.PLAN.project(survey);
        return projection;
    }

}

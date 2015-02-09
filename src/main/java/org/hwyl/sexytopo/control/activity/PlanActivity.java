package org.hwyl.sexytopo.control.activity;

import org.hwyl.sexytopo.control.util.Space3DTransformer;
import org.hwyl.sexytopo.model.Survey;
import org.hwyl.sexytopo.model.graph.Coord2D;
import org.hwyl.sexytopo.model.graph.Coord3D;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.graph.Space;
import org.hwyl.sexytopo.model.sketch.Sketch;

public class PlanActivity extends GraphActivity {

    @Override
    protected Sketch getSketch(Survey survey) {
        return survey.getPlanSketch();
    }

    @Override
    protected Space<Coord2D> getProjection(Survey survey) {
        Space<Coord3D> space3D = Space3DTransformer.transformTo3D(survey);
        Space<Coord2D> projection = Projection2D.PLAN.project(space3D);
        return projection;
    }

}

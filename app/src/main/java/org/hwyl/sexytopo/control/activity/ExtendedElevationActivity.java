package org.hwyl.sexytopo.control.activity;


import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;

public class ExtendedElevationActivity extends GraphActivity {

    @Override
    public Sketch getSketch(Survey survey) {
        return survey.getElevationSketch();
    }


    @Override
    public Projection2D getProjectionType() {
        return Projection2D.EXTENDED_ELEVATION;
    }

}

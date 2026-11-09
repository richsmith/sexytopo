package org.hwyl.sexytopo.control.activity;

import org.hwyl.sexytopo.model.geometry.Projection2D;

public class PlanActivity extends SketchActivity {

    @Override
    public Projection2D getProjectionType() {
        return Projection2D.PLAN;
    }
}

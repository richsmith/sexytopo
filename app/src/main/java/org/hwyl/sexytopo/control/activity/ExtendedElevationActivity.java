package org.hwyl.sexytopo.control.activity;

import org.hwyl.sexytopo.model.graph.Projection2D;

public class ExtendedElevationActivity extends GraphActivity {

    @Override
    public Projection2D getProjectionType() {
        return Projection2D.EXTENDED_ELEVATION;
    }
}

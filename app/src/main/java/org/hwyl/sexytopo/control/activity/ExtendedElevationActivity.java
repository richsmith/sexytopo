package org.hwyl.sexytopo.control.activity;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.geometry.Projection2D;

public class ExtendedElevationActivity extends SketchActivity {

    @Override
    public Projection2D getProjectionType() {
        return Projection2D.EXTENDED_ELEVATION;
    }

    @Override
    protected int getActiveViewMenuItemId() {
        return R.id.action_elevation;
    }
}

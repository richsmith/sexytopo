package org.hwyl.sexytopo.control.activity;


import android.view.View;
import android.widget.PopupWindow;

import org.hwyl.sexytopo.control.graph.ExtendedElevationContextMenu;
import org.hwyl.sexytopo.model.graph.Projection2D;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Station;
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

    @Override
    public PopupWindow getContextMenu(Station station, View.OnClickListener listener) {
        return new ExtendedElevationContextMenu()
                .getFakeStationContextMenu(this, station, listener);
    }

}

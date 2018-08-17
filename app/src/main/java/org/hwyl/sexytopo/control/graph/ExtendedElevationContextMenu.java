package org.hwyl.sexytopo.control.graph;

import android.view.View;
import android.widget.Button;

import org.hwyl.sexytopo.R;


public class ExtendedElevationContextMenu extends StationContextMenu {

    @Override
    protected void postCreationHook(View view) {
        Button switchbackButton = (Button)(view.findViewById(R.id.graph_station_toggle_left_right));
        // switchbackButton.setVisibility(View.VISIBLE); FIXME when saving works for toggling LR
    }
}

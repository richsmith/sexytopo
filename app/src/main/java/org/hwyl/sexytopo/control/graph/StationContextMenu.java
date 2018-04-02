package org.hwyl.sexytopo.control.graph;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.survey.Station;

/**
 * Context menu to select specific operations to perform on individual stations when in sketch mode
 */
public class StationContextMenu {


    public PopupWindow getFakeStationContextMenu(Context context, Station station,
            final View.OnClickListener listener) {
        final PopupWindow fakeMenu = new PopupWindow(context);

        LayoutInflater inflater = (LayoutInflater)
                (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        View view = inflater.inflate(R.layout.graph_station_menu, null);

        postCreationHook(view);

        Button title = (Button)(view.findViewById(R.id.graph_station_title));
        title.setText(station.getName());

        fakeMenu.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        fakeMenu.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        fakeMenu.setContentView(view);

        int[] ids = new int[] {
                R.id.graph_station_toggle_left_right,
                R.id.graph_station_comment,
                R.id.graph_station_delete,
                R.id.graph_station_reverse,
                R.id.graph_station_new_cross_section,
                R.id.graph_station_start_new_survey,
                R.id.graph_station_unlink_survey
        };
        for (int id : ids) {
            ((Button)(view.findViewById(id))).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fakeMenu.dismiss();
                    listener.onClick(view);
                }

            });
        }


        fakeMenu.setFocusable(true);
        fakeMenu.setOutsideTouchable(true);

        return fakeMenu;
    }

    protected void postCreationHook(View view) {
        // Does nothing - provided to allow subclasses to override
    }
}

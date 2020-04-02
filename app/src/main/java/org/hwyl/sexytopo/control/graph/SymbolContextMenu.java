package org.hwyl.sexytopo.control.graph;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupWindow;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.sketch.Symbol;


public class SymbolContextMenu {
    public PopupWindow getFakeSymbolContextMenu(Context context,
                                                final View.OnClickListener listener) {
        final PopupWindow fakeMenu = new PopupWindow(context);

        GridLayout gridLayout = new GridLayout(context);
        GridView gridView = new GridView(context);
        gridView.setNumColumns(1);
        gridLayout.setColumnCount(1);
        for (Symbol symbol : Symbol.values()) {
            ImageView imageView = new ImageView(context);
            imageView.setImageBitmap(symbol.getBitmap());
            gridLayout.addView(imageView);
            gridView.addView(imageView);
        }

        LayoutInflater inflater = (LayoutInflater)
                (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        View view = inflater.inflate(R.layout.graph_station_menu, null);

        postCreationHook(view);

        Button title = view.findViewById(R.id.graph_station_title);
        title.setText("HELLOO");

        fakeMenu.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        fakeMenu.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        fakeMenu.setContentView(gridView);

        int[] ids = new int[] {
                R.id.graph_station_select,
                R.id.graph_station_toggle_left_right,
                R.id.graph_station_comment,
                R.id.graph_station_delete,
                R.id.graph_station_reverse,
                R.id.graph_station_new_cross_section,
                R.id.graph_station_jump_to_table,
                R.id.graph_station_start_new_survey,
                R.id.graph_station_unlink_survey
        };
        for (int id : ids) {
            view.findViewById(id).setOnClickListener(new View.OnClickListener() {
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

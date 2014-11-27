package org.hwyl.sexytopo.control.util;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import java.util.List;

/**
 * Created by rls on 22/09/14.
 */
public class ColourPickerMenu extends PopupMenu {

    public ColourPickerMenu(Context context, View anchor, List<Color> colours, OnClickListener listener) {
        super(context, anchor);

        for (Color colour : colours) {
            //ImageButton button = new ImageButton();
        }
    }
}

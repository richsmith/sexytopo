package org.hwyl.sexytopo.control.sketch;

import android.view.Menu;
import android.view.SubMenu;
import java.util.HashMap;
import java.util.Map;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.sketch.BrushColour;
import org.hwyl.sexytopo.model.sketch.Colour;

/**
 * The Colour submenu shared by the sketch detail context menus: the brush colours, with the
 * detail's current colour ticked.
 */
final class ColourSubMenu {

    private ColourSubMenu() {}

    /**
     * Fills in the menu's Colour item, returning the colour each of its items stands for, keyed by
     * item ID, for the click listener.
     */
    static Map<Integer, Colour> populate(Menu menu, Colour currentColour) {
        Map<Integer, Colour> itemIdsToColours = new HashMap<>();
        SubMenu colourMenu = menu.findItem(R.id.menu_colour).getSubMenu();
        for (BrushColour brushColour : BrushColour.values()) {
            int itemId = brushColour.getId();
            colourMenu
                    .add(R.id.menu_colour, itemId, Menu.NONE, brushColour.getStringId())
                    .setChecked(brushColour.getColour() == currentColour);
            itemIdsToColours.put(itemId, brushColour.getColour());
        }
        colourMenu.setGroupCheckable(R.id.menu_colour, true, true);
        return itemIdsToColours;
    }
}

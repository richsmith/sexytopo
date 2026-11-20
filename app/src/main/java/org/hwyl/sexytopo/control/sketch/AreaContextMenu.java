package org.hwyl.sexytopo.control.sketch;

import android.view.Menu;
import android.view.SubMenu;
import android.view.View;
import android.widget.PopupMenu;
import java.util.HashMap;
import java.util.Map;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.sketch.AreaDetail;
import org.hwyl.sexytopo.model.sketch.AreaType;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.Sketch;

/**
 * The menu shown on long-pressing an area, offering to change its type or colour or delete it. Each
 * is a single undoable step.
 */
final class AreaContextMenu {

    private AreaContextMenu() {}

    /**
     * Show the menu for an area at a point on the anchor view. onChanged is run after the area has
     * been retyped or deleted, so the view can redraw.
     */
    static void show(
            View anchorView,
            int x,
            int y,
            Sketch sketch,
            AreaDetail areaDetail,
            Runnable onChanged) {
        PopupMenu popup =
                ContextMenuManager.createPopupMenuAt(anchorView.getContext(), anchorView, x, y);
        popup.inflate(R.menu.context_area);
        ContextMenuManager.enableGroupDividers(popup);
        Menu menu = popup.getMenu();

        AreaType currentType = areaDetail.getAreaType();
        menu.findItem(R.id.area_title).setTitle(currentType.getStringId());

        Map<Integer, AreaType> itemIdsToAreaTypes = new HashMap<>();
        SubMenu areaTypeMenu = menu.findItem(R.id.menu_area_type).getSubMenu();
        for (AreaType areaType : AreaType.values()) {
            int itemId = areaType.getButtonViewId();
            areaTypeMenu
                    .add(R.id.menu_area_type, itemId, Menu.NONE, areaType.getStringId())
                    .setChecked(areaType == currentType);
            itemIdsToAreaTypes.put(itemId, areaType);
        }
        areaTypeMenu.setGroupCheckable(R.id.menu_area_type, true, true);

        Map<Integer, Colour> itemIdsToColours =
                ColourSubMenu.populate(menu, areaDetail.getColour());

        popup.setOnMenuItemClickListener(
                item -> {
                    if (item.getItemId() == R.id.action_delete_area) {
                        sketch.deleteDetail(areaDetail);
                        onChanged.run();
                        return true;
                    }
                    AreaType areaType = itemIdsToAreaTypes.get(item.getItemId());
                    if (areaType != null && areaType != currentType) {
                        sketch.retypeAreaDetail(areaDetail, areaType);
                        onChanged.run();
                        return true;
                    }
                    Colour colour = itemIdsToColours.get(item.getItemId());
                    if (colour != null && colour != areaDetail.getColour()) {
                        sketch.recolourAreaDetail(areaDetail, colour);
                        onChanged.run();
                        return true;
                    }
                    return false;
                });
        popup.show();
    }
}

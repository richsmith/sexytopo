package org.hwyl.sexytopo.control.sketch;

import android.view.Menu;
import android.view.SubMenu;
import android.view.View;
import android.widget.PopupMenu;
import java.util.HashMap;
import java.util.Map;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.LineType;
import org.hwyl.sexytopo.model.sketch.PathDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;

/** The context menu shown by long-pressing on a line. */
final class LineContextMenu {

    private LineContextMenu() {}

    /**
     * Show the menu for a line at a point on the anchor view. onChanged is run after the line has
     * been changed or deleted, so the view can redraw.
     */
    static void show(
            View anchorView,
            int x,
            int y,
            Sketch sketch,
            PathDetail pathDetail,
            Runnable onChanged) {
        PopupMenu popup =
                ContextMenuManager.createPopupMenuAt(anchorView.getContext(), anchorView, x, y);
        popup.inflate(R.menu.context_line);
        ContextMenuManager.enableGroupDividers(popup);
        Menu menu = popup.getMenu();

        LineType currentType = pathDetail.getLineType();
        menu.findItem(R.id.line_title).setTitle(currentType.getStringId());
        // Orientation means nothing for a plain sketch line or a rock edge
        menu.findItem(R.id.action_flip_line).setEnabled(currentType.isOriented());

        Map<Integer, LineType> itemIdsToLineTypes = new HashMap<>();
        SubMenu lineTypeMenu = menu.findItem(R.id.menu_line_type).getSubMenu();
        for (LineType lineType : LineType.values()) {
            int itemId = lineType.getButtonViewId();
            lineTypeMenu
                    .add(R.id.menu_line_type, itemId, Menu.NONE, lineType.getStringId())
                    .setChecked(lineType == currentType);
            itemIdsToLineTypes.put(itemId, lineType);
        }
        lineTypeMenu.setGroupCheckable(R.id.menu_line_type, true, true);

        Map<Integer, Colour> itemIdsToColours =
                ColourSubMenu.populate(menu, pathDetail.getColour());

        popup.setOnMenuItemClickListener(
                item -> {
                    if (item.getItemId() == R.id.action_delete_line) {
                        sketch.deleteDetail(pathDetail);
                        onChanged.run();
                        return true;
                    }
                    if (item.getItemId() == R.id.action_flip_line) {
                        sketch.flipPathDetail(pathDetail);
                        onChanged.run();
                        return true;
                    }
                    LineType lineType = itemIdsToLineTypes.get(item.getItemId());
                    if (lineType != null && lineType != currentType) {
                        sketch.retypePathDetail(pathDetail, lineType);
                        onChanged.run();
                        return true;
                    }
                    Colour colour = itemIdsToColours.get(item.getItemId());
                    if (colour != null && colour != pathDetail.getColour()) {
                        sketch.recolourPathDetail(pathDetail, colour);
                        onChanged.run();
                        return true;
                    }
                    return false;
                });
        popup.show();
    }
}

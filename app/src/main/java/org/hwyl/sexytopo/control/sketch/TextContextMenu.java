package org.hwyl.sexytopo.control.sketch;

import android.view.View;
import android.widget.PopupMenu;
import java.util.Map;
import java.util.function.Consumer;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.TextDetail;

/** The menu shown on long-pressing some text. */
final class TextContextMenu {

    private TextContextMenu() {}

    /**
     * Show the menu for some text at a point on the anchor view. onChanged is run after the text
     * has been edited or deleted, so the view can redraw. onMove or onResize is given the text if
     * the user chooses to move or resize it.
     */
    static void show(
            View anchorView,
            int x,
            int y,
            Sketch sketch,
            TextDetail textDetail,
            Runnable onChanged,
            Consumer<TextDetail> onMove,
            Consumer<TextDetail> onResize) {
        PopupMenu popup =
                ContextMenuManager.createPopupMenuAt(anchorView.getContext(), anchorView, x, y);
        popup.inflate(R.menu.context_text);
        ContextMenuManager.enableGroupDividers(popup);
        Map<Integer, Colour> itemIdsToColours =
                ColourSubMenu.populate(popup.getMenu(), textDetail.getColour());

        popup.setOnMenuItemClickListener(
                item -> {
                    if (item.getItemId() == R.id.action_move_text) {
                        onMove.accept(textDetail);
                        return true;
                    }
                    if (item.getItemId() == R.id.action_resize_text) {
                        onResize.accept(textDetail);
                        return true;
                    }
                    if (item.getItemId() == R.id.action_edit_text) {
                        TextEntryDialog.show(
                                anchorView.getContext(),
                                textDetail.getText(),
                                text -> {
                                    if (!text.equals(textDetail.getText())) {
                                        sketch.changeText(textDetail, text);
                                        onChanged.run();
                                    }
                                });
                        return true;
                    }
                    if (item.getItemId() == R.id.action_delete_text) {
                        sketch.deleteDetail(textDetail);
                        onChanged.run();
                        return true;
                    }
                    Colour colour = itemIdsToColours.get(item.getItemId());
                    if (colour != null && colour != textDetail.getColour()) {
                        sketch.recolourTextDetail(textDetail, colour);
                        onChanged.run();
                        return true;
                    }
                    return false;
                });
        popup.show();
    }
}

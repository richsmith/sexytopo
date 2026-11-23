package org.hwyl.sexytopo.control.sketch;

import android.view.Menu;
import android.view.SubMenu;
import android.view.View;
import android.widget.PopupMenu;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.Symbol;
import org.hwyl.sexytopo.model.sketch.SymbolDetail;

/**
 * The menu shown on long-pressing a symbol, offering to move or resize it, swap it for another
 * symbol, change its colour or delete it. Each is a single undoable step.
 */
final class SymbolContextMenu {

    private SymbolContextMenu() {}

    /**
     * Show the menu for a symbol at a point on the anchor view. onChanged is run after the symbol
     * has been changed or deleted, so the view can redraw. onMove or onResize is given the symbol
     * if the user chooses to move or resize it.
     */
    static void show(
            View anchorView,
            int x,
            int y,
            Sketch sketch,
            SymbolDetail symbolDetail,
            Runnable onChanged,
            Consumer<SymbolDetail> onMove,
            Consumer<SymbolDetail> onResize) {
        PopupMenu popup =
                ContextMenuManager.createPopupMenuAt(anchorView.getContext(), anchorView, x, y);
        popup.inflate(R.menu.context_symbol);
        ContextMenuManager.enableGroupDividers(popup);
        Menu menu = popup.getMenu();

        Symbol currentSymbol = symbolDetail.getSymbol();
        menu.findItem(R.id.symbol_title).setTitle(currentSymbol.getStringId());

        Map<Integer, Symbol> itemIdsToSymbols = new HashMap<>();
        SubMenu symbolMenu = menu.findItem(R.id.menu_symbol_type).getSubMenu();
        for (Symbol symbol : Symbol.values()) {
            if (symbol == Symbol.TEXT) {
                continue; // text is a different kind of detail, not a symbol to swap to
            }
            int itemId = symbol.getButtonViewId();
            symbolMenu
                    .add(R.id.menu_symbol_type, itemId, Menu.NONE, symbol.getStringId())
                    .setChecked(symbol == currentSymbol);
            itemIdsToSymbols.put(itemId, symbol);
        }
        symbolMenu.setGroupCheckable(R.id.menu_symbol_type, true, true);

        Map<Integer, Colour> itemIdsToColours =
                ColourSubMenu.populate(menu, symbolDetail.getColour());

        popup.setOnMenuItemClickListener(
                item -> {
                    if (item.getItemId() == R.id.action_move_symbol) {
                        onMove.accept(symbolDetail);
                        return true;
                    }
                    if (item.getItemId() == R.id.action_resize_symbol) {
                        onResize.accept(symbolDetail);
                        return true;
                    }
                    if (item.getItemId() == R.id.action_delete_symbol) {
                        sketch.deleteDetail(symbolDetail);
                        onChanged.run();
                        return true;
                    }
                    Symbol symbol = itemIdsToSymbols.get(item.getItemId());
                    if (symbol != null && symbol != currentSymbol) {
                        sketch.changeSymbol(symbolDetail, symbol);
                        onChanged.run();
                        return true;
                    }
                    Colour colour = itemIdsToColours.get(item.getItemId());
                    if (colour != null && colour != symbolDetail.getColour()) {
                        sketch.recolourSymbolDetail(symbolDetail, colour);
                        onChanged.run();
                        return true;
                    }
                    return false;
                });
        popup.show();
    }
}

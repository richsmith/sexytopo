package org.hwyl.sexytopo.control.graph;

import android.view.Menu;
import android.view.MenuItem;
import org.hwyl.sexytopo.R;

/**
 * Represents the different view contexts where station context menus can be displayed. Each context
 * configures menu visibility differently.
 */
public enum ViewContext {
    TABLE {
        @Override
        public void configureViewSpecificItems(Menu menu) {
            setItemVisible(menu, R.id.action_jump_to_table, false);
            setDirectionSubmenuVisible(menu, false);
            setCrossSectionVisible(menu, false);
        }
    },
    PLAN {
        @Override
        public void configureViewSpecificItems(Menu menu) {
            setItemVisible(menu, R.id.action_jump_to_plan, false);
            setDirectionSubmenuVisible(menu, false);
            setCrossSectionVisible(menu, true);
        }
    },
    ELEVATION {
        @Override
        public void configureViewSpecificItems(Menu menu) {
            setItemVisible(menu, R.id.action_jump_to_elevation, false);
            setDirectionSubmenuVisible(menu, false);
            setCrossSectionVisible(menu, false);
        }
    },
    EXTENDED_ELEVATION {
        @Override
        public void configureViewSpecificItems(Menu menu) {
            setItemVisible(menu, R.id.action_jump_to_elevation, false);
            setDirectionSubmenuVisible(menu, true);
        }
    },
    CROSS_SECTION {
        @Override
        public void configureViewSpecificItems(Menu menu) {
            // No station context menu is shown in the cross-section editor.
        }

        @Override
        public boolean hasStationContextMenu() {
            return false;
        }
    },
    THREE_D {
        @Override
        public void configureViewSpecificItems(Menu menu) {
            setDirectionSubmenuVisible(menu, false);
            setCrossSectionVisible(menu, false);
        }

        @Override
        public boolean hasStationContextMenu() {
            return false;
        }
    };

    public abstract void configureViewSpecificItems(Menu menu);

    /** Whether a station long-press in this view context should open a context menu. */
    public boolean hasStationContextMenu() {
        return true;
    }

    protected void setItemVisible(Menu menu, int itemId, boolean visible) {
        MenuItem item = menu.findItem(itemId);
        if (item != null) {
            item.setVisible(visible);
        }
    }

    protected void setDirectionSubmenuVisible(Menu menu, boolean visible) {
        MenuItem elevationMenu = menu.findItem(R.id.menu_elevation);
        if (elevationMenu != null) {
            elevationMenu.setVisible(visible);
        }
    }

    protected void setCrossSectionVisible(Menu menu, boolean visible) {
        MenuItem crossSectionMenu = menu.findItem(R.id.menu_xsection);
        if (crossSectionMenu != null) {
            crossSectionMenu.setVisible(visible);
        }
    }
}

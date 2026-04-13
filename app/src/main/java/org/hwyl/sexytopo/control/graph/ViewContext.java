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
    THREE_D {
        @Override
        public void configureViewSpecificItems(Menu menu) {
            setDirectionSubmenuVisible(menu, false);
            setCrossSectionVisible(menu, false);
        }
    };

    public abstract void configureViewSpecificItems(Menu menu);

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
        MenuItem crossSectionItem = menu.findItem(R.id.action_new_cross_section);
        if (crossSectionItem != null) {
            crossSectionItem.setVisible(visible);
        }
    }
}

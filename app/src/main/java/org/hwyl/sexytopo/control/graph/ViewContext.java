package org.hwyl.sexytopo.control.graph;

import android.view.Menu;
import android.view.MenuItem;

import org.hwyl.sexytopo.R;

/**
 * Represents the different view contexts where station context menus can be displayed.
 * Each context configures menu visibility differently.
 */
public enum ViewContext {
    TABLE {
        @Override
        public void configureViewSpecificItems(Menu menu) {
            menu.findItem(R.id.action_jump_to_table).setVisible(false);
            setDirectionSubmenuVisible(menu, false);
            setCrossSectionVisible(menu, false);
        }
    },
    PLAN {
        @Override
        public void configureViewSpecificItems(Menu menu) {
            menu.findItem(R.id.action_jump_to_plan).setVisible(false);
            setDirectionSubmenuVisible(menu, false);
            setCrossSectionVisible(menu, true);
        }
    },
    ELEVATION {
        @Override
        public void configureViewSpecificItems(Menu menu) {
            menu.findItem(R.id.action_jump_to_elevation).setVisible(false);
            setDirectionSubmenuVisible(menu, false);
            setCrossSectionVisible(menu, false);
        }
    },
    EXTENDED_ELEVATION {
        @Override
        public void configureViewSpecificItems(Menu menu) {
            menu.findItem(R.id.action_jump_to_elevation).setVisible(false);
            setDirectionSubmenuVisible(menu, true);
        }
    };

    public abstract void configureViewSpecificItems(Menu menu);

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

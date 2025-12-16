package org.hwyl.sexytopo.control.graph;

import android.view.Menu;

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
            menu.findItem(R.id.action_toggle_left_right).setVisible(false);
        }
    },
    PLAN {
        @Override
        public void configureViewSpecificItems(Menu menu) {
            menu.findItem(R.id.action_jump_to_plan).setVisible(false);
            menu.findItem(R.id.action_toggle_left_right).setVisible(true);
        }
    },
    ELEVATION {
        @Override
        public void configureViewSpecificItems(Menu menu) {
            menu.findItem(R.id.action_jump_to_elevation).setVisible(false);
            menu.findItem(R.id.action_toggle_left_right).setVisible(false);
        }
    },
    EXTENDED_ELEVATION {
        @Override
        public void configureViewSpecificItems(Menu menu) {
            menu.findItem(R.id.action_jump_to_elevation).setVisible(false);
            menu.findItem(R.id.action_toggle_left_right).setVisible(false);
        }
    };

    public abstract void configureViewSpecificItems(Menu menu);
}

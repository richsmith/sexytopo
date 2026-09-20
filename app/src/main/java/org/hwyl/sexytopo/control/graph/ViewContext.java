package org.hwyl.sexytopo.control.graph;

import android.view.Menu;
import android.view.MenuItem;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.survey.Survey;

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

        @Override
        public Sketch getSketch(Survey survey) {
            return survey.getPlanSketch();
        }

        @Override
        public boolean canRotateCrossSections() {
            return true;
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
            setCrossSectionVisible(menu, true);
        }

        @Override
        public Sketch getSketch(Survey survey) {
            return survey.getElevationSketch();
        }

        @Override
        public boolean canCreateHorizontalCrossSection() {
            return true;
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

    /**
     * The sketch this view context shows for the given survey, or null if it has no sketch of its
     * own (e.g. the table).
     */
    public Sketch getSketch(Survey survey) {
        return null;
    }

    /**
     * Whether the user can choose the direction a cross-section faces in this view context. In the
     * plan a section is a vertical plane that can face any way around its station, so the user sets
     * it. Elsewhere the direction is worked out when the section is created and stays fixed.
     */
    public boolean canRotateCrossSections() {
        return false;
    }

    /**
     * Whether a horizontal cross-section can be created in this view context. On the plan a
     * horizontal slice would only repeat what the plan already shows, so it is for the elevation.
     */
    public boolean canCreateHorizontalCrossSection() {
        return false;
    }

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
        setItemVisible(menu, R.id.action_xsection_set_direction, canRotateCrossSections());
        setItemVisible(
                menu, R.id.action_xsection_create_horizontal, canCreateHorizontalCrossSection());
        if (canCreateHorizontalCrossSection()) {
            // With two kinds to choose from, plain "New Cross-Section" needs to say which it is
            MenuItem createItem = menu.findItem(R.id.action_xsection_create);
            if (createItem != null) {
                createItem.setTitle(R.string.menu_xsection_create_vertical);
            }
        }
    }
}

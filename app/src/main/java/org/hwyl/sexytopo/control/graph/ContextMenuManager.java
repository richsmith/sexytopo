package org.hwyl.sexytopo.control.graph;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified manager for station context menus across all views (table, plan, elevation).
 * Handles menu creation, visibility logic, and provides a consistent interface for actions.
 */
public class ContextMenuManager {

    /**
     * Simple functional interface for station actions (replacement for java.util.function.Consumer
     * which requires API 24, but our minSdk is 23).
     */
    private interface StationAction {
        void execute(Station station);
    }

    private final Context context;
    private final ViewContext viewContext;
    private final org.hwyl.sexytopo.control.activity.SurveyEditorActivity activity;
    private final Map<Integer, StationAction> menuActions;

    // Store the current leg context for actions that need it
    private Leg currentLeg;

    public ContextMenuManager(Context context, ViewContext viewContext, org.hwyl.sexytopo.control.activity.SurveyEditorActivity activity) {
        this.context = context;
        this.viewContext = viewContext;
        this.activity = activity;

        this.menuActions = new HashMap<>();
        menuActions.put(R.id.action_set_active_station, activity::onSetActiveStation);
        menuActions.put(R.id.action_comment, activity::onComment);
        menuActions.put(R.id.action_jump_to_table, activity::onJumpToTable);
        menuActions.put(R.id.action_jump_to_plan, activity::onJumpToPlan);
        menuActions.put(R.id.action_jump_to_elevation, activity::onJumpToElevation);
        menuActions.put(R.id.action_direction_left, activity::onSetDirectionLeft);
        menuActions.put(R.id.action_direction_right, activity::onSetDirectionRight);
        menuActions.put(R.id.action_reverse, activity::onReverse);
        menuActions.put(R.id.action_new_cross_section, activity::onNewCrossSection);
        menuActions.put(R.id.action_start_new_survey, activity::onStartNewSurvey);
        menuActions.put(R.id.action_unlink_survey, activity::onUnlinkSurvey);
        menuActions.put(R.id.action_rename_station, activity::onRenameStation);
        menuActions.put(R.id.action_edit_leg, activity::onEditLeg);
        menuActions.put(R.id.action_delete_station, activity::onDeleteStation);
        menuActions.put(R.id.action_delete_leg, activity::onDeleteStation);
        // Note: upgrade_splay and downgrade_leg are handled specially in handleMenuItemClick
    }

    /**
     * Show the station context menu at specific coordinates.
     * Uses a temporary invisible anchor view for precise positioning with PopupMenu.
     *
     * @param anchorView View to anchor the menu to (the view that was touched)
     * @param station Station for this context menu
     * @param survey Survey context (used to check linked surveys, origin station, etc.)
     * @param x X coordinate for menu position (relative to anchorView)
     * @param y Y coordinate for menu position (relative to anchorView)
     */
    public void showMenu(View anchorView, Station station, org.hwyl.sexytopo.model.survey.Survey survey, int x, int y) {
        // Use the window's decor view as the parent (it's a FrameLayout that supports free positioning)
        if (!(context instanceof android.app.Activity)) {
            // Fallback if context isn't an Activity
            showMenu(anchorView, station, survey);
            return;
        }

        android.app.Activity activity = (android.app.Activity) context;
        android.view.ViewGroup decorView = (android.view.ViewGroup) activity.getWindow().getDecorView();

        // Convert coordinates from anchorView-relative to screen coordinates
        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);

        int screenX = location[0] + x;
        int screenY = location[1] + y;

        // Create an invisible anchor view at the touch location
        View invisibleAnchor = new View(context);

        // Use FrameLayout.LayoutParams for positioning in the decor view
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(1, 1);
        params.leftMargin = screenX;
        params.topMargin = screenY;
        invisibleAnchor.setLayoutParams(params);

        decorView.addView(invisibleAnchor);

        // Create and show PopupMenu anchored to the invisible view
        PopupMenu popup = new PopupMenu(context, invisibleAnchor);
        popup.inflate(R.menu.station_context);

        configureMenuVisibility(popup.getMenu(), station, survey);
        setStationTitle(popup.getMenu(), station);

        popup.setOnMenuItemClickListener(item -> {
            boolean handled = handleMenuItemClick(item, station);
            // Remove the anchor view when menu is dismissed
            decorView.post(() -> decorView.removeView(invisibleAnchor));
            return handled;
        });

        popup.setOnDismissListener(menu -> {
            // Remove anchor view if menu dismissed without selection
            decorView.post(() -> {
                try {
                    decorView.removeView(invisibleAnchor);
                } catch (Exception e) {
                    // View might already be removed
                }
            });
        });

        popup.show();
    }

    /**
     * Show the station context menu anchored to the given view.
     * @param anchorView View to anchor the menu to
     * @param station Station for this context menu
     * @param survey Survey context (used to check linked surveys, origin station, etc.)
     */
    public void showMenu(View anchorView, Station station, org.hwyl.sexytopo.model.survey.Survey survey) {
        showMenu(anchorView, station, survey, null, null);
    }

    /**
     * Show the station context menu anchored to the given view with custom title.
     * @param anchorView View to anchor the menu to
     * @param station Station for this context menu
     * @param survey Survey context (used to check linked surveys, origin station, etc.)
     * @param customTitle Custom title to display (or null to use station name)
     * @param onDismiss Optional callback when menu is dismissed
     */
    public void showMenu(View anchorView, Station station, org.hwyl.sexytopo.model.survey.Survey survey,
                        String customTitle, Runnable onDismiss) {
        showMenu(anchorView, station, survey, customTitle, onDismiss, null);
    }

    /**
     * Show the station context menu with explicit leg context.
     * @param anchorView View to anchor the menu to
     * @param station Station for this context menu
     * @param survey Survey context
     * @param customTitle Custom title to display (or null to use station name)
     * @param onDismiss Optional callback when menu is dismissed
     * @param leg The specific leg clicked on (or null to infer from station)
     */
    public void showMenu(View anchorView, Station station, Survey survey,
                        String customTitle, Runnable onDismiss, Leg leg) {
        // Store the leg for action handlers
        this.currentLeg = leg;

        PopupMenu popup = new PopupMenu(context, anchorView);
        popup.inflate(R.menu.station_context);

        configureMenuVisibility(popup.getMenu(), station, survey, leg);
        if (customTitle != null) {
            setCustomTitle(popup.getMenu(), customTitle);
        } else {
            setStationTitle(popup.getMenu(), station);
        }

        popup.setOnMenuItemClickListener(item -> handleMenuItemClick(item, station));
        if (onDismiss != null) {
            popup.setOnDismissListener(menu -> onDismiss.run());
        }
        popup.show();
    }

    /**
     * Show the station context menu anchored to the given view (without survey context).
     * @deprecated Use showMenu(View, Station, Survey) for proper enable/disable logic
     */
    @Deprecated
    public void showMenu(View anchorView, Station station) {
        showMenu(anchorView, station, null);
    }

    /**
     * Configure which menu items are visible based on the current view context.
     */
    private void configureMenuVisibility(Menu menu, Station station, Survey survey) {
        configureMenuVisibility(menu, station, survey, null);
    }

    /**
     * Configure which menu items are visible based on the current view context.
     * @param leg The specific leg clicked on (or null to infer from station)
     */
    private void configureMenuVisibility(Menu menu, Station station, Survey survey, Leg leg) {
        // Enable/disable unlink survey based on whether station has connections
        MenuItem unlinkItem = menu.findItem(R.id.action_unlink_survey);
        if (unlinkItem != null && survey != null) {
            unlinkItem.setEnabled(survey.hasLinkedSurveys(station));
        }

        // Enable/disable comment based on whether this is the origin station
        MenuItem commentItem = menu.findItem(R.id.action_comment);
        if (commentItem != null && survey != null) {
            commentItem.setEnabled(station != survey.getOrigin());
        }

        // Configure upgrade/downgrade visibility and submenu title based on leg type
        if (survey != null) {
            // Use provided leg, or infer from station if not provided
            Leg referringLeg = (leg != null) ? leg : survey.getReferringLeg(station);
            MenuItem upgradeItem = menu.findItem(R.id.action_upgrade_splay);
            MenuItem downgradeItem = menu.findItem(R.id.action_downgrade_leg);
            MenuItem legSubmenu = menu.findItem(R.id.menu_leg);

            if (referringLeg != null) {
                boolean isSplay = !referringLeg.hasDestination();
                if (upgradeItem != null) {
                    upgradeItem.setVisible(isSplay);
                }
                if (downgradeItem != null) {
                    // Only show downgrade if it's a full leg AND destination has no onward legs
                    boolean canDowngrade = !isSplay &&
                        referringLeg.getDestination().getOnwardLegs().isEmpty();
                    downgradeItem.setVisible(canDowngrade);
                }
                // Set submenu title based on leg type
                if (legSubmenu != null) {
                    legSubmenu.setTitle(isSplay ? R.string.menu_splay : R.string.menu_leg);
                }
            } else {
                // No referring leg (origin station) - hide both
                if (upgradeItem != null) {
                    upgradeItem.setVisible(false);
                }
                if (downgradeItem != null) {
                    downgradeItem.setVisible(false);
                }
                // Default to "Leg" title
                if (legSubmenu != null) {
                    legSubmenu.setTitle(R.string.menu_leg);
                }
            }
        }

        // Configure direction radio buttons based on current station direction
        MenuItem leftItem = menu.findItem(R.id.action_direction_left);
        MenuItem rightItem = menu.findItem(R.id.action_direction_right);
        if (leftItem != null && rightItem != null) {
            org.hwyl.sexytopo.model.graph.Direction currentDirection =
                station.getExtendedElevationDirection();
            leftItem.setChecked(currentDirection == org.hwyl.sexytopo.model.graph.Direction.LEFT);
            rightItem.setChecked(currentDirection == org.hwyl.sexytopo.model.graph.Direction.RIGHT);
        }

        // Configure view-specific menu items using polymorphism
        // Do this last so we can hide anything made visible above
        viewContext.configureViewSpecificItems(menu);
    }

    /**
     * Set the menu title to show the station name with primary color background.
     */
    private void setStationTitle(Menu menu, Station station) {
        setCustomTitle(menu, station.getName());
    }

    /**
     * Set a custom menu title with primary color background.
     */
    private void setCustomTitle(Menu menu, String title) {
        MenuItem titleItem = menu.findItem(R.id.station_title);
        if (titleItem != null) {
            String displayTitle = " " + title + " ";
            SpannableString spannable = new SpannableString(displayTitle);

            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
            int backgroundColor = typedValue.data;

            spannable.setSpan(new BackgroundColorSpan(backgroundColor), 0, displayTitle.length(), 0);
            spannable.setSpan(new ForegroundColorSpan(Color.WHITE), 0, displayTitle.length(), 0);
            titleItem.setTitle(spannable);
        }
    }

    /**
     * Handle menu item clicks and delegate to the appropriate listener method.
     */
    private boolean handleMenuItemClick(MenuItem item, Station station) {
        int itemId = item.getItemId();

        // Special handling for actions that need the leg context
        if (itemId == R.id.action_upgrade_splay && currentLeg != null) {
            activity.onUpgradeSplay(currentLeg);
            return true;
        }
        if (itemId == R.id.action_downgrade_leg && currentLeg != null) {
            activity.onDowngradeLeg(currentLeg);
            return true;
        }
        if ((itemId == R.id.action_delete_leg || itemId == R.id.action_delete_station) && currentLeg != null) {
            activity.onDeleteLeg(currentLeg);
            return true;
        }

        StationAction action = menuActions.get(itemId);
        if (action != null) {
            action.execute(station);
            return true;
        }
        return false;
    }
}

package org.hwyl.sexytopo.control.graph;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;

import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.content.ContextCompat;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Unified manager for station context menus across all views (table, plan, elevation).
 * Handles menu creation, visibility logic, and provides a consistent interface for actions.
 */
public class ContextMenuManager {

    private final Context context;
    private final ViewContext viewContext;
    private final org.hwyl.sexytopo.control.activity.SurveyEditorActivity activity;
    private final Map<Integer, Consumer<Station>> menuActions;

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
        menuActions.put(R.id.action_toggle_left_right, activity::onToggleLeftRight);
        menuActions.put(R.id.action_reverse, activity::onReverse);
        menuActions.put(R.id.action_new_cross_section, activity::onNewCrossSection);
        menuActions.put(R.id.action_start_new_survey, activity::onStartNewSurvey);
        menuActions.put(R.id.action_unlink_survey, activity::onUnlinkSurvey);
        menuActions.put(R.id.action_rename_station, activity::onRenameStation);
        menuActions.put(R.id.action_edit_leg, activity::onEditLeg);
        menuActions.put(R.id.action_delete_station, activity::onDeleteStation);
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
        PopupMenu popup = new PopupMenu(context, anchorView);
        popup.inflate(R.menu.station_context);

        configureMenuVisibility(popup.getMenu(), station, survey);
        setStationTitle(popup.getMenu(), station);

        popup.setOnMenuItemClickListener(item -> handleMenuItemClick(item, station));
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

        // Configure view-specific menu items using polymorphism
        // Do this last so we can hide anything made visible above
        viewContext.configureViewSpecificItems(menu);
    }

    /**
     * Set the menu title to show the station name with primary color background.
     */
    private void setStationTitle(Menu menu, Station station) {
        MenuItem titleItem = menu.findItem(R.id.station_title);
        if (titleItem != null) {
            String stationName = " " + station.getName() + " ";
            SpannableString spannable = new SpannableString(stationName);

            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
            int backgroundColor = typedValue.data;

            spannable.setSpan(new BackgroundColorSpan(backgroundColor), 0, stationName.length(), 0);
            spannable.setSpan(new ForegroundColorSpan(Color.WHITE), 0, stationName.length(), 0);
            titleItem.setTitle(spannable);
        }
    }

    /**
     * Handle menu item clicks and delegate to the appropriate listener method.
     */
    private boolean handleMenuItemClick(MenuItem item, Station station) {
        Consumer<Station> action = menuActions.get(item.getItemId());
        if (action != null) {
            action.accept(station);
            return true;
        }
        return false;
    }
}

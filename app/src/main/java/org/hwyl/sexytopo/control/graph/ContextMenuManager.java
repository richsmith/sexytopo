package org.hwyl.sexytopo.control.graph;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.UpdateAppearance;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import java.util.HashMap;
import java.util.Map;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.model.graph.ExtendedElevationDirection;
import org.hwyl.sexytopo.model.sketch.CrossSection;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

/**
 * Unified manager for station context menus across all views (table, plan, elevation). Handles menu
 * creation, visibility logic, and provides a consistent interface for actions.
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

    public ContextMenuManager(
            Context context,
            ViewContext viewContext,
            org.hwyl.sexytopo.control.activity.SurveyEditorActivity activity) {
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
        menuActions.put(R.id.action_direction_vertical, activity::onSetDirectionVertical);
        menuActions.put(R.id.action_xsection_create, activity::onNewCrossSection);
        menuActions.put(
                R.id.action_xsection_create_horizontal, activity::onNewHorizontalCrossSection);
        menuActions.put(R.id.action_xsection_edit, activity::onEditCrossSection);
        menuActions.put(R.id.action_xsection_set_direction, activity::onRotateCrossSection);
        menuActions.put(R.id.action_xsection_delete, activity::onDeleteCrossSection);
        menuActions.put(R.id.action_start_new_survey, activity::onStartNewSurvey);
        menuActions.put(R.id.action_link_survey, activity::onLinkSurvey);
        menuActions.put(R.id.action_unlink_survey, activity::onUnlinkSurvey);
        menuActions.put(R.id.action_rename_station, activity::onRenameStation);
        menuActions.put(R.id.action_delete_station, activity::onDeleteStation);
    }

    /**
     * Show the station context menu at specific coordinates (used from graph views). Uses a
     * temporary invisible anchor view for precise positioning with PopupMenu.
     */
    public void showMenuForStation(View anchorView, Station station, Survey survey, int x, int y) {
        if (!(context instanceof Activity)) {
            showMenuForStation(anchorView, station, survey, null);
            return;
        }

        Activity activityContext = (Activity) context;
        ViewGroup decorView = (ViewGroup) activityContext.getWindow().getDecorView();

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);

        View invisibleAnchor = new View(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(1, 1);
        params.leftMargin = location[0] + x;
        params.topMargin = location[1] + y;
        invisibleAnchor.setLayoutParams(params);
        decorView.addView(invisibleAnchor);

        PopupMenu popup = new PopupMenu(context, invisibleAnchor);
        popup.inflate(R.menu.context_station);
        enableGroupDividers(popup);
        configureMenuVisibility(popup.getMenu(), station, survey, null);
        setStationTitle(popup.getMenu(), station);

        popup.setOnMenuItemClickListener(
                item -> {
                    boolean handled = handleMenuItemClick(item, station);
                    decorView.post(() -> decorView.removeView(invisibleAnchor));
                    return handled;
                });
        popup.setOnDismissListener(
                menu ->
                        decorView.post(
                                () -> {
                                    try {
                                        decorView.removeView(invisibleAnchor);
                                    } catch (Exception e) {
                                        // View might already be removed
                                    }
                                }));
        popup.show();
    }

    public void showMenuForStation(
            View anchorView, Station station, Survey survey, Runnable onDismiss) {
        PopupMenu popup = new PopupMenu(context, anchorView);
        popup.inflate(R.menu.context_station);
        enableGroupDividers(popup);
        configureMenuVisibility(popup.getMenu(), station, survey, null);
        setStationTitle(popup.getMenu(), station);
        popup.setOnMenuItemClickListener(item -> handleMenuItemClick(item, station));
        if (onDismiss != null) {
            popup.setOnDismissListener(menu -> onDismiss.run());
        }
        popup.show();
    }

    public void showMenuForLeg(
            View anchorView,
            Station station,
            Survey survey,
            String customTitle,
            Runnable onDismiss,
            Leg leg) {
        PopupMenu popup = new PopupMenu(context, anchorView);
        popup.inflate(R.menu.context_leg);
        enableGroupDividers(popup);
        configureMenuVisibility(popup.getMenu(), station, survey, leg);
        setCustomTitle(popup.getMenu(), customTitle != null ? customTitle : station.getName());
        popup.setOnMenuItemClickListener(item -> handleMenuItemClick(item, station));
        if (onDismiss != null) {
            popup.setOnDismissListener(menu -> onDismiss.run());
        }
        popup.show();
    }

    private void enableGroupDividers(PopupMenu popup) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            popup.getMenu().setGroupDividerEnabled(true);
        }
    }

    private void configureMenuVisibility(Menu menu, Station station, Survey survey, Leg leg) {
        // Enable/disable link/unlink survey based on whether station has linked surveys
        MenuItem linkItem = menu.findItem(R.id.action_link_survey);
        MenuItem unlinkItem = menu.findItem(R.id.action_unlink_survey);
        if (survey != null) {
            boolean hasLinks = survey.hasLinkedSurveys(station);
            if (linkItem != null) {
                linkItem.setEnabled(!hasLinks);
            }
            if (unlinkItem != null) {
                unlinkItem.setEnabled(hasLinks);
            }
        }

        if (survey != null) {
            currentLeg = (leg != null) ? leg : survey.getReferringLeg(station);

            MenuItem upgradeItem = menu.findItem(R.id.action_upgrade_splay);
            MenuItem promoteItem = menu.findItem(R.id.action_promote_to_above_leg);
            MenuItem downgradeItem = menu.findItem(R.id.action_downgrade_leg);
            MenuItem reverseItem = menu.findItem(R.id.action_reverse);
            MenuItem legMenuItem = menu.findItem(R.id.menu_leg);

            if (currentLeg != null) {
                boolean isSplay = !currentLeg.hasDestination();
                if (upgradeItem != null) {
                    upgradeItem.setVisible(isSplay);
                }
                if (promoteItem != null) {
                    promoteItem.setVisible(isSplay);
                }
                if (reverseItem != null) {
                    reverseItem.setVisible(!isSplay);
                }
                if (downgradeItem != null) {
                    boolean canDowngrade =
                            !isSplay && currentLeg.getDestination().getOnwardLegs().isEmpty();
                    downgradeItem.setVisible(!isSplay);
                    downgradeItem.setEnabled(canDowngrade);
                }

                MenuItem commentLegItem = menu.findItem(R.id.action_comment_leg);
                if (commentLegItem != null) {
                    commentLegItem.setTitle(
                            isSplay ? R.string.menu_comment_splay : R.string.menu_comment_leg);
                }
                if (legMenuItem != null) {
                    legMenuItem.setTitle(R.string.menu_incoming_leg);
                    if (!isSplay) {
                        Station fromStation = survey.getOriginatingStation(currentLeg);
                        String from =
                                currentLeg.wasShotBackwards()
                                        ? currentLeg.getDestination().getName()
                                        : fromStation.getName();
                        String to =
                                currentLeg.wasShotBackwards()
                                        ? fromStation.getName()
                                        : currentLeg.getDestination().getName();
                        legMenuItem
                                .getSubMenu()
                                .setHeaderTitle(
                                        context.getString(
                                                R.string.menu_leg_title_dynamic, from, to));
                    }
                }
            } else {
                // Origin station — hide leg submenu and upgrade/downgrade
                if (upgradeItem != null) {
                    upgradeItem.setVisible(false);
                }
                if (downgradeItem != null) {
                    downgradeItem.setVisible(false);
                }
                if (legMenuItem != null) {
                    legMenuItem.setVisible(false);
                }
            }
        }

        // Configure direction radio buttons
        MenuItem leftItem = menu.findItem(R.id.action_direction_left);
        MenuItem rightItem = menu.findItem(R.id.action_direction_right);
        MenuItem verticalItem = menu.findItem(R.id.action_direction_vertical);
        if (leftItem != null && rightItem != null) {
            ExtendedElevationDirection currentDirection = station.getExtendedElevationDirection();
            leftItem.setChecked(currentDirection == ExtendedElevationDirection.LEFT);
            rightItem.setChecked(currentDirection == ExtendedElevationDirection.RIGHT);
            if (verticalItem != null) {
                verticalItem.setChecked(currentDirection == ExtendedElevationDirection.VERTICAL);
            }
        }

        // Cross-section submenu: enable/disable based on whether one exists at this station.
        if (survey != null) {
            boolean hasCrossSection = activity.hasCrossSection(station);
            MenuItem createItem = menu.findItem(R.id.action_xsection_create);
            MenuItem createHorizontalItem = menu.findItem(R.id.action_xsection_create_horizontal);
            MenuItem editItem = menu.findItem(R.id.action_xsection_edit);
            MenuItem setDirectionItem = menu.findItem(R.id.action_xsection_set_direction);
            MenuItem deleteItem = menu.findItem(R.id.action_xsection_delete);
            if (createItem != null) {
                createItem.setEnabled(!hasCrossSection);
            }
            if (createHorizontalItem != null) {
                createHorizontalItem.setEnabled(!hasCrossSection);
            }
            if (editItem != null) {
                // The editor activity only exists in the new mode; hide Edit for legacy
                // cross-sections.
                boolean legacyCrossSections = GeneralPreferences.isLegacyCrossSectionsOn();
                editItem.setVisible(!legacyCrossSections);
                editItem.setEnabled(hasCrossSection);
            }
            if (setDirectionItem != null) {
                setDirectionItem.setEnabled(activity.hasRotatableCrossSection(station));
            }
            if (deleteItem != null) {
                deleteItem.setEnabled(hasCrossSection);
            }
        }

        viewContext.configureViewSpecificItems(menu);

        if (viewContext.canCreateHorizontalCrossSection()) {
            // With two kinds to choose from, plain "New Cross-Section" needs to say which it is
            setCrossSectionCreateTitle(
                    menu,
                    R.id.action_xsection_create,
                    CrossSection.Orientation.VERTICAL,
                    R.string.menu_xsection_create_vertical,
                    R.string.menu_xsection_hint_vertical);
            setCrossSectionCreateTitle(
                    menu,
                    R.id.action_xsection_create_horizontal,
                    CrossSection.Orientation.HORIZONTAL,
                    R.string.menu_xsection_create_horizontal,
                    R.string.menu_xsection_hint_horizontal);
        }
    }

    /**
     * Titles a new-cross-section item as name, glyph and then a smaller, fainter hint at where that
     * kind of section is used, e.g. "New Vertical Section ↕ passage". The glyph goes after the name
     * so that the names line up.
     */
    private static final float HINT_ALPHA = 0.6f;

    /**
     * Fades text by scaling the alpha of whatever colour it is already drawn in. Unlike a fixed
     * colour, this still follows the item's state, so the text greys out when the item is disabled.
     */
    private static class FadedSpan extends CharacterStyle implements UpdateAppearance {
        private final float alpha;

        FadedSpan(float alpha) {
            this.alpha = alpha;
        }

        @Override
        public void updateDrawState(TextPaint textPaint) {
            textPaint.setAlpha(Math.round(textPaint.getAlpha() * alpha));
        }
    }

    private void setCrossSectionCreateTitle(
            Menu menu,
            int itemId,
            CrossSection.Orientation orientation,
            int titleRes,
            int hintRes) {
        MenuItem item = menu.findItem(itemId);
        if (item == null) {
            return;
        }
        String title = context.getString(titleRes) + " " + CrossSectionLabels.getGlyph(orientation);
        String hint = context.getString(hintRes);
        SpannableString spannable = new SpannableString(title + "\u2003" + hint);
        int hintStart = spannable.length() - hint.length();

        spannable.setSpan(new FadedSpan(HINT_ALPHA), hintStart, spannable.length(), 0);
        spannable.setSpan(new RelativeSizeSpan(0.8f), hintStart, spannable.length(), 0);
        item.setTitle(spannable);
    }

    private void setStationTitle(Menu menu, Station station) {
        setCustomTitle(menu, station.getName());
    }

    private void setCustomTitle(Menu menu, String title) {
        MenuItem titleItem = menu.findItem(R.id.station_title);
        if (titleItem == null) return;

        String displayTitle = " " + title + " ";
        SpannableString spannable = new SpannableString(displayTitle);

        TypedValue typedValue = new TypedValue();
        context.getTheme()
                .resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);

        spannable.setSpan(new BackgroundColorSpan(typedValue.data), 0, displayTitle.length(), 0);
        spannable.setSpan(new ForegroundColorSpan(Color.WHITE), 0, displayTitle.length(), 0);
        titleItem.setTitle(spannable);
    }

    private boolean handleMenuItemClick(MenuItem item, Station station) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_upgrade_splay && currentLeg != null) {
            activity.onUpgradeSplay(currentLeg);
            return true;
        }
        if (itemId == R.id.action_promote_to_above_leg && currentLeg != null) {
            activity.onPromoteToAboveLeg(currentLeg);
            return true;
        }
        if (itemId == R.id.action_downgrade_leg && currentLeg != null) {
            activity.onDowngradeLeg(currentLeg);
            return true;
        }
        if (itemId == R.id.action_edit_leg && currentLeg != null) {
            activity.onEditLeg(currentLeg);
            return true;
        }
        if (itemId == R.id.action_reverse && currentLeg != null) {
            activity.onReverse(currentLeg);
            return true;
        }
        if (itemId == R.id.action_delete_leg && currentLeg != null) {
            activity.onDeleteLeg(currentLeg);
            return true;
        }
        if (itemId == R.id.action_comment_leg && currentLeg != null) {
            activity.onCommentLeg(currentLeg);
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

package org.hwyl.sexytopo.control.activity;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.WindowManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.graph.ContextMenuManager;
import org.hwyl.sexytopo.control.table.ManualEntry;
import org.hwyl.sexytopo.control.util.SurveyStats;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.graph.Direction;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;

/**
 * Abstract base class for activities that allow editing surveys with context menus.
 * Provides standard context menu action implementations.
 */
public abstract class SurveyEditorActivity extends SexyTopoActivity {

    public void onSetActiveStation(Station station) {
        setActiveStation(station);
        invalidateView();
    }

    public void onComment(Station station) {
        openCommentDialog(station);
    }

    public void onJumpToTable(Station station) {
        jumpToStation(station, TableActivity.class);
    }

    public void onJumpToPlan(Station station) {
        jumpToStation(station, PlanActivity.class);
    }

    public void onJumpToElevation(Station station) {
        jumpToStation(station, ExtendedElevationActivity.class);
    }

    public void onToggleLeftRight(Station station) {
        Direction newDirection = station.getExtendedElevationDirection().opposite();
        SurveyUpdater.setDirectionOfSubtree(station, newDirection);
        getSurveyManager().broadcastSurveyUpdated();
        invalidateView();
    }

    public void onReverse(Station station) {
        SurveyUpdater.reverseLeg(getSurvey(), station);
        getSurveyManager().broadcastSurveyUpdated();
        invalidateView();
    }

    public void onNewCrossSection(Station station) {
        // Default: not applicable in some views
        // Override in activities that support adding cross-sections
    }

    public void onStartNewSurvey(Station station) {
        if (!getSurvey().isSaved()) {
            showSimpleToast(R.string.file_cannot_extend_unsaved_survey);
            return;
        }
        continueSurvey(station);
    }

    public void onUnlinkSurvey(Station station) {
        unlinkSurvey(station);
    }

    public void onRenameStation(Station station) {
        // Default: not applicable in some views
        // Override in activities that support renaming
    }

    public void onEditLeg(Station station) {
        // Get the leg that leads to this station
        Leg leg = getSurvey().getReferringLeg(station);
        if (leg == null) {
            // This is the origin station, cannot edit
            return;
        }

        // Get the station this leg originates from
        Station fromStation = getSurvey().getOriginatingStation(leg);
        if (fromStation == null) {
            return;
        }

        // Call the unified edit leg dialog
        ManualEntry.editLeg(this, getSurvey(), fromStation, leg);
    }

    public void onDeleteStation(Station station) {
        askAboutDeletingStation(station);
        invalidateView();
    }

    /**
     * Set the active station in the current view.
     */
    protected void setActiveStation(Station station) {
        getSurvey().setActiveStation(station);
        getSurveyManager().broadcastSurveyUpdated();
    }

    /**
     * Invalidate/refresh the current view.
     * Must be implemented by subclasses.
     */
    protected abstract void invalidateView();


    /**
     * Open the comment dialog for the given station.
     */
    protected void openCommentDialog(Station station) {
        TextInputLayout inputLayout = new TextInputLayout(this);
        inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        inputLayout.setHint(getString(R.string.graph_comment_hint));

        TextInputEditText input = new TextInputEditText(this);
        input.setLines(8);
        input.setGravity(Gravity.START | Gravity.TOP);
        input.setText(station.getComment());
        input.setFocusableInTouchMode(true);
        inputLayout.addView(input);

        int paddingH = (int) (24 * getResources().getDisplayMetrics().density);
        int paddingV = (int) (20 * getResources().getDisplayMetrics().density);
        inputLayout.setPadding(paddingH, paddingV, paddingH, 0);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setView(inputLayout)
            .setTitle(station.getName())
            .setPositiveButton(R.string.save, (dialog, which) -> {
                    station.setComment(input.getText().toString());
                    invalidateView();
                })
            .setNegativeButton(R.string.cancel, null);

        Dialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();
        input.requestFocus();
    }

    /**
     * Ask the user about deleting the given station.
     */
    protected void askAboutDeletingStation(Station station) {
        int numFullLegsToBeDeleted = 1 + SurveyStats.calcNumberSubFullLegs(station);
        int numSplaysToBeDeleted = SurveyStats.calcNumberSubSplays(station);

        String message = getString(R.string.context_this_will_delete);

        if (numFullLegsToBeDeleted > 0) {
            String noun = getString(R.string.leg).toLowerCase();
            message += "\n" + TextTools.pluralise(numFullLegsToBeDeleted, noun);
            noun = getString(R.string.station).toLowerCase();
            message += " (" + TextTools.pluralise(numFullLegsToBeDeleted, noun) + ")";
        }
        if (numSplaysToBeDeleted > 0) {
            String noun = getString(R.string.splay).toLowerCase();
            message += "\n" + TextTools.pluralise(numSplaysToBeDeleted, noun);
        }

        new MaterialAlertDialogBuilder(this)
            .setMessage(message)
            .setPositiveButton(R.string.delete, (dialog, which) -> {
                    SurveyUpdater.deleteStation(getSurvey(), station);
                    getSurveyManager().broadcastSurveyUpdated();
                    invalidateView();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
}

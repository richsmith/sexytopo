package org.hwyl.sexytopo.control.activity;

import android.app.Dialog;
import android.view.Gravity;
import android.view.WindowManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.table.LegDialogs;
import org.hwyl.sexytopo.control.util.InputMode;
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

    public void onSetDirectionLeft(Station station) {
        if (station.getExtendedElevationDirection() != Direction.LEFT) {
            SurveyUpdater.setDirectionOfSubtree(station, Direction.LEFT);
            getSurveyManager().broadcastSurveyUpdated();
            invalidateView();
        }
    }

    public void onSetDirectionRight(Station station) {
        if (station.getExtendedElevationDirection() != Direction.RIGHT) {
            SurveyUpdater.setDirectionOfSubtree(station, Direction.RIGHT);
            getSurveyManager().broadcastSurveyUpdated();
            invalidateView();
        }
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
        LegDialogs.editLeg(this, getSurvey(), fromStation, leg);
    }

    public void onDeleteStation(Station station) {
        Leg leg = getSurvey().getReferringLeg(station);
        if (leg == null) {
            return;
        }

        onDeleteLeg(leg);
    }

    public void onDeleteLeg(Leg leg) {
        if (leg == null) {
            return;
        }

        askAboutDeletingLeg(leg);
    }

    public void onUpgradeSplay(Leg leg) {
        if (leg == null || leg.hasDestination()) {
            return;
        }

        SurveyUpdater.upgradeSplay(getSurvey(), leg, InputMode.FORWARD);
        getSurveyManager().broadcastSurveyUpdated();
    }

    public void onDowngradeLeg(Leg leg) {
        if (leg == null || !leg.hasDestination()) {
            return;
        }

        SurveyUpdater.downgradeLeg(getSurvey(), leg);
        getSurveyManager().broadcastSurveyUpdated();
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
     * Synchronise the view with the survey data.
     * Must be implemented by subclasses.
     */
    public abstract void syncWithSurvey();


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
     * Ask the user about deleting the given leg (and any onwards legs
     * / stations if it's a full leg).
     * @noinspection UnusedAssignment
     */
    protected void askAboutDeletingLeg(Leg leg) {
        Station fromStation = getSurvey().getOriginatingStation(leg);
        if (fromStation == null) {
            return;
        }

        int numFullLegsToBeDeleted = 0;
        int numSplaysToBeDeleted = 0;

        if (leg.hasDestination()) {
            Station toStation = leg.getDestination();
            numFullLegsToBeDeleted = 1 + SurveyStats.calcNumberSubFullLegs(toStation);
            numSplaysToBeDeleted = SurveyStats.calcNumberSubSplays(toStation);
        } else {
            numSplaysToBeDeleted = 1;
        }

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
            .setTitle(R.string.delete_question)
            .setMessage(message)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(R.string.delete, (dialog, which) -> {
                SurveyUpdater.deleteLeg(getSurvey(), fromStation, leg);
                getSurveyManager().broadcastSurveyUpdated();
                invalidateView();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }
}

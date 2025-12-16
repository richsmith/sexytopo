package org.hwyl.sexytopo.control.activity;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.graph.ContextMenuManager;
import org.hwyl.sexytopo.control.util.SurveyUpdater;
import org.hwyl.sexytopo.model.graph.Direction;
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
        handleNewCrossSection(station);
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
        // Default: not applicable in graph views
        // Override in activities that support renaming
    }

    public void onEditLeg(Station station) {
        // Default: not applicable in graph views
        // Override in activities that support leg editing
    }

    public void onDeleteStation(Station station) {
        askAboutDeletingStation(station);
        invalidateView();
    }

    /**
     * Set the active station in the current view.
     * Must be implemented by subclasses.
     */
    protected abstract void setActiveStation(Station station);

    /**
     * Invalidate/refresh the current view.
     * Must be implemented by subclasses.
     */
    protected abstract void invalidateView();

    /**
     * Open the comment dialog for the given station.
     * Must be implemented by subclasses.
     */
    protected abstract void openCommentDialog(Station station);

    /**
     * Handle creating a new cross section at the given station.
     * Must be implemented by subclasses.
     */
    protected abstract void handleNewCrossSection(Station station);

    /**
     * Ask the user about deleting the given station.
     * Must be implemented by subclasses.
     */
    protected abstract void askAboutDeletingStation(Station station);
}

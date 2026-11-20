package org.hwyl.sexytopo.control.sketch;

import android.view.MotionEvent;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

/**
 * Makes the tapped station the active one. Tapping the station that's already active opens its
 * context menu.
 */
class SelectTool implements ToolHandler {

    private final SketchState state;
    private final SketchSelector selector;
    private final ToolHost host;

    SelectTool(SketchState state, SketchSelector selector, ToolHost host) {
        this.state = state;
        this.selector = selector;
        this.host = host;
    }

    @Override
    public boolean onTouch(MotionEvent event) {

        Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Station newSelectedStation = selector.findStationAt(touchPointOnView);
                Survey survey = state.getSurvey();

                if (newSelectedStation == null) {
                    // keep the rest of the touch coming: it may be a tap on a linked survey
                    return true;

                } else if (newSelectedStation != survey.getActiveStation()) {
                    survey.setActiveStation(newSelectedStation);
                    host.broadcastSurveyUpdated();
                    host.invalidate();
                    return true;

                } else { // double selection opens context menu
                    host.openStationMenu(event, newSelectedStation);
                    return true;
                }

            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                break;
            default:
                return false;
        }

        return true;
    }
}

package org.hwyl.sexytopo.control.sketch;

import android.content.Context;
import android.view.MotionEvent;
import org.hwyl.sexytopo.model.survey.Station;

/** What the sketch tools need the view they're part of to do. */
interface ToolHost {

    Context getContext();

    void invalidate();

    void showToast(int stringId);

    /** Let the rest of the app know the survey has changed, e.g. a new active station. */
    void broadcastSurveyUpdated();

    /** Open a station's context menu, unless one has already been opened during this touch. */
    void openStationMenu(MotionEvent event, Station station);
}

package org.hwyl.sexytopo.control.sketch;

import android.content.Context;
import android.view.MotionEvent;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.model.geometry.Coord2D;

/** Asks for some text and places it where the user tapped. */
class TextTool implements ToolHandler {

    private final Viewport viewport;
    private final SketchState state;
    private final ToolHost host;

    TextTool(Viewport viewport, SketchState state, ToolHost host) {
        this.viewport = viewport;
        this.state = state;
        this.host = host;
    }

    @Override
    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    public boolean onTouch(MotionEvent event) {

        final Coord2D touchPointOnView = new Coord2D(event.getX(), event.getY());
        final Coord2D touchPointOnSurvey = viewport.toSurvey(touchPointOnView);
        Context context = host.getContext();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                TextEntryDialog.show(
                        context,
                        "",
                        text -> {
                            int startingSizeSp = GeneralPreferences.getTextStartingSizeSp();
                            float startingSizePixels =
                                    ScreenUnits.spToPixels(context, startingSizeSp);
                            float size = startingSizePixels / viewport.getScale();
                            state.getSketch()
                                    .addTextDetail(
                                            touchPointOnSurvey, text, state.getColour(), size);
                            host.invalidate();
                        });
                return true;

            default:
                return false;
        }
    }
}

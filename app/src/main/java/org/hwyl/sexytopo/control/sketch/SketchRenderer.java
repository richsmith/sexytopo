package org.hwyl.sexytopo.control.sketch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import java.util.function.BooleanSupplier;
import org.hwyl.sexytopo.control.util.SketchPreferences;
import org.hwyl.sexytopo.model.sketch.SinglePositionDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.SketchDetail;

/**
 * Draws a sketch: areas underneath, then lines, then text and symbols on top. Used for the main
 * sketch, for linked surveys' sketches, and for each cross-section's own sketch.
 */
class SketchRenderer {

    // whether to mark each point of each line, as an aid to debugging
    private final BooleanSupplier isDebugMode;

    private final AreaRenderer areaRenderer;
    private final LineRenderer lineRenderer;
    private final PointRenderer pointRenderer;

    private boolean isDarkModeActive = false;

    SketchRenderer(Viewport viewport, BooleanSupplier isDebugMode) {
        this.isDebugMode = isDebugMode;
        this.areaRenderer = new AreaRenderer(viewport);
        this.lineRenderer = new LineRenderer(viewport);
        this.pointRenderer = new PointRenderer(viewport);
    }

    /** Set the paints up from the theme and current preferences. */
    void initialise(Context context) {
        areaRenderer.initialise();
        lineRenderer.initialise(context);
        pointRenderer.initialise(context);
    }

    /** In dark mode, details drawn in black are drawn in white instead. */
    void setDarkModeActive(boolean isDarkModeActive) {
        this.isDarkModeActive = isDarkModeActive;
    }

    void draw(Canvas canvas, Sketch sketch, int alpha) {
        draw(canvas, sketch, alpha, null);
    }

    /**
     * Draws the sketch, leaving out the hidden detail, which something else is drawing (e.g. a
     * symbol being moved), if there is one.
     */
    void draw(Canvas canvas, Sketch sketch, int alpha, SketchDetail hidden) {

        if (!SketchPreferences.Toggle.SHOW_SKETCH.isOn()) {
            return;
        }

        areaRenderer.draw(canvas, sketch, alpha, isDarkModeActive);
        lineRenderer.draw(canvas, sketch, alpha, isDarkModeActive, isDebugMode.getAsBoolean());
        pointRenderer.draw(canvas, sketch, alpha, isDarkModeActive, hidden);
    }

    /** Draws a single symbol or piece of text. */
    void drawDetail(Canvas canvas, SinglePositionDetail detail) {
        pointRenderer.drawDetail(canvas, detail, SketchPaints.SOLID_ALPHA, isDarkModeActive);
    }

    /** Where a symbol or piece of text is drawn on the view. */
    RectF getBoundsOnView(SinglePositionDetail detail) {
        return pointRenderer.getBoundsOnView(detail);
    }
}

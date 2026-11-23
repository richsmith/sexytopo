package org.hwyl.sexytopo.control.sketch;

import android.content.Context;
import android.graphics.Canvas;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.hwyl.sexytopo.control.util.SketchPreferences;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.SymbolDetail;

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

        if (!SketchPreferences.Toggle.SHOW_SKETCH.isOn()) {
            return;
        }

        areaRenderer.draw(canvas, sketch, alpha, isDarkModeActive);
        lineRenderer.draw(canvas, sketch, alpha, isDarkModeActive, isDebugMode.getAsBoolean());
        pointRenderer.draw(canvas, sketch, alpha, isDarkModeActive);
    }

    /**
     * Where to find a symbol to leave out when drawing its sketch, e.g. while it's drawn elsewhere
     * being moved. It gives null when there's nothing to leave out.
     */
    void setHiddenSymbol(Supplier<SymbolDetail> hiddenSymbol) {
        pointRenderer.setHiddenSymbol(hiddenSymbol);
    }

    void drawSymbol(Canvas canvas, SymbolDetail symbolDetail) {
        pointRenderer.drawSymbol(canvas, symbolDetail, SketchPaints.SOLID_ALPHA, isDarkModeActive);
    }
}

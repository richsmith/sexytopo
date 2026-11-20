package org.hwyl.sexytopo.control.sketch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import androidx.core.content.ContextCompat;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.Symbol;
import org.hwyl.sexytopo.model.sketch.SymbolDetail;
import org.hwyl.sexytopo.model.sketch.TextDetail;

/**
 * Draws a sketch's text and symbols: the details placed at a single point and scaled with the zoom.
 */
class PointRenderer {

    private final Viewport viewport;

    private final Paint labelPaint = new Paint();

    // one colour filter per colour used, so symbols don't each need a new one on every frame
    private final SparseArray<PorterDuffColorFilter> colourFilters = new SparseArray<>();

    PointRenderer(Viewport viewport) {
        this.viewport = viewport;
    }

    /** Set the text paint up from the theme and current preferences. */
    void initialise(Context context) {
        SketchPaints.applyAntiAliasPreference(labelPaint);

        int labelColour = ContextCompat.getColor(context, R.color.station);
        labelPaint.setColor(labelColour);
        int labelSizeSp = GeneralPreferences.getLabelFontSizeSp();
        float labelSizePixels = ScreenUnits.spToPixels(context, labelSizeSp);
        labelPaint.setTextSize(labelSizePixels);
    }

    void draw(Canvas canvas, Sketch sketch, int alpha, boolean isDarkModeActive) {
        drawText(canvas, sketch, alpha, isDarkModeActive);
        drawSymbols(canvas, sketch, alpha, isDarkModeActive);
    }

    private void drawText(Canvas canvas, Sketch sketch, int alpha, boolean isDarkModeActive) {
        float viewScale = viewport.getScale();

        for (TextDetail textDetail : sketch.getTextDetails()) {
            if (!viewport.couldBeVisible(textDetail)) {
                continue;
            }
            Coord2D location = viewport.toView(textDetail.getPosition());
            float x = location.x, y = location.y;
            String text = textDetail.getText();
            // setColor replaces the alpha too, so set the alpha after it
            labelPaint.setColor(textDetail.getDrawColour(isDarkModeActive).intValue);
            labelPaint.setAlpha(alpha);
            labelPaint.setTextSize(textDetail.getSize() * viewScale);
            for (String line : text.split("\n")) {
                canvas.drawText(line, x, y, labelPaint);
                y += labelPaint.descent() - labelPaint.ascent();
            }
        }
    }

    private void drawSymbols(Canvas canvas, Sketch sketch, int alpha, boolean isDarkModeActive) {
        float viewScale = viewport.getScale();

        for (SymbolDetail symbolDetail : sketch.getSymbolDetails()) {
            if (!viewport.couldBeVisible(symbolDetail)) {
                continue;
            }
            Coord2D location = viewport.toView(symbolDetail.getPosition());

            Symbol symbol = symbolDetail.getSymbol();

            int size = Math.round(symbolDetail.getSize() * viewScale);
            if (size < 1) {
                continue;
            }

            Drawable drawable = symbolDetail.getDrawable();
            float offset = size / 2f;
            int x = Math.round(location.x - offset), y = Math.round(location.y - offset);
            drawable.setBounds(x, y, x + size, y + size);
            drawable.setAlpha(alpha);
            PorterDuffColorFilter colourFilter =
                    getColourFilter(symbolDetail.getDrawColour(isDarkModeActive));
            if (drawable.getColorFilter() != colourFilter) {
                drawable.setColorFilter(colourFilter);
            }

            if (symbol.isDirectional()) {
                canvas.save();
                canvas.rotate(symbolDetail.getAngle(), location.x, location.y);
                drawable.draw(canvas);
                canvas.restore();
            } else {
                drawable.draw(canvas);
            }
        }
    }

    private PorterDuffColorFilter getColourFilter(Colour colour) {
        PorterDuffColorFilter colourFilter = colourFilters.get(colour.intValue);
        if (colourFilter == null) {
            colourFilter = new PorterDuffColorFilter(colour.intValue, PorterDuff.Mode.SRC_IN);
            colourFilters.put(colour.intValue, colourFilter);
        }
        return colourFilter;
    }
}

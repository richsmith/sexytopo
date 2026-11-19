package org.hwyl.sexytopo.control.sketch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RotateDrawable;
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
            Colour drawColour = symbolDetail.getDrawColour(isDarkModeActive);
            drawable.setColorFilter(
                    new PorterDuffColorFilter(drawColour.intValue, PorterDuff.Mode.SRC_IN));

            if (symbol.isDirectional()) {
                RotateDrawable rotateDrawable = new RotateDrawable();
                rotateDrawable.setDrawable(drawable);
                rotateDrawable.setPivotX(0.5f);
                rotateDrawable.setPivotY(0.5f);
                rotateDrawable.setLevel((int) (10000 * (symbolDetail.getAngle() / 360f)));
                rotateDrawable.setBounds(x, y, x + size, y + size);
                rotateDrawable.draw(canvas);

            } else { // skip some calcs for efficiency
                drawable.draw(canvas);
            }
        }
    }
}

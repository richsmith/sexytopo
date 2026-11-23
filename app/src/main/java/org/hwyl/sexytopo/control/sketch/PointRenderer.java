package org.hwyl.sexytopo.control.sketch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import androidx.core.content.ContextCompat;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.util.GeneralPreferences;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.SinglePositionDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.SketchDetail;
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

    /** Draws the sketch's text and symbols, apart from any hidden one. */
    void draw(
            Canvas canvas,
            Sketch sketch,
            int alpha,
            boolean isDarkModeActive,
            SketchDetail hidden) {
        drawText(canvas, sketch, alpha, isDarkModeActive, hidden);
        drawSymbols(canvas, sketch, alpha, isDarkModeActive, hidden);
    }

    private void drawText(
            Canvas canvas,
            Sketch sketch,
            int alpha,
            boolean isDarkModeActive,
            SketchDetail hidden) {
        for (TextDetail textDetail : sketch.getTextDetails()) {
            if (textDetail == hidden || !viewport.couldBeVisible(textDetail)) {
                continue;
            }
            drawTextDetail(canvas, textDetail, alpha, isDarkModeActive);
        }
    }

    private void drawSymbols(
            Canvas canvas,
            Sketch sketch,
            int alpha,
            boolean isDarkModeActive,
            SketchDetail hidden) {
        for (SymbolDetail symbolDetail : sketch.getSymbolDetails()) {
            if (symbolDetail == hidden || !viewport.couldBeVisible(symbolDetail)) {
                continue;
            }
            drawSymbol(canvas, symbolDetail, alpha, isDarkModeActive);
        }
    }

    /** Draws a single symbol or piece of text. */
    void drawDetail(
            Canvas canvas, SinglePositionDetail detail, int alpha, boolean isDarkModeActive) {
        if (detail instanceof SymbolDetail) {
            drawSymbol(canvas, (SymbolDetail) detail, alpha, isDarkModeActive);
        } else if (detail instanceof TextDetail) {
            drawTextDetail(canvas, (TextDetail) detail, alpha, isDarkModeActive);
        }
    }

    /**
     * Where a symbol or piece of text is drawn on the view: the square a symbol is drawn in (before
     * any turning), or the box round the text as drawn.
     */
    RectF getBoundsOnView(SinglePositionDetail detail) {
        Coord2D location = viewport.toView(detail.getPosition());
        if (detail instanceof TextDetail) {
            return getTextBoundsOnView((TextDetail) detail, location);
        }
        float half =
                detail instanceof SymbolDetail
                        ? ((SymbolDetail) detail).getSize() * viewport.getScale() / 2
                        : 0;
        return new RectF(
                location.x - half, location.y - half, location.x + half, location.y + half);
    }

    private RectF getTextBoundsOnView(TextDetail textDetail, Coord2D location) {
        String text = textDetail.getText();
        labelPaint.setTextSize(textDetail.getSize() * viewport.getScale());
        float lineHeight = labelPaint.descent() - labelPaint.ascent();
        float width = 0;
        int lines = 0;
        int lineStart = 0;
        while (lineStart <= text.length()) {
            int lineEnd = getLineEnd(text, lineStart);
            width = Math.max(width, labelPaint.measureText(text, lineStart, lineEnd));
            lines++;
            lineStart = lineEnd + 1;
        }
        return new RectF(
                location.x,
                location.y + labelPaint.ascent(),
                location.x + width,
                location.y + (lines - 1) * lineHeight + labelPaint.descent());
    }

    private void drawTextDetail(
            Canvas canvas, TextDetail textDetail, int alpha, boolean isDarkModeActive) {
        Coord2D location = viewport.toView(textDetail.getPosition());
        float x = location.x, y = location.y;
        String text = textDetail.getText();
        // setColor replaces the alpha too, so set the alpha after it
        labelPaint.setColor(textDetail.getDrawColour(isDarkModeActive).intValue);
        labelPaint.setAlpha(alpha);
        labelPaint.setTextSize(textDetail.getSize() * viewport.getScale());
        float lineHeight = labelPaint.descent() - labelPaint.ascent();
        // draw each line by its place in the text, rather than splitting it into new strings
        int lineStart = 0;
        while (lineStart <= text.length()) {
            int lineEnd = getLineEnd(text, lineStart);
            canvas.drawText(text, lineStart, lineEnd, x, y, labelPaint);
            y += lineHeight;
            lineStart = lineEnd + 1;
        }
    }

    /** Where the line of text starting at lineStart ends: at the next newline, or the end. */
    private static int getLineEnd(String text, int lineStart) {
        int lineEnd = text.indexOf('\n', lineStart);
        return lineEnd == -1 ? text.length() : lineEnd;
    }

    private void drawSymbol(
            Canvas canvas, SymbolDetail symbolDetail, int alpha, boolean isDarkModeActive) {
        Coord2D location = viewport.toView(symbolDetail.getPosition());

        int size = Math.round(symbolDetail.getSize() * viewport.getScale());
        if (size < 1) {
            return;
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

        if (symbolDetail.getSymbol().isDirectional()) {
            canvas.save();
            canvas.rotate(symbolDetail.getAngle(), location.x, location.y);
            drawable.draw(canvas);
            canvas.restore();
        } else {
            drawable.draw(canvas);
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

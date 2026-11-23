package org.hwyl.sexytopo.control.sketch;

import static org.hwyl.sexytopo.control.sketch.ScreenUnits.dpToPixels;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import androidx.core.content.ContextCompat;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.util.SketchPreferences;
import org.hwyl.sexytopo.control.util.TextTools;
import org.hwyl.sexytopo.model.geometry.Projection2D;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.SketchTool;

/**
 * Draws what sits on the screen rather than in the cave: the grid behind the survey, and in front
 * of it the legend (survey name, length, depth and a scale bar) and debugging info.
 */
class OverlayRenderer {

    private static final int BOX_SIZE = 10; // every grid box is 10 units square
    private static final int LEGEND_SIZE = 18;
    private static final int LEGEND_TICK_SIZE_DP = 5;

    private final Viewport viewport;
    private final SketchState state;

    private final Paint legendPaint = new Paint();
    private final Paint gridPaint = new Paint();

    private float legendTickSizePx;

    private float surveyLength = 0;
    private float surveyHeight = 0;

    OverlayRenderer(Viewport viewport, SketchState state) {
        this.viewport = viewport;
        this.state = state;
    }

    /** Set the paints up from the theme and current preferences. */
    void initialise(Context context) {

        SketchPaints.applyAntiAliasPreference(legendPaint);

        int gridColour = ContextCompat.getColor(context, R.color.grid);
        gridPaint.setColor(gridColour);

        SketchPaints.setUpLegendPaint(context, legendPaint);

        legendTickSizePx = dpToPixels(LEGEND_TICK_SIZE_DP);
    }

    /** The survey's length and vertical range, as shown in the legend. */
    void setStats(float surveyLength, float surveyHeight) {
        this.surveyLength = surveyLength;
        this.surveyHeight = surveyHeight;
    }

    void drawGrid(Canvas canvas, int width, int height) {
        if (!SketchPreferences.Toggle.SHOW_GRID.isOn()) {
            return;
        }

        float viewOffsetX = viewport.getOffsetX();
        float viewOffsetY = viewport.getOffsetY();
        float viewScale = viewport.getScale();

        int tickSizeInMetres = getMinorGridBoxSize();
        int numberTicksJustBeforeViewpointOffsetX = (int) (viewOffsetX / tickSizeInMetres);

        for (int n = numberTicksJustBeforeViewpointOffsetX; true; n++) {
            float xSurvey = n * tickSizeInMetres;
            int xView = (int) ((xSurvey - viewOffsetX) * viewScale);
            gridPaint.setStrokeWidth(n % BOX_SIZE == 0 ? 3 : 1);
            canvas.drawLine(xView, 0, xView, height, gridPaint);
            if (xView >= width) {
                break;
            }
        }

        int numberTicksJustBeforeViewpointOffsetY = (int) (viewOffsetY / tickSizeInMetres);

        for (int n = numberTicksJustBeforeViewpointOffsetY; true; n++) {
            float ySurvey = n * tickSizeInMetres;
            int yView = (int) ((ySurvey - viewOffsetY) * viewScale);
            gridPaint.setStrokeWidth(n % BOX_SIZE == 0 ? 3 : 1);
            canvas.drawLine(0, yView, width, yView, gridPaint);
            if (yView >= height) {
                break;
            }
        }
    }

    /** The minor grid spacing in metres, coarser as the view zooms out. */
    private int getMinorGridBoxSize() {

        if (viewport.getScale() > 15) {
            return 1;
        } else if (viewport.getScale() > 2) {
            return 10;
        } else {
            return 100;
        }
    }

    void drawLegend(Canvas canvas, int height) {

        String surveyLabel =
                state.getSurvey().getName()
                        + " L"
                        + TextTools.formatTo0dpWithComma(surveyLength)
                        + " V"
                        + TextTools.formatTo0dpWithComma(surveyHeight);

        float legendSize = legendPaint.getTextSize();
        float offsetX = legendSize * 1.25f;
        float offsetY = legendSize * 1.25f;
        float y = height - offsetY;
        float x = offsetX;
        canvas.drawText(surveyLabel, x, y, legendPaint);

        int minorGridSize = getMinorGridBoxSize();
        float scaleWidth = viewport.getScale() * minorGridSize;
        float scaleOffsetY = offsetY * 2;
        float scaleY = height - scaleOffsetY;
        legendPaint.setStyle(Paint.Style.STROKE);
        Path scalePath = new Path();
        scalePath.moveTo(x, scaleY - legendTickSizePx);
        scalePath.lineTo(x, scaleY);
        scalePath.lineTo(x + scaleWidth, scaleY);
        scalePath.lineTo(x + scaleWidth, scaleY - legendTickSizePx);
        canvas.drawPath(scalePath, legendPaint);
        legendPaint.setStyle(Paint.Style.FILL);
        String scaleLabel = minorGridSize + "m";
        canvas.drawText(scaleLabel, x + scaleWidth + 0.3f * legendSize, scaleY, legendPaint);
    }

    /**
     * Draw debugging info: how long the last frame took to draw, and a smoothed average, then the
     * zoom figures. The times cover building the frame on the UI thread, not the GPU rendering it.
     */
    void drawDebuggingInfo(Canvas canvas, int width, long lastDrawNanos, long averageDrawNanos) {
        float offsetX = width * 0.03f;
        float offsetY = LEGEND_SIZE * 2;

        String x = TextTools.formatTo2dp(viewport.getOffsetX());
        String y = TextTools.formatTo2dp(viewport.getOffsetY());
        String scale = TextTools.formatTo2dp(viewport.getScale());
        SketchTool tool = state.getSketchTool();
        Colour colour = state.getColour();
        Projection2D projection = state.getProjectionType();

        String[] lines = {
            "project=" + projection,
            "tool=" + tool + " colour=" + colour,
            "x=" + x + " y=" + y + " x" + scale,
            "draw="
                    + TextTools.formatTo2dp(lastDrawNanos / 1e6)
                    + "ms avg="
                    + TextTools.formatTo2dp(averageDrawNanos / 1e6)
                    + "ms"
        };

        // drawText doesn't break lines at a newline, so draw each line separately
        float lineHeight = legendPaint.descent() - legendPaint.ascent();
        float lineY = offsetY;
        for (String line : lines) {
            canvas.drawText(line, offsetX, lineY, legendPaint);
            lineY += lineHeight;
        }
    }
}

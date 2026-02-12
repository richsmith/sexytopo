package org.hwyl.sexytopo.control.graph;

import android.graphics.Canvas;
import android.graphics.Paint;

import org.hwyl.sexytopo.model.graph.Coord2D;

/**
 * Shared utility for drawing grids on sketch views.
 * Used by both GraphView and CrossSectionView.
 */
public class GridDrawer {

    private static final int BOX_SIZE = 10; // Major grid line every 10 minor lines

    /**
     * Draws a grid with major and minor lines.
     *
     * @param canvas          The canvas to draw on
     * @param gridPaint       The paint to use for grid lines (color will be used, stroke width will be modified)
     * @param viewpointOffset The current viewpoint offset in survey coordinates
     * @param surveyToViewScale The scale factor from survey to view coordinates
     * @param viewWidth       The width of the view in pixels
     * @param viewHeight      The height of the view in pixels
     */
    public static void drawGrid(Canvas canvas, Paint gridPaint,
                                 Coord2D viewpointOffset, float surveyToViewScale,
                                 int viewWidth, int viewHeight) {

        int tickSizeInMetres = getMinorGridBoxSize(surveyToViewScale);
        
        // Vertical lines
        int numberTicksJustBeforeViewpointOffsetX = (int)(viewpointOffset.x / tickSizeInMetres);
        for (int n = numberTicksJustBeforeViewpointOffsetX; true; n++) {
            float xSurvey = n * tickSizeInMetres;
            int xView = (int)((xSurvey - viewpointOffset.x) * surveyToViewScale);
            gridPaint.setStrokeWidth(n % BOX_SIZE == 0 ? 3 : 1);
            canvas.drawLine(xView, 0, xView, viewHeight, gridPaint);
            if (xView >= viewWidth) {
                break;
            }
        }

        // Horizontal lines
        int numberTicksJustBeforeViewpointOffsetY = (int)(viewpointOffset.y / tickSizeInMetres);
        for (int n = numberTicksJustBeforeViewpointOffsetY; true; n++) {
            float ySurvey = n * tickSizeInMetres;
            int yView = (int)((ySurvey - viewpointOffset.y) * surveyToViewScale);
            gridPaint.setStrokeWidth(n % BOX_SIZE == 0 ? 3 : 1);
            canvas.drawLine(0, yView, viewWidth, yView, gridPaint);
            if (yView >= viewHeight) {
                break;
            }
        }
    }

    /**
     * Calculate the minor grid box size based on zoom level.
     * Returns 1m, 10m, or 100m depending on scale.
     */
    public static int getMinorGridBoxSize(float surveyToViewScale) {
        if (surveyToViewScale > 15) {
            return 1;
        } else if (surveyToViewScale > 2) {
            return 10;
        } else {
            return 100;
        }
    }
}

package org.hwyl.sexytopo.control.sketch;

import static org.hwyl.sexytopo.control.sketch.ScreenUnits.dpToPixels;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.geometry.Coord2D;

/** Simple shapes drawn in view coordinates, shared by the different parts of a sketch view. */
final class CanvasShapes {

    private static final int DASHED_LINE_INTERVAL_DP = 4;

    private CanvasShapes() {}

    /** Draws a dashed line between two points on the view. */
    static void drawDashedLine(Canvas canvas, Coord2D start, Coord2D end, Paint paint) {
        float dashLength = dpToPixels(DASHED_LINE_INTERVAL_DP);

        // this switcheroo is so we start from the end of the line and draw backwards
        // (we prefer the end of the line to line up with a dash rather than the start)
        Coord2D swap = end;
        end = start;
        start = swap;

        float lineLength = Space2DUtils.getDistance(start, end);
        int dashes = (int) (lineLength / dashLength / 2f);

        Coord2D direction = end.minus(start).normalise();
        Coord2D dashStep = direction.scale(dashLength);

        float stepX = dashStep.x;
        float stepY = dashStep.y;

        float[] lines = new float[dashes * 4]; // xy coords for start and end of dashes == 4

        int lineIndex = 0;
        float startX, startY, endX, endY;
        float previousX = 0, previousY = 0;

        for (int dashIndex = 0; dashIndex < dashes; dashIndex++) {

            if (dashIndex == 0) {
                startX = start.x;
                startY = start.y;
            } else {
                startX = previousX + stepX;
                startY = previousY + stepY;
            }

            endX = startX + stepX;
            endY = startY + stepY;

            lines[lineIndex++] = startX;
            lines[lineIndex++] = startY;
            lines[lineIndex++] = endX;
            lines[lineIndex++] = endY;

            previousX = endX;
            previousY = endY;
        }

        canvas.drawLines(lines, paint);
    }

    /**
     * Draws the four corners of a square centred on a point on the view, to highlight what's in it,
     * as for the active station.
     */
    static void drawHighlightBrackets(Canvas canvas, float x, float y, float size, Paint paint) {
        float half = size / 2;
        drawHighlightBrackets(canvas, new RectF(x - half, y - half, x + half, y + half), paint);
    }

    /**
     * Draws the four corners of a rectangle on the view, to highlight what's in it. Each corner
     * reaches a third of the way along the shorter side.
     */
    static void drawHighlightBrackets(Canvas canvas, RectF box, Paint paint) {

        float arm = Math.min(box.width(), box.height()) / 3f;
        float topY = box.top;
        float bottomY = box.bottom;
        float leftX = box.left;
        float rightX = box.right;

        float innerLeft = leftX + arm;
        float innerRight = rightX - arm;
        float innerTop = topY + arm;
        float innerBottom = bottomY - arm;

        Path corners = new Path();

        // top-left corner
        corners.moveTo(innerLeft, topY);
        corners.lineTo(leftX, topY);
        corners.lineTo(leftX, innerTop);

        // top-right corner
        corners.moveTo(innerRight, topY);
        corners.lineTo(rightX, topY);
        corners.lineTo(rightX, innerTop);

        // bottom-left corner
        corners.moveTo(leftX, innerBottom);
        corners.lineTo(leftX, bottomY);
        corners.lineTo(innerLeft, bottomY);

        // bottom-right corner
        corners.moveTo(rightX, innerBottom);
        corners.lineTo(rightX, bottomY);
        corners.lineTo(innerRight, bottomY);

        canvas.drawPath(corners, paint);
    }
}

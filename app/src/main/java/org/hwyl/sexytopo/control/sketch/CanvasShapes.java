package org.hwyl.sexytopo.control.sketch;

import android.graphics.Canvas;
import android.graphics.Paint;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.geometry.Coord2D;

/** Simple shapes drawn in view coordinates, shared by the different parts of a sketch view. */
final class CanvasShapes {

    private CanvasShapes() {}

    static void drawStationCross(
            Canvas canvas, Paint paint, float x, float y, int crossDiameter, int alpha) {
        paint.setAlpha(alpha);
        float halfCross = crossDiameter / 2f;
        canvas.drawLine(x, y - halfCross, x, y + halfCross, paint);
        canvas.drawLine(x - halfCross, y, x + halfCross, y, paint);
    }

    static void drawDashedLine(
            Canvas canvas, Coord2D start, Coord2D end, float dashLength, Paint paint) {

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
}

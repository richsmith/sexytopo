package org.hwyl.sexytopo.control.sketch;

import static org.hwyl.sexytopo.control.sketch.SketchPaints.FADED_ALPHA;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import androidx.core.content.ContextCompat;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.util.GeneralPreferences;

/**
 * The hot corners: touching one pans the view, whatever tool is selected. Three of the four corners
 * are drawn as faint squares, highlighted while a pan from one is in progress.
 */
class HotCorners {

    // how far in from the edges a corner reaches, as a fraction of the view's shorter side
    private static final float DISTANCE_PROPORTION = 0.05f;

    private final Paint paint = new Paint();
    private int colour;
    private int activeColour;

    private boolean isEnabled = true;

    /** Set up from the theme and current preferences. */
    void initialise(Context context) {
        isEnabled = GeneralPreferences.isHotCornersModeActive();
        colour = ContextCompat.getColor(context, R.color.hotCorner);
        activeColour = ContextCompat.getColor(context, R.color.hotCornerActive);
        paint.setColor(colour);
        paint.setAlpha(FADED_ALPHA);
    }

    /** Whether hot corners are turned on and the event lies in one of them. */
    boolean isHit(MotionEvent event, int width, int height) {
        if (!isEnabled) {
            return false;
        }

        float x = event.getX();
        float y = event.getY();

        float cornerDelta = Math.min(height, width) * DISTANCE_PROPORTION;

        boolean hitLeftEdge = x < cornerDelta;
        boolean hitRightEdge = x > width - cornerDelta;
        boolean hitTopEdge = y < cornerDelta;
        boolean hitBottomEdge = y > height - cornerDelta;

        return (hitLeftEdge && (hitBottomEdge || hitTopEdge))
                || (hitRightEdge && (hitBottomEdge || hitTopEdge));
    }

    /** Draw the corners, highlighted if a pan from one is in progress. */
    void draw(Canvas canvas, int width, int height, boolean isPanning) {

        if (!isEnabled) {
            return;
        }

        paint.setColor(isPanning ? activeColour : colour);
        paint.setAlpha(FADED_ALPHA);

        final int side = (int) (DISTANCE_PROPORTION * Math.min(width, height));
        canvas.drawRect(new Rect(0, 0, side, side), paint);
        canvas.drawRect(new Rect(width - side, 0, width, side), paint);
        canvas.drawRect(new Rect(width - side, height - side, width, height), paint);
    }
}

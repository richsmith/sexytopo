package org.hwyl.sexytopo.control.sketch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * The compass, turned to show which way the device is facing, drawn in the bottom-left corner of
 * the sketch above the legend. It's a view of its own laid over the sketch, so turning it redraws
 * only the compass and not the whole survey beneath. Touches pass through it to the sketch.
 */
public class CompassView extends View {

    // The sensor reports many times a second, mostly with changes too small to see, so the
    // compass is only redrawn once the device has turned at least this far
    private static final float MIN_VISIBLE_TURN_DEGREES = 1;

    private final Paint paint = new Paint();

    // reused for the arrow, to avoid allocating on every frame
    private final Path arrowPath = new Path();

    // which way the compass is drawn as facing
    private float azimuthDegrees = 0;

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** Set the paint up from the theme and current preferences. */
    public void initialise() {
        SketchPaints.applyAntiAliasPreference(paint);
        SketchPaints.setUpLegendPaint(getContext(), paint);
        invalidate();
    }

    /** Which way the device is facing, in degrees clockwise from north. */
    public void setAzimuth(float degrees) {
        float turn = Math.abs(degrees - azimuthDegrees) % 360;
        if (Math.min(turn, 360 - turn) < MIN_VISIBLE_TURN_DEGREES) {
            return;
        }
        azimuthDegrees = degrees;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float textSize = paint.getTextSize();
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float textHeight = metrics.descent - metrics.ascent;
        float offsetX = textSize * 1.25f; // matches legend x
        float arrowLength = textSize * 2.5f;
        float arrowHeadSize = textSize * 0.6f;
        float cx = offsetX + arrowLength / 2f + textSize;
        float scaleBarY = getHeight() - textSize * 4f;
        float cy = scaleBarY - arrowLength / 2f - textHeight;

        canvas.save();
        canvas.rotate(-azimuthDegrees, cx, cy);

        float tipY = cy - arrowLength / 2f;
        float tailY = cy + arrowLength / 2f;

        paint.setStyle(Paint.Style.STROKE);
        arrowPath.rewind();
        arrowPath.moveTo(cx - arrowHeadSize, tipY + arrowHeadSize);
        arrowPath.lineTo(cx, tipY);
        arrowPath.lineTo(cx + arrowHeadSize, tipY + arrowHeadSize);
        arrowPath.moveTo(cx, tipY);
        arrowPath.lineTo(cx, tailY);
        canvas.drawPath(arrowPath, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawText("N", cx - textSize * 0.35f, tipY - textSize * 0.2f, paint);

        canvas.restore();
    }
}

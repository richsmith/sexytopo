package org.hwyl.sexytopo.control.threed;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class SurveyView3D extends GLSurfaceView {

    private SurveyRenderer renderer;

    // Touch state
    private float previousX;
    private float previousY;
    private float previousSpacing;

    private static final int NONE = 0;
    private static final int PAN = 1;
    private static final int ROTATE = 2;

    private int touchMode = NONE;

    public SurveyView3D(Context context) {
        super(context);
        init();
    }

    public SurveyView3D(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);
        renderer = new SurveyRenderer();
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public SurveyRenderer getSurveyRenderer() {
        return renderer;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int pointerCount = event.getPointerCount();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                previousX = event.getX();
                previousY = event.getY();
                touchMode = PAN;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (pointerCount == 2) {
                    previousSpacing = getSpacing(event);
                    previousX = getMidpointX(event);
                    previousY = getMidpointY(event);
                    touchMode = ROTATE;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (pointerCount == 1 && touchMode == PAN) {
                    float dx = event.getX() - previousX;
                    float dy = event.getY() - previousY;
                    renderer.panBy(dx, dy);
                    previousX = event.getX();
                    previousY = event.getY();
                    requestRender();
                } else if (pointerCount == 2) {
                    float spacing = getSpacing(event);
                    float midX = getMidpointX(event);
                    float midY = getMidpointY(event);

                    if (previousSpacing > 10f && spacing > 10f) {
                        float spacingDelta = Math.abs(spacing - previousSpacing);
                        float midDelta =
                                (float)
                                        Math.sqrt(
                                                (midX - previousX) * (midX - previousX)
                                                        + (midY - previousY) * (midY - previousY));

                        if (spacingDelta > midDelta) {
                            // Predominantly a pinch — zoom
                            float scaleFactor = previousSpacing / spacing;
                            renderer.zoomBy(scaleFactor);
                        } else {
                            // Predominantly a drag — rotate
                            float dx = midX - previousX;
                            float dy = midY - previousY;
                            renderer.rotateBy(dx, dy);
                        }
                    }

                    previousSpacing = spacing;
                    previousX = midX;
                    previousY = midY;
                    requestRender();
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                touchMode = NONE;
                break;
        }

        return true;
    }

    private float getSpacing(MotionEvent event) {
        float dx = event.getX(0) - event.getX(1);
        float dy = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private float getMidpointX(MotionEvent event) {
        return (event.getX(0) + event.getX(1)) / 2f;
    }

    private float getMidpointY(MotionEvent event) {
        return (event.getY(0) + event.getY(1)) / 2f;
    }
}

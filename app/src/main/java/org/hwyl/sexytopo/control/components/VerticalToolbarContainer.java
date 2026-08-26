package org.hwyl.sexytopo.control.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Container that displays its single child rotated 90 degrees so that a normally horizontal view
 * (e.g. a MaterialToolbar) appears as a vertical bar.
 *
 * <p>The child is measured as if it were laid out horizontally: the container's height is passed to
 * the child as its width constraint, and the container's width is passed to the child as its height
 * constraint. The child is then rotated 90 degrees counter-clockwise and translated so that its
 * original right edge lands at the top of the container. This means the last items in the child
 * (e.g. the toolbar's menu action buttons and overflow icon) end up at the top of the vertical bar.
 */
public class VerticalToolbarContainer extends FrameLayout {

    public VerticalToolbarContainer(Context context) {
        super(context);
    }

    public VerticalToolbarContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VerticalToolbarContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();
        int containerWidth = MeasureSpec.getSize(widthMeasureSpec);
        int containerHeight = MeasureSpec.getSize(heightMeasureSpec);

        // Measure each child with width/height constraints swapped so the child
        // lays itself out as if it had the container's height as its width.
        int childWidthSpec =
                MeasureSpec.makeMeasureSpec(
                        containerHeight, MeasureSpec.getMode(heightMeasureSpec));
        int childHeightSpec =
                MeasureSpec.makeMeasureSpec(containerWidth, MeasureSpec.getMode(widthMeasureSpec));

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                child.measure(childWidthSpec, childHeightSpec);
            }
        }

        setMeasuredDimension(
                resolveSize(containerWidth, widthMeasureSpec),
                resolveSize(containerHeight, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            // Lay the child out at (0, 0) using its measured (horizontal) size.
            child.layout(0, 0, childWidth, childHeight);

            // Rotate the child 90 degrees counter-clockwise about its
            // top-left corner, then translate it down by childWidth so the
            // rotated bounding box lines up with the container's top-left.
            // After this, the child's original right edge (e.g. the toolbar's
            // menu buttons and overflow icon) sits along the top of the
            // container, and its original left edge sits along the bottom.
            child.setPivotX(0f);
            child.setPivotY(0f);
            child.setRotation(-90f);
            child.setTranslationX(0f);
            child.setTranslationY(childWidth);
        }
    }
}

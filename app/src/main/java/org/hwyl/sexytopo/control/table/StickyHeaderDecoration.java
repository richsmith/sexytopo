package org.hwyl.sexytopo.control.table;

import android.graphics.Canvas;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

/**
 * ItemDecoration for drawing a sticky header above the RecyclerView content.
 * The header stays fixed at the top while scrolling, accounting for system insets (action bar, etc).
 */
public class StickyHeaderDecoration extends RecyclerView.ItemDecoration {

    private final View headerView;

    public StickyHeaderDecoration(View headerView) {
        this.headerView = headerView;
    }

    @Override
    public void onDrawOver(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.onDrawOver(canvas, parent, state);

        // Measure header if needed
        measureHeaderView(parent);

        // Get system insets (action bar, status bar, etc.)
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(parent);
        float topInset = 0;
        if (insets != null) {
            Insets systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            topInset = systemInsets.top;
        }

        // Save canvas state and translate to below system bars
        canvas.save();
        canvas.translate(0, topInset);

        // Draw the header at the adjusted top position
        headerView.draw(canvas);

        canvas.restore();
    }

    private void measureHeaderView(RecyclerView parent) {
        if (headerView.getMeasuredWidth() == 0) {
            int widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            headerView.measure(widthSpec, heightSpec);
            headerView.layout(0, 0, headerView.getMeasuredWidth(), headerView.getMeasuredHeight());
        }
    }
}

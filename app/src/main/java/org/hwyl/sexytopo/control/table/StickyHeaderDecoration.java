package org.hwyl.sexytopo.control.table;

import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * ItemDecoration for drawing a sticky header above the RecyclerView content.
 * The header stays fixed at the top while scrolling.
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
        
        // Save canvas state and translate to top of RecyclerView
        canvas.save();
        canvas.translate(0, 0);  // Keep at top, don't translate
        
        // Draw the header at the top
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

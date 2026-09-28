package net.golbarg.skillassessment.ui.widget;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/** Horizontal breathing room between cards when a list is shown as a multi-column grid. */
public final class GridGapDecoration extends RecyclerView.ItemDecoration {
    private final int halfGap;

    public GridGapDecoration(int halfGap) {
        this.halfGap = halfGap;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        GridLayoutManager.LayoutParams lp = (GridLayoutManager.LayoutParams) view.getLayoutParams();
        if (lp.getSpanSize() > 1) return;
        outRect.left = halfGap;
        outRect.right = halfGap;
    }
}

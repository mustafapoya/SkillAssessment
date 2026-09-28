package net.golbarg.skillassessment.ui.widget;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/** Hosts a single, pre-inflated view (e.g. a screen header) as an item of a RecyclerView. */
public final class StaticViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final View view;
    private final int viewType;

    public StaticViewAdapter(View view, int viewType) {
        this.view = view;
        this.viewType = viewType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        if (view.getParent() instanceof ViewGroup) ((ViewGroup) view.getParent()).removeView(view);
        return new RecyclerView.ViewHolder(view) { };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        return viewType;
    }
}

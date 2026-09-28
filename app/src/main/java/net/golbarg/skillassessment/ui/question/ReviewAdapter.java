package net.golbarg.skillassessment.ui.question;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.golbarg.skillassessment.databinding.ItemQuestionCardBinding;
import net.golbarg.skillassessment.models.ResultItem;
import net.golbarg.skillassessment.ui.widget.QuestionCardBinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.Holder> {
    private final List<ResultItem> items = new ArrayList<>();
    private Map<Integer, String> slugs = new HashMap<>();

    /** @param topicSlugs slug per topic id, so mixed tests render each question with its own topic. */
    public void setItems(List<ResultItem> newItems, Map<Integer, String> topicSlugs) {
        items.clear();
        items.addAll(newItems);
        slugs = topicSlugs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemQuestionCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ResultItem item = items.get(position);
        String slug = slugs.get(item.getQuestion().getCategoryId());
        QuestionCardBinder.bindReview(holder.binding, item, position, slug == null ? "" : slug);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemQuestionCardBinding binding;

        Holder(ItemQuestionCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

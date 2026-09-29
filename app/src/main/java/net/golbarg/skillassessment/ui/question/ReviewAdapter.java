package net.golbarg.skillassessment.ui.question;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.golbarg.skillassessment.models.ResultItem;
import net.golbarg.skillassessment.ui.widget.QuestionCardBinder;
import net.golbarg.skillassessment.ui.widget.QuestionCardHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The answered questions of a finished test, each with the user's pick and the correct answer. */
public class ReviewAdapter extends RecyclerView.Adapter<QuestionCardHolder> {

    public interface BookmarkListener {
        void onBookmarkToggled(int questionId, boolean bookmarked);
    }

    private final List<ResultItem> items = new ArrayList<>();
    private final List<Integer> originalPositions = new ArrayList<>();
    private Map<Integer, String> slugs = new HashMap<>();
    private final Set<Integer> bookmarkedIds = new HashSet<>();
    private BookmarkListener bookmarkListener;

    public void setBookmarkListener(BookmarkListener listener) {
        this.bookmarkListener = listener;
    }

    public void setBookmarkedIds(Set<Integer> ids) {
        bookmarkedIds.clear();
        if (ids != null) bookmarkedIds.addAll(ids);
        notifyDataSetChanged();
    }

    public void toggleBookmark(int questionId) {
        if (bookmarkedIds.contains(questionId)) {
            bookmarkedIds.remove(questionId);
        } else {
            bookmarkedIds.add(questionId);
        }
        notifyDataSetChanged();
    }

    /** @param topicSlugs slug per topic id, so mixed tests render each question with its own topic. */
    public void setItems(List<ResultItem> newItems, List<Integer> positions, Map<Integer, String> topicSlugs) {
        items.clear();
        items.addAll(newItems);
        originalPositions.clear();
        if (positions != null && positions.size() == newItems.size()) {
            originalPositions.addAll(positions);
        } else {
            for (int i = 0; i < newItems.size(); i++) originalPositions.add(i);
        }
        slugs = topicSlugs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QuestionCardHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return QuestionCardHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestionCardHolder holder, int position) {
        ResultItem item = items.get(position);
        int originalPos = position < originalPositions.size() ? originalPositions.get(position) : position;
        String slug = slugs.get(item.getQuestion().getCategoryId());
        int qId = item.getQuestion().getId();
        boolean isBookmarked = bookmarkedIds.contains(qId);
        QuestionCardBinder.bindReview(holder.binding, item, originalPos, slug == null ? "" : slug, isBookmarked, v -> {
            boolean nowBookmarked = !bookmarkedIds.contains(qId);
            toggleBookmark(qId);
            if (bookmarkListener != null) bookmarkListener.onBookmarkToggled(qId, nowBookmarked);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}

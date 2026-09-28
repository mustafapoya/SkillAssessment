package net.golbarg.skillassessment.ui.bookmark;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.ads.AdManager;
import net.golbarg.skillassessment.databinding.FragmentBookmarkBinding;
import net.golbarg.skillassessment.databinding.ItemQuestionCardBinding;
import net.golbarg.skillassessment.db.QuizRepository;
import net.golbarg.skillassessment.models.Bookmark;
import net.golbarg.skillassessment.ui.widget.QuestionCardBinder;
import net.golbarg.skillassessment.util.Async;
import net.golbarg.skillassessment.util.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class BookmarkFragment extends Fragment {
    private FragmentBookmarkBinding binding;
    private QuizRepository repository;
    private final List<Bookmark> bookmarks = new ArrayList<>();
    private final Adapter adapter = new Adapter();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBookmarkBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = QuizRepository.get(requireContext());
        UiUtils.applySystemBarPadding(binding.root, true, false);
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);
        binding.empty.imgIcon.setImageResource(R.drawable.ic_bookmark_border);
        binding.empty.txtTitle.setText(R.string.saved_empty_title);
        binding.empty.txtDescription.setText(R.string.saved_empty_desc);
        AdManager.loadBanner(getViewLifecycleOwner(), binding.adContainer, AdManager.Banner.BOOKMARK);
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        Async.run(getViewLifecycleOwner(), repository::getBookmarks, result -> {
            if (binding == null) return;
            binding.progressLoading.setVisibility(View.GONE);
            bookmarks.clear();
            if (result != null) bookmarks.addAll(result);
            adapter.notifyDataSetChanged();
            updateEmptyState();
        });
    }

    private void updateEmptyState() {
        if (binding == null) return;
        boolean empty = bookmarks.isEmpty();
        binding.empty.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.list.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.txtCount.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.txtCount.setText(getResources().getQuantityString(R.plurals.question_count, bookmarks.size(), bookmarks.size()));
    }

    private void remove(int position) {
        if (position < 0 || position >= bookmarks.size()) return;
        Bookmark removed = bookmarks.remove(position);
        adapter.notifyItemRemoved(position);
        updateEmptyState();
        int questionId = removed.getQuestion().getId();
        Async.io(() -> repository.setBookmarked(questionId, false));
        UiUtils.snackbar(binding.getRoot(), R.string.bookmark_deleted, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, v -> {
                    int index = Math.min(position, bookmarks.size());
                    bookmarks.add(index, removed);
                    adapter.notifyItemInserted(index);
                    updateEmptyState();
                    Async.io(() -> repository.setBookmarked(questionId, true));
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private class Adapter extends RecyclerView.Adapter<Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemQuestionCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Bookmark bookmark = bookmarks.get(position);
            QuestionCardBinder.bindSaved(holder.binding, bookmark.getQuestion(), bookmark.getCategory().getDisplayName(),
                    bookmark.getCategory().getSlug(), v -> remove(holder.getBindingAdapterPosition()));
        }

        @Override
        public int getItemCount() {
            return bookmarks.size();
        }
    }

    private static class Holder extends RecyclerView.ViewHolder {
        final ItemQuestionCardBinding binding;

        Holder(ItemQuestionCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

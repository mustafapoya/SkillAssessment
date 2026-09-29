package net.golbarg.skillassessment.ui.bookmark;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import net.golbarg.skillassessment.db.QuizRepository;
import net.golbarg.skillassessment.models.Bookmark;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.ui.home.TestSetupSheet;
import net.golbarg.skillassessment.ui.widget.QuestionCardBinder;
import net.golbarg.skillassessment.ui.widget.QuestionCardHolder;
import net.golbarg.skillassessment.util.Async;
import net.golbarg.skillassessment.util.UiUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The saved-questions tab: search, removal with undo, and a practice test of every saved question. */
public class BookmarkFragment extends Fragment {
    private FragmentBookmarkBinding binding;
    private QuizRepository repository;
    private final List<Bookmark> allBookmarks = new ArrayList<>();
    private final List<Bookmark> displayedBookmarks = new ArrayList<>();
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

        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) { applySearch(); }
        });

        binding.btnPracticeSaved.setOnClickListener(v -> startPractice());
        binding.cardPracticeSaved.setOnClickListener(v -> startPractice());

        AdManager.loadBanner(getViewLifecycleOwner(), binding.adContainer, AdManager.Banner.BOOKMARK);
    }

    private void startPractice() {
        if (allBookmarks.isEmpty()) return;
        Category bookmarkedCategory = Category.pseudo(Category.BOOKMARKED, allBookmarks.size());
        TestSetupSheet.newInstance(bookmarkedCategory).show(getChildFragmentManager(), TestSetupSheet.TAG);
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
            allBookmarks.clear();
            if (result != null) allBookmarks.addAll(result);
            applySearch();
        });
    }

    private void applySearch() {
        if (binding == null) return;
        String query = binding.editSearch.getText() == null ? "" : binding.editSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        displayedBookmarks.clear();
        for (Bookmark b : allBookmarks) {
            if (query.isEmpty()) {
                displayedBookmarks.add(b);
            } else {
                String qText = b.getQuestion().getTitle().toLowerCase(Locale.ROOT);
                String catText = b.getCategory().getDisplayName().toLowerCase(Locale.ROOT);
                if (qText.contains(query) || catText.contains(query)) {
                    displayedBookmarks.add(b);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState(query);
    }

    private void updateEmptyState(String query) {
        if (binding == null) return;
        boolean hasTotalBookmarks = !allBookmarks.isEmpty();
        boolean hasFilteredBookmarks = !displayedBookmarks.isEmpty();

        binding.cardPracticeSaved.setVisibility(hasTotalBookmarks ? View.VISIBLE : View.GONE);
        binding.layoutSearch.setVisibility(hasTotalBookmarks ? View.VISIBLE : View.GONE);
        binding.txtCount.setVisibility(hasTotalBookmarks ? View.VISIBLE : View.GONE);

        if (hasTotalBookmarks) {
            binding.txtCount.setText(getResources().getQuantityString(R.plurals.question_count, allBookmarks.size(), allBookmarks.size()));
            binding.txtPracticeSavedDesc.setText(getString(R.string.practice_saved_desc, allBookmarks.size()));
        }

        if (!hasTotalBookmarks) {
            binding.empty.getRoot().setVisibility(View.VISIBLE);
            binding.empty.txtTitle.setText(R.string.saved_empty_title);
            binding.empty.txtDescription.setText(R.string.saved_empty_desc);
            binding.list.setVisibility(View.GONE);
        } else if (!hasFilteredBookmarks) {
            binding.empty.getRoot().setVisibility(View.VISIBLE);
            binding.empty.txtTitle.setText(R.string.no_topics_in_filter);
            binding.empty.txtDescription.setText(getString(R.string.no_saved_match, query));
            binding.list.setVisibility(View.GONE);
        } else {
            binding.empty.getRoot().setVisibility(View.GONE);
            binding.list.setVisibility(View.VISIBLE);
        }
    }

    private void remove(int position) {
        if (position < 0 || position >= displayedBookmarks.size()) return;
        Bookmark removed = displayedBookmarks.remove(position);
        int originalIndex = allBookmarks.indexOf(removed);
        allBookmarks.remove(removed);
        adapter.notifyItemRemoved(position);
        String query = binding.editSearch.getText() == null ? "" : binding.editSearch.getText().toString().trim();
        updateEmptyState(query);
        int questionId = removed.getQuestion().getId();
        Async.io(() -> repository.setBookmarked(questionId, false));
        UiUtils.snackbar(binding.getRoot(), R.string.bookmark_deleted, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, v -> {
                    allBookmarks.add(Math.min(originalIndex, allBookmarks.size()), removed);
                    applySearch();
                    Async.io(() -> repository.setBookmarked(questionId, true));
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private class Adapter extends RecyclerView.Adapter<QuestionCardHolder> {
        @NonNull
        @Override
        public QuestionCardHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return QuestionCardHolder.create(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull QuestionCardHolder holder, int position) {
            Bookmark bookmark = displayedBookmarks.get(position);
            QuestionCardBinder.bindSaved(holder.binding, bookmark.getQuestion(), bookmark.getCategory().getDisplayName(),
                    bookmark.getCategory().getSlug(), v -> remove(holder.getBindingAdapterPosition()));
        }

        @Override
        public int getItemCount() {
            return displayedBookmarks.size();
        }
    }
}

package net.golbarg.skillassessment.ui.search;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.databinding.ActivitySearchBinding;
import net.golbarg.skillassessment.db.QuizRepository;
import net.golbarg.skillassessment.models.Question;
import net.golbarg.skillassessment.ui.widget.QuestionCardBinder;
import net.golbarg.skillassessment.ui.widget.QuestionCardHolder;
import net.golbarg.skillassessment.util.Async;
import net.golbarg.skillassessment.util.CategoryNames;
import net.golbarg.skillassessment.util.Feedback;
import net.golbarg.skillassessment.util.UiUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Finds questions by keyword across every unlocked topic; results are study flashcards. */
public class SearchActivity extends AppCompatActivity {
    private static final int MIN_CHARS = 2;
    private static final int MAX_RESULTS = 100;
    private static final long DEBOUNCE_MS = 300;

    private static final class Results {
        String query;
        List<Question> questions;
        Set<Integer> bookmarks;
        Map<Integer, String> slugs;
    }

    private ActivitySearchBinding binding;
    private QuizRepository repository;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Question> results = new ArrayList<>();
    private final Set<Integer> revealed = new HashSet<>();
    private final Set<Integer> bookmarks = new HashSet<>();
    private Map<Integer, String> slugs = new HashMap<>();
    private final Adapter adapter = new Adapter();
    /** Guards against a slow older search overwriting a newer one. */
    private int generation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        UiUtils.enableEdgeToEdge(this);
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applySystemBarPadding(binding.root, true, false);
        UiUtils.applySystemBarPadding(binding.list, false, true);
        repository = QuizRepository.get(this);

        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);
        binding.btnClose.setOnClickListener(v -> finish());
        binding.empty.imgIcon.setImageResource(R.drawable.ic_search);

        binding.editQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(SearchActivity.this::search, DEBOUNCE_MS);
            }
        });
        binding.editQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return false;
            handler.removeCallbacksAndMessages(null);
            search();
            new WindowInsetsControllerCompat(getWindow(), v).hide(WindowInsetsCompat.Type.ime());
            return true;
        });
        binding.editQuery.requestFocus();
        showHint();
    }

    private String query() {
        return binding.editQuery.getText() == null ? "" : binding.editQuery.getText().toString().trim();
    }

    private void showHint() {
        results.clear();
        adapter.notifyDataSetChanged();
        binding.txtSummary.setVisibility(View.GONE);
        binding.empty.getRoot().setVisibility(View.VISIBLE);
        binding.empty.txtTitle.setText(R.string.search_questions);
        binding.empty.txtDescription.setText(R.string.search_min_chars);
    }

    private void search() {
        String query = query();
        int token = ++generation;
        if (query.length() < MIN_CHARS) {
            showHint();
            return;
        }
        Async.run(this, () -> {
            Results r = new Results();
            r.query = query;
            r.questions = repository.searchQuestions(query, MAX_RESULTS);
            r.bookmarks = repository.getBookmarkedQuestionIds();
            r.slugs = repository.getSlugs();
            return r;
        }, r -> {
            if (r == null || token != generation || binding == null) return;
            results.clear();
            results.addAll(r.questions);
            revealed.clear();
            bookmarks.clear();
            bookmarks.addAll(r.bookmarks);
            slugs = r.slugs;
            adapter.notifyDataSetChanged();
            binding.list.scrollToPosition(0);
            boolean empty = results.isEmpty();
            binding.empty.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
            if (empty) {
                binding.empty.txtTitle.setText(R.string.search_questions);
                binding.empty.txtDescription.setText(getString(R.string.search_no_results, r.query));
            }
            binding.txtSummary.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.txtSummary.setText(results.size() >= MAX_RESULTS ? getString(R.string.search_results_capped, MAX_RESULTS)
                    : getResources().getQuantityString(R.plurals.search_results, results.size(), results.size()));
        });
    }

    private String slugOf(Question q) {
        String slug = slugs.get(q.getCategoryId());
        return slug == null ? "" : slug;
    }

    private void toggleReveal(int position) {
        if (position == RecyclerView.NO_POSITION) return;
        int id = results.get(position).getId();
        if (!revealed.remove(id)) revealed.add(id);
        adapter.notifyItemChanged(position);
    }

    private void toggleBookmark(View v, int position) {
        if (position == RecyclerView.NO_POSITION) return;
        int id = results.get(position).getId();
        boolean now = !bookmarks.contains(id);
        if (now) bookmarks.add(id);
        else bookmarks.remove(id);
        Feedback.haptic(v, Feedback.Haptic.LIGHT);
        adapter.notifyItemChanged(position);
        Async.io(() -> repository.setBookmarked(id, now));
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private class Adapter extends RecyclerView.Adapter<QuestionCardHolder> {
        @NonNull
        @Override
        public QuestionCardHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return QuestionCardHolder.create(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull QuestionCardHolder holder, int position) {
            Question q = results.get(position);
            String slug = slugOf(q);
            QuestionCardBinder.bindStudy(holder.binding, q, CategoryNames.displayName(slug), slug,
                    revealed.contains(q.getId()), bookmarks.contains(q.getId()),
                    v -> toggleReveal(holder.getBindingAdapterPosition()),
                    v -> toggleBookmark(v, holder.getBindingAdapterPosition()));
        }

        @Override
        public int getItemCount() {
            return results.size();
        }
    }
}

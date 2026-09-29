package net.golbarg.skillassessment.ui.study;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.databinding.ActivityStudyBinding;
import net.golbarg.skillassessment.db.QuizRepository;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.models.Question;
import net.golbarg.skillassessment.ui.widget.QuestionCardBinder;
import net.golbarg.skillassessment.ui.widget.QuestionCardHolder;
import net.golbarg.skillassessment.util.Async;
import net.golbarg.skillassessment.util.Feedback;
import net.golbarg.skillassessment.util.UiUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Study mode: every question of a topic as a flashcard. Nothing is scored or recorded. */
public class StudyActivity extends AppCompatActivity {
    private static final String EXTRA_CATEGORY = "category_id";
    private static final String STATE_REVEALED = "revealed";

    private static final class Data {
        Category category;
        List<Question> questions;
        Set<Integer> bookmarks;
    }

    private ActivityStudyBinding binding;
    private QuizRepository repository;
    private final List<Question> questions = new ArrayList<>();
    private final Set<Integer> revealed = new HashSet<>();
    private final Set<Integer> bookmarks = new HashSet<>();
    private final Adapter adapter = new Adapter();
    private String slug = "";

    public static Intent intent(Context context, int categoryId) {
        return new Intent(context, StudyActivity.class).putExtra(EXTRA_CATEGORY, categoryId);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        UiUtils.enableEdgeToEdge(this);
        super.onCreate(savedInstanceState);
        binding = ActivityStudyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applySystemBarPadding(binding.root, true, false);
        UiUtils.applySystemBarPadding(binding.list, false, true);
        repository = QuizRepository.get(this);

        if (savedInstanceState != null) {
            int[] ids = savedInstanceState.getIntArray(STATE_REVEALED);
            if (ids != null) for (int id : ids) revealed.add(id);
        }

        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);
        binding.btnClose.setOnClickListener(v -> finish());
        binding.btnRevealAll.setOnClickListener(v -> {
            boolean allShown = revealed.size() == questions.size();
            revealed.clear();
            if (!allShown) for (Question q : questions) revealed.add(q.getId());
            adapter.notifyDataSetChanged();
            bindProgress();
        });

        int categoryId = getIntent().getIntExtra(EXTRA_CATEGORY, -1);
        Async.run(this, () -> {
            Data d = new Data();
            d.category = repository.getCategory(categoryId);
            // Only unlocked topics can be studied, just like they can be tested.
            d.questions = d.category != null && d.category.isUnlocked() ? repository.getQuestions(categoryId) : new ArrayList<>();
            d.bookmarks = repository.getBookmarkedQuestionIds();
            return d;
        }, d -> {
            if (d == null || d.category == null) {
                finish();
                return;
            }
            slug = d.category.getSlug();
            binding.txtTitle.setText(getString(R.string.joined_dot, getString(R.string.study_mode), d.category.getDisplayName()));
            binding.progressLoading.setVisibility(View.GONE);
            questions.addAll(d.questions);
            bookmarks.addAll(d.bookmarks);
            binding.txtEmpty.setVisibility(questions.isEmpty() ? View.VISIBLE : View.GONE);
            binding.btnRevealAll.setVisibility(questions.isEmpty() ? View.INVISIBLE : View.VISIBLE);
            adapter.notifyDataSetChanged();
            bindProgress();
        });
    }

    private void bindProgress() {
        int total = questions.size();
        binding.txtSubtitle.setText(getString(R.string.study_progress, revealed.size(), total));
        binding.progressRevealed.setProgressCompat(total == 0 ? 0 : revealed.size() * 100 / total, true);
        boolean allShown = total > 0 && revealed.size() == total;
        binding.btnRevealAll.setIconResource(allShown ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
        binding.btnRevealAll.setContentDescription(getString(allShown ? R.string.study_hide_all : R.string.study_show_all));
    }

    private void toggleReveal(int position) {
        if (position == RecyclerView.NO_POSITION) return;
        int id = questions.get(position).getId();
        if (!revealed.remove(id)) revealed.add(id);
        adapter.notifyItemChanged(position);
        bindProgress();
    }

    private void toggleBookmark(View v, int position) {
        if (position == RecyclerView.NO_POSITION) return;
        int id = questions.get(position).getId();
        boolean now = !bookmarks.contains(id);
        if (now) bookmarks.add(id);
        else bookmarks.remove(id);
        Feedback.haptic(v, Feedback.Haptic.LIGHT);
        adapter.notifyItemChanged(position);
        Async.write(() -> repository.setBookmarked(id, now));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        int[] ids = new int[revealed.size()];
        int i = 0;
        for (int id : revealed) ids[i++] = id;
        outState.putIntArray(STATE_REVEALED, ids);
    }

    private class Adapter extends RecyclerView.Adapter<QuestionCardHolder> {
        @NonNull
        @Override
        public QuestionCardHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return QuestionCardHolder.create(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull QuestionCardHolder holder, int position) {
            Question q = questions.get(position);
            QuestionCardBinder.bindStudy(holder.binding, q, getString(R.string.question_label, position + 1), slug, revealed.contains(q.getId()), bookmarks.contains(q.getId()),
                    v -> toggleReveal(holder.getBindingAdapterPosition()),
                    v -> toggleBookmark(v, holder.getBindingAdapterPosition()));
        }

        @Override
        public int getItemCount() {
            return questions.size();
        }
    }
}

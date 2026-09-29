package net.golbarg.skillassessment.ui.path;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.databinding.ActivityPathBinding;
import net.golbarg.skillassessment.databinding.ItemTopicProgressBinding;
import net.golbarg.skillassessment.db.QuizRepository;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.models.LearningPath;
import net.golbarg.skillassessment.ui.question.QuestionActivity;
import net.golbarg.skillassessment.util.Async;
import net.golbarg.skillassessment.util.CategoryNames;
import net.golbarg.skillassessment.util.Prefs;
import net.golbarg.skillassessment.util.UiUtils;

import java.util.List;

/** A topic's learning path: levels with their mastery, each started as a practice test. */
public class PathActivity extends AppCompatActivity {
    private static final String EXTRA_CATEGORY = "category_id";

    private static final class Data {
        Category category;
        List<LearningPath.Level> levels;
    }

    private ActivityPathBinding binding;
    private int categoryId;

    public static Intent intent(Context context, int categoryId) {
        return new Intent(context, PathActivity.class).putExtra(EXTRA_CATEGORY, categoryId);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        UiUtils.enableEdgeToEdge(this);
        super.onCreate(savedInstanceState);
        binding = ActivityPathBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applySystemBarPadding(binding.root, true, false);
        UiUtils.applySystemBarPadding(binding.scroll, false, true);
        binding.btnClose.setOnClickListener(v -> finish());
        binding.txtIntro.setText(getString(R.string.path_intro, LearningPath.LEVEL_SIZE, LearningPath.UNLOCK_PERCENT));
        categoryId = getIntent().getIntExtra(EXTRA_CATEGORY, -1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reloaded every time: a finished level test may have opened the next level.
        QuizRepository repository = QuizRepository.get(this);
        Async.run(this, () -> {
            Data d = new Data();
            d.category = repository.getCategory(categoryId);
            d.levels = d.category != null && d.category.isUnlocked() ? repository.getLearningPath(categoryId) : null;
            return d;
        }, d -> {
            if (d == null || d.levels == null || d.levels.isEmpty()) {
                finish();
                return;
            }
            bind(d);
        });
    }

    private void bind(Data d) {
        binding.progressLoading.setVisibility(View.GONE);
        binding.txtTitle.setText(getString(R.string.path_title, d.category.getDisplayName()));
        int complete = 0;
        for (LearningPath.Level level : d.levels) if (level.isComplete()) complete++;
        binding.txtSubtitle.setText(getString(R.string.path_progress, complete, d.levels.size()));
        binding.progressPath.setProgressCompat(complete * 100 / d.levels.size(), true);

        binding.listLevels.removeAllViews();
        for (LearningPath.Level level : d.levels) addRow(d.category, level);
    }

    private void addRow(Category category, LearningPath.Level level) {
        ItemTopicProgressBinding row = ItemTopicProgressBinding.inflate(getLayoutInflater(), binding.listLevels, false);
        int number = level.index + 1;
        CategoryNames.styleBadge(row.txtBadge, category.getSlug(), 12);
        row.txtBadge.setText(String.valueOf(number));
        row.txtTitle.setText(getString(R.string.level_label, number));
        row.progress.setProgressCompat(level.masteryPercent(), false);
        if (level.isComplete()) {
            row.progress.setIndicatorColor(ContextCompat.getColor(this, R.color.correct));
        }

        String subtitle;
        if (!level.open) {
            subtitle = getString(R.string.level_locked, level.index, LearningPath.UNLOCK_PERCENT);
            row.txtPercent.setText("");
            row.txtPercent.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_lock, 0);
            TextViewCompat.setCompoundDrawableTintList(row.txtPercent,
                    ColorStateList.valueOf(UiUtils.color(this, com.google.android.material.R.attr.colorOnSurfaceVariant)));
            row.getRoot().setAlpha(0.6f);
        } else {
            subtitle = getString(level.isComplete() ? R.string.level_complete : R.string.level_progress, level.mastered, level.size());
            row.txtPercent.setText(level.masteryPercent() + "%");
        }
        row.txtSubtitle.setText(subtitle);
        row.getRoot().setContentDescription(row.txtTitle.getText() + ", " + subtitle);
        row.getRoot().setOnClickListener(v -> {
            if (!level.open) {
                Toast.makeText(this, subtitle, Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(QuestionActivity.levelIntent(this, category.getId(), level.index, Prefs.isTimerEnabled(this)));
        });
        binding.listLevels.addView(row.getRoot());
    }
}

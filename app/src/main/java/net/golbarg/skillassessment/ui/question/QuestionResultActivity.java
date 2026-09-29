package net.golbarg.skillassessment.ui.question;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.ads.AdManager;
import net.golbarg.skillassessment.databinding.ActivityQuestionResultBinding;
import net.golbarg.skillassessment.databinding.DialogEditNameBinding;
import net.golbarg.skillassessment.databinding.ItemResultHeaderBinding;
import net.golbarg.skillassessment.databinding.ViewProgressBannerBinding;
import net.golbarg.skillassessment.databinding.ViewStatTileBinding;
import net.golbarg.skillassessment.db.QuizRepository;
import net.golbarg.skillassessment.models.Achievement;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.models.QuestionResult;
import net.golbarg.skillassessment.models.ResultItem;
import net.golbarg.skillassessment.ui.widget.StaticViewAdapter;
import net.golbarg.skillassessment.util.Async;
import net.golbarg.skillassessment.util.Feedback;
import net.golbarg.skillassessment.util.Prefs;
import net.golbarg.skillassessment.util.ProgressTracker;
import net.golbarg.skillassessment.util.ShareCard;
import net.golbarg.skillassessment.util.UiUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuestionResultActivity extends AppCompatActivity {
    private static final String EXTRA_RESULT_ID = "question_result_id";
    private static final String EXTRA_TEST_ARGS = "test_args";
    private static final String EXTRA_BEST_STREAK = "best_streak";
    private static final String EXTRA_DAY_STREAK = "day_streak";
    private static final String EXTRA_BONUS = "bonus_coins";
    private static final String EXTRA_ACHIEVEMENTS = "achievements";
    private static final String EXTRA_GOAL_REACHED = "goal_reached";

    private static final class Data {
        QuestionResult result;
        Category category;
        List<ResultItem> items;
        Map<Integer, String> slugs;
    }

    private ActivityQuestionResultBinding binding;
    private ItemResultHeaderBinding header;
    private final ReviewAdapter reviewAdapter = new ReviewAdapter();

    /** Opens a past result from the history list (no celebration). */
    public static Intent intent(Context context, long resultId, @Nullable Bundle testArgs) {
        return intent(context, resultId, testArgs, -1, null);
    }

    /**
     * @param testArgs   the extras used to start the test, so "Try again" repeats the same setup.
     * @param bestStreak longest run of correct answers, or -1 when not coming straight from a test.
     */
    public static Intent intent(Context context, long resultId, @Nullable Bundle testArgs, int bestStreak,
                                @Nullable ProgressTracker.Outcome outcome) {
        Intent intent = new Intent(context, QuestionResultActivity.class)
                .putExtra(EXTRA_RESULT_ID, resultId)
                .putExtra(EXTRA_BEST_STREAK, bestStreak);
        if (testArgs != null) intent.putExtra(EXTRA_TEST_ARGS, testArgs);
        if (outcome != null) {
            intent.putExtra(EXTRA_DAY_STREAK, outcome.dayStreakIncreased ? outcome.dayStreak : 0);
            intent.putExtra(EXTRA_BONUS, outcome.bonusCoins);
            intent.putExtra(EXTRA_GOAL_REACHED, outcome.goalReached);
            String[] names = new String[outcome.newAchievements.size()];
            for (int i = 0; i < names.length; i++) names[i] = outcome.newAchievements.get(i).name();
            intent.putExtra(EXTRA_ACHIEVEMENTS, names);
        }
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        UiUtils.enableEdgeToEdge(this);
        super.onCreate(savedInstanceState);
        binding = ActivityQuestionResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applySystemBarPadding(binding.root, true, false);
        UiUtils.applySystemBarPadding(binding.bottomBar, false, true);

        binding.list.setLayoutManager(new LinearLayoutManager(this));
        header = ItemResultHeaderBinding.inflate(getLayoutInflater(), binding.list, false);
        binding.list.setAdapter(new ConcatAdapter(new StaticViewAdapter(header.getRoot(), R.layout.item_result_header), reviewAdapter));
        header.getRoot().setVisibility(View.INVISIBLE);

        binding.btnClose.setOnClickListener(v -> finish());
        header.btnDone.setOnClickListener(v -> finish());

        long resultId = getIntent().getLongExtra(EXTRA_RESULT_ID, -1);
        QuizRepository repository = QuizRepository.get(this);
        reviewAdapter.setBookmarkListener((qId, bookmarked) -> Async.io(() -> repository.setBookmarked(qId, bookmarked)));

        Async.run(this, () -> {
            Data data = new Data();
            data.result = repository.getResult(resultId);
            if (data.result == null) return null;
            data.category = repository.getCategory(data.result.getCategoryId());
            data.items = repository.getResultItems(resultId);
            data.slugs = repository.getSlugs();
            return data;
        }, data -> {
            if (data == null || data.category == null) {
                finish();
                return;
            }
            Async.run(this, repository::getBookmarkedQuestionIds, ids -> {
                reviewAdapter.setBookmarkedIds(ids);
                bind(data, savedInstanceState == null);
            });
        });

        AdManager.loadBanner(this, binding.adContainer, AdManager.Banner.RESULT);
    }

    private void bind(Data data, boolean animate) {
        QuestionResult r = data.result;
        int percent = r.getScorePercent();
        binding.txtToolbarTitle.setText(data.category.getDisplayName());
        header.getRoot().setVisibility(View.VISIBLE);
        header.ring.setValues(r.getCorrectAnswer(), r.getWrongAnswer(), r.getNoAnswer(), animate);
        if (animate) {
            ValueAnimator countUp = ValueAnimator.ofInt(0, percent);
            countUp.setDuration(900);
            countUp.setInterpolator(new DecelerateInterpolator());
            countUp.addUpdateListener(a -> header.txtPercent.setText(a.getAnimatedValue() + "%"));
            countUp.start();
        } else {
            header.txtPercent.setText(percent + "%");
        }
        header.txtRatio.setText(r.getCorrectAnswer() + " / " + r.getTotal());
        header.txtHeadline.setText(percent >= 90 ? R.string.result_excellent : percent >= 70 ? R.string.result_good
                : percent >= 50 ? R.string.result_ok : R.string.result_low);
        header.txtSummary.setText(getString(R.string.result_summary, r.getCorrectAnswer(), r.getTotal()));

        int bestStreak = getIntent().getIntExtra(EXTRA_BEST_STREAK, -1);
        header.streakBadge.setVisibility(bestStreak >= 2 ? View.VISIBLE : View.GONE);
        header.txtBestStreak.setText(getString(R.string.best_streak, bestStreak));

        // Celebrate only when arriving straight from a finished test, not when reopening history.
        if (animate && bestStreak >= 0) {
            Feedback.play(Feedback.Sound.COMPLETE);
            if (percent >= 70) binding.confetti.burst(percent >= 90 ? 160 : 90);
        }
        bindExtras(animate);

        binding.btnShare.setVisibility(View.VISIBLE);
        binding.btnShare.setOnClickListener(v -> ShareCard.share(this, r, data.category.getDisplayName(), Math.max(bestStreak, 0)));

        header.btnCertificate.setVisibility(ShareCard.earnsCertificate(r) ? View.VISIBLE : View.GONE);
        header.btnCertificate.setOnClickListener(v -> withName(name -> ShareCard.shareCertificate(this, r, data.category.getDisplayName(), name)));

        boolean hasMistakes = r.getWrongAnswer() + r.getNoAnswer() > 0;
        header.btnPracticeMistakes.setVisibility(hasMistakes ? View.VISIBLE : View.GONE);
        header.btnPracticeMistakes.setOnClickListener(v -> {
            startActivity(QuestionActivity.intent(this, Category.REVIEW, 0, Prefs.isTimerEnabled(this), false));
            finish();
        });

        tile(header.statCorrect, String.valueOf(r.getCorrectAnswer()), R.string.correct, ContextCompat.getColor(this, R.color.correct));
        tile(header.statWrong, String.valueOf(r.getWrongAnswer()), R.string.wrong, ContextCompat.getColor(this, R.color.wrong));
        tile(header.statSkipped, String.valueOf(r.getNoAnswer()), R.string.skipped, UiUtils.color(this, com.google.android.material.R.attr.colorOnSurface));
        tile(header.statTime, r.getDurationMs() > 0 ? UiUtils.formatDuration(r.getDurationMs()) : "—", R.string.time_taken,
                UiUtils.color(this, com.google.android.material.R.attr.colorOnSurface));

        header.btnRetry.setOnClickListener(v -> {
            Bundle args = getIntent().getBundleExtra(EXTRA_TEST_ARGS);
            Intent retry;
            if (args != null) {
                retry = new Intent(this, QuestionActivity.class).putExtras(args);
            } else {
                retry = QuestionActivity.intent(this, data.category.getId(), Prefs.getTestLength(this),
                        Prefs.isTimerEnabled(this), Prefs.isShuffleEnabled(this));
            }
            startActivity(retry);
            finish();
        });

        int totalCount = data.items.size();
        int mistakesCount = r.getWrongAnswer() + r.getNoAnswer();
        int correctCount = r.getCorrectAnswer();

        header.chipReviewAll.setText(getString(R.string.filter_review_all, totalCount));
        header.chipReviewMistakes.setText(getString(R.string.filter_review_mistakes, mistakesCount));
        header.chipReviewCorrect.setText(getString(R.string.filter_review_correct, correctCount));

        header.chipGroupReview.setOnCheckedStateChangeListener((group, checkedIds) -> applyReviewFilter(data));

        header.txtReviewTitle.setVisibility(data.items.isEmpty() ? View.GONE : View.VISIBLE);
        header.chipGroupReview.setVisibility(data.items.isEmpty() ? View.GONE : View.VISIBLE);
        applyReviewFilter(data);
    }

    private void applyReviewFilter(Data data) {
        int checked = header.chipGroupReview.getCheckedChipId();
        List<ResultItem> filtered = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < data.items.size(); i++) {
            ResultItem item = data.items.get(i);
            boolean isCorrect = item.getOutcome() == net.golbarg.skillassessment.models.AnswerResponseType.CORRECT;
            if (checked == R.id.chip_review_mistakes && isCorrect) continue;
            if (checked == R.id.chip_review_correct && !isCorrect) continue;
            filtered.add(item);
            positions.add(i);
        }
        reviewAdapter.setItems(filtered, positions, data.slugs);
    }

    /** Daily bonus, day streak and new achievements, only right after finishing a test. */
    private void bindExtras(boolean animate) {
        Intent intent = getIntent();
        header.extras.removeAllViews();
        int bonus = intent.getIntExtra(EXTRA_BONUS, 0);
        if (bonus > 0) {
            addBanner(R.drawable.ic_today, getString(R.string.today_title), getString(R.string.daily_bonus_earned, bonus));
        }
        if (intent.getBooleanExtra(EXTRA_GOAL_REACHED, false)) {
            addBanner(R.drawable.ic_flag, getString(R.string.daily_goal), getString(R.string.goal_reached_banner));
        }
        int dayStreak = intent.getIntExtra(EXTRA_DAY_STREAK, 0);
        if (dayStreak >= 2) {
            addBanner(R.drawable.ic_flame_mono, getString(R.string.today_title), getResources().getQuantityString(R.plurals.day_streak, dayStreak, dayStreak));
        }
        String[] achievements = intent.getStringArrayExtra(EXTRA_ACHIEVEMENTS);
        if (achievements != null) {
            for (String name : achievements) {
                try {
                    Achievement a = Achievement.valueOf(name);
                    addBanner(a.icon, getString(R.string.achievement_unlocked), getString(a.title));
                } catch (IllegalArgumentException ignored) {
                    // Unknown id from a newer version: nothing to show.
                }
            }
        }
        boolean any = header.extras.getChildCount() > 0;
        header.extras.setVisibility(any ? View.VISIBLE : View.GONE);
        if (any && animate) {
            if (achievements != null && achievements.length > 0) Feedback.play(Feedback.Sound.UNLOCK);
            for (int i = 0; i < header.extras.getChildCount(); i++) {
                View child = header.extras.getChildAt(i);
                child.setAlpha(0f);
                child.setTranslationY(UiUtils.dp(this, 12));
                child.animate().alpha(1f).translationY(0).setStartDelay(700L + i * 120L).setDuration(300).start();
            }
        }
    }

    private interface NameCallback {
        void onName(String name);
    }

    /** Certificates need a name: use the profile name, or ask for one once and remember it. */
    private void withName(NameCallback callback) {
        String saved = Prefs.getUserName(this);
        if (!saved.isEmpty()) {
            callback.onName(saved);
            return;
        }
        DialogEditNameBinding dialog = DialogEditNameBinding.inflate(getLayoutInflater());
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.certificate_name_prompt)
                .setView(dialog.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.get_certificate, (d, w) -> {
                    String name = dialog.editName.getText() == null ? "" : dialog.editName.getText().toString().trim();
                    if (name.isEmpty()) return;
                    Prefs.setUserName(this, name);
                    callback.onName(name);
                })
                .show();
        dialog.editName.requestFocus();
    }

    private void addBanner(int icon, String overline, String title) {
        ViewProgressBannerBinding banner = ViewProgressBannerBinding.inflate(getLayoutInflater(), header.extras, false);
        banner.imgIcon.setImageResource(icon);
        banner.txtOverline.setText(overline);
        banner.txtTitle.setText(title);
        header.extras.addView(banner.getRoot());
    }

    private void tile(ViewStatTileBinding tile, String value, int label, int color) {
        tile.txtValue.setText(value);
        tile.txtValue.setTextColor(color);
        tile.txtLabel.setText(label);
    }
}

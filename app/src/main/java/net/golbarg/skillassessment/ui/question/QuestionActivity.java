package net.golbarg.skillassessment.ui.question;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.snackbar.Snackbar;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.ads.AdManager;
import net.golbarg.skillassessment.databinding.ActivityQuestionBinding;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.models.Question;
import net.golbarg.skillassessment.util.CategoryNames;
import net.golbarg.skillassessment.util.ContentRenderer;
import net.golbarg.skillassessment.util.Feedback;
import net.golbarg.skillassessment.util.UiUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class QuestionActivity extends AppCompatActivity {
    public static final String[] LETTERS = {"A", "B", "C", "D", "E", "F", "G", "H"};

    private static final String EXTRA_CATEGORY = "category_id";
    private static final String EXTRA_LENGTH = "length";
    private static final String EXTRA_TIMER = "timer";
    private static final String EXTRA_SHUFFLE = "shuffle";
    private static final String EXTRA_EXAM = "exam";

    private ActivityQuestionBinding binding;
    private QuizViewModel vm;
    private final List<OptionView> optionViews = new ArrayList<>();
    private int renderedQuestionId = -1;
    private QuizViewModel.Reveal lastReveal = QuizViewModel.Reveal.NONE;
    private boolean dialogShowing;
    private ColorStateList defaultButtonTint;
    private ColorStateList defaultButtonText;
    private MaterialShapeDrawable panelBackground;
    private ValueAnimator panelColorAnimator;
    private int panelColor;
    private long lastTickSecond = -1;
    private int lastStreakShown;

    public static Intent intent(Context context, int categoryId, int length, boolean timer, boolean shuffle) {
        return intent(context, categoryId, length, timer, shuffle, false);
    }

    /**
     * @param categoryId a topic id or one of the pseudo topics ({@link Category#DAILY}, {@link Category#MIXED}, {@link Category#REVIEW})
     * @param exam       exam mode: one timer for the whole test and no per-question feedback
     */
    public static Intent intent(Context context, int categoryId, int length, boolean timer, boolean shuffle, boolean exam) {
        return new Intent(context, QuestionActivity.class)
                .putExtra(EXTRA_CATEGORY, categoryId)
                .putExtra(EXTRA_LENGTH, length)
                .putExtra(EXTRA_TIMER, timer)
                .putExtra(EXTRA_SHUFFLE, shuffle)
                .putExtra(EXTRA_EXAM, exam);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        UiUtils.enableEdgeToEdge(this);
        super.onCreate(savedInstanceState);
        binding = ActivityQuestionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // The top inset goes on the root; the bottom inset goes on the action sheet so its colour
        // continues behind the navigation bar.
        UiUtils.applySystemBarPadding(binding.root, true, false);
        UiUtils.applySystemBarPadding(binding.panel, false, true);
        setUpPanelBackground();
        defaultButtonTint = binding.btnPrimary.getBackgroundTintList();
        defaultButtonText = binding.btnPrimary.getTextColors();

        vm = new ViewModelProvider(this).get(QuizViewModel.class);
        Intent intent = getIntent();
        vm.start(intent.getIntExtra(EXTRA_CATEGORY, -1), intent.getIntExtra(EXTRA_LENGTH, 0),
                intent.getBooleanExtra(EXTRA_TIMER, true), intent.getBooleanExtra(EXTRA_SHUFFLE, true),
                intent.getBooleanExtra(EXTRA_EXAM, false));
        // After a rotation the current reveal was already celebrated; don't replay it.
        if (savedInstanceState != null) lastReveal = vm.getReveal();
        lastStreakShown = vm.getStreak();

        binding.btnClose.setOnClickListener(v -> confirmQuit());
        binding.btnBookmark.setOnClickListener(v -> {
            vm.toggleBookmark();
            Feedback.haptic(v, Feedback.Haptic.LIGHT);
            Snackbar.make(binding.getRoot(), vm.isCurrentBookmarked() ? R.string.added_to_bookmark : R.string.bookmark_deleted, Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.panel)
                    .show();
        });
        binding.btnSkip.setOnClickListener(v -> vm.skip());
        binding.btnPrimary.setOnClickListener(v -> {
            if (vm.getPhase() == QuizViewModel.Phase.ANSWERING) vm.check();
            else vm.next();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmQuit();
            }
        });

        vm.getState().observe(this, v -> render());
        vm.getTimeLeft().observe(this, this::renderTimer);
        vm.getFinished().observe(this, this::openResult);

        AdManager.loadBanner(this, binding.adContainer, AdManager.Banner.QUESTION);
        AdManager.preloadInterstitial(this, AdManager.Interstitial.TEST_FINISH);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!dialogShowing) vm.onScreenVisible();
    }

    @Override
    protected void onPause() {
        super.onPause();
        vm.onScreenHidden();
    }

    private void setUpPanelBackground() {
        float radius = UiUtils.dp(this, 28);
        panelBackground = new MaterialShapeDrawable(ShapeAppearanceModel.builder()
                .setTopLeftCornerSize(radius)
                .setTopRightCornerSize(radius)
                .build());
        panelColor = UiUtils.color(this, com.google.android.material.R.attr.colorSurfaceContainer);
        panelBackground.setFillColor(ColorStateList.valueOf(panelColor));
        binding.panel.setBackground(panelBackground);
    }

    private void animatePanelColor(int target, boolean animate) {
        if (panelColorAnimator != null) panelColorAnimator.cancel();
        if (!animate || target == panelColor) {
            panelColor = target;
            panelBackground.setFillColor(ColorStateList.valueOf(target));
            return;
        }
        panelColorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), panelColor, target);
        panelColorAnimator.setDuration(220);
        panelColorAnimator.addUpdateListener(a -> {
            panelColor = (int) a.getAnimatedValue();
            panelBackground.setFillColor(ColorStateList.valueOf(panelColor));
        });
        panelColorAnimator.start();
    }

    private void render() {
        QuizViewModel.Phase phase = vm.getPhase();
        binding.progressLoading.setVisibility(phase == QuizViewModel.Phase.LOADING ? View.VISIBLE : View.GONE);
        binding.txtEmpty.setVisibility(phase == QuizViewModel.Phase.EMPTY ? View.VISIBLE : View.GONE);
        boolean active = phase == QuizViewModel.Phase.ANSWERING || phase == QuizViewModel.Phase.REVEALED;
        binding.scroll.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
        binding.panel.setVisibility(active ? View.VISIBLE : View.GONE);
        binding.btnBookmark.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
        binding.timerRing.setVisibility(active && vm.isTimerEnabled() ? View.VISIBLE : View.GONE);
        binding.txtCounter.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
        Question question = vm.getCurrentQuestion();
        Category category = vm.getCategory();
        if (!active || question == null || category == null) return;

        if (question.getId() != renderedQuestionId) {
            renderedQuestionId = question.getId();
            renderQuestion(question, category, vm.getSlug(question));
        }

        int done = vm.getIndex() + (phase == QuizViewModel.Phase.REVEALED ? 1 : 0);
        binding.progressStep.setProgressCompat(done * 100 / Math.max(1, vm.getCount()), true);
        binding.txtCounter.setText((vm.getIndex() + 1) + "/" + vm.getCount());
        renderStreak();

        boolean bookmarked = vm.isCurrentBookmarked();
        binding.btnBookmark.setIconResource(bookmarked ? R.drawable.ic_bookmark : R.drawable.ic_bookmark_border);
        binding.btnBookmark.setIconTint(ColorStateList.valueOf(bookmarked
                ? UiUtils.color(this, androidx.appcompat.R.attr.colorPrimary)
                : UiUtils.color(this, com.google.android.material.R.attr.colorOnSurfaceVariant)));
        binding.btnBookmark.setContentDescription(getString(bookmarked ? R.string.bookmark_remove : R.string.bookmark_add));

        Set<Integer> selected = vm.getSelected();
        Set<Integer> correct = question.getCorrectPositions();
        for (int i = 0; i < optionViews.size(); i++) {
            OptionView option = optionViews.get(i);
            if (phase == QuizViewModel.Phase.ANSWERING) {
                option.setState(selected.contains(i) ? OptionView.State.SELECTED : OptionView.State.NORMAL);
            } else if (correct.contains(i)) {
                option.setState(selected.contains(i) ? OptionView.State.CORRECT : OptionView.State.MISSED);
            } else {
                option.setState(selected.contains(i) ? OptionView.State.WRONG : OptionView.State.DIMMED);
            }
            option.setEnabled(phase == QuizViewModel.Phase.ANSWERING);
        }

        renderPanel(phase, question, selected);
    }

    private void renderQuestion(Question question, Category category, String slug) {
        boolean dark = UiUtils.isNightMode(this);
        GradientDrawable badge = new GradientDrawable();
        badge.setCornerRadius(UiUtils.dp(this, 10));
        badge.setColor(CategoryNames.badgeBackground(slug, dark));
        binding.txtBadge.setBackground(badge);
        binding.txtBadge.setTextColor(CategoryNames.badgeForeground(slug, dark));
        binding.txtBadge.setText(CategoryNames.monogram(slug));
        // Mixed tests show each question's own topic, with the mode as context.
        String topic = CategoryNames.displayName(slug);
        binding.txtTopic.setText(category.isPseudo() ? topic + " · " + category.getDisplayName() : topic);
        String number = getString(R.string.question_label, vm.getIndex() + 1);
        binding.txtQuestionNumber.setText(vm.isExam() ? number + " · " + getString(R.string.exam_badge) : number);
        ContentRenderer.render(binding.questionContent, question.getTitle(), slug, ContentRenderer.Style.QUESTION, null);

        int required = question.getRequiredSelections();
        binding.txtHint.setVisibility(required > 1 ? View.VISIBLE : View.GONE);
        binding.txtHint.setText(getString(R.string.select_n_answers, required));

        binding.options.removeAllViews();
        optionViews.clear();
        int gap = UiUtils.dp(this, 10);
        for (int i = 0; i < question.getAnswers().size() && i < LETTERS.length; i++) {
            OptionView option = new OptionView(this);
            option.bind(LETTERS[i], question.getAnswers().get(i).getTitle(), slug);
            final int position = i;
            option.setOnClickListener(v -> {
                Feedback.play(Feedback.Sound.TAP);
                Feedback.haptic(v, Feedback.Haptic.LIGHT);
                vm.toggleOption(position);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.topMargin = gap;
            binding.options.addView(option, lp);
            optionViews.add(option);

            // Options slide in one after another.
            option.setAlpha(0f);
            option.setTranslationY(UiUtils.dp(this, 16));
            option.animate().alpha(1f).translationY(0).setStartDelay(60L + i * 45L).setDuration(240)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }

        binding.scroll.scrollTo(0, 0);
        binding.cardQuestion.setAlpha(0f);
        binding.cardQuestion.setTranslationX(UiUtils.dp(this, 24));
        binding.cardQuestion.animate().alpha(1f).translationX(0).setDuration(240).setInterpolator(new DecelerateInterpolator()).start();
        lastTickSecond = -1;
    }

    private void renderStreak() {
        int streak = vm.getStreak();
        boolean show = streak >= 2 && !vm.isExam();
        binding.chipStreak.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.txtStreak.setText(String.valueOf(streak));
        binding.chipStreak.setContentDescription(getResources().getQuantityString(R.plurals.streak_message, streak, streak));
        if (show && streak > lastStreakShown) {
            binding.chipStreak.setScaleX(0.6f);
            binding.chipStreak.setScaleY(0.6f);
            binding.chipStreak.animate().scaleX(1f).scaleY(1f).setDuration(320).setInterpolator(new OvershootInterpolator(3f)).start();
        }
        lastStreakShown = streak;
    }

    private void renderPanel(QuizViewModel.Phase phase, Question question, Set<Integer> selected) {
        if (phase == QuizViewModel.Phase.ANSWERING && vm.isExam()) {
            binding.feedback.setVisibility(View.GONE);
            binding.btnSkip.setVisibility(View.VISIBLE);
            binding.btnPrimary.setText(vm.isLastQuestion() ? R.string.finish_exam : R.string.next_question);
            binding.btnPrimary.setEnabled(!selected.isEmpty());
            return;
        }
        if (phase == QuizViewModel.Phase.ANSWERING) {
            boolean fromReveal = lastReveal != QuizViewModel.Reveal.NONE;
            lastReveal = QuizViewModel.Reveal.NONE;
            binding.feedback.setVisibility(View.GONE);
            binding.btnSkip.setVisibility(View.VISIBLE);
            binding.btnPrimary.setText(R.string.check);
            binding.btnPrimary.setEnabled(!selected.isEmpty());
            animatePanelColor(UiUtils.color(this, com.google.android.material.R.attr.colorSurfaceContainer), fromReveal);
            binding.btnPrimary.setBackgroundTintList(defaultButtonTint);
            binding.btnPrimary.setTextColor(defaultButtonText);
            return;
        }

        QuizViewModel.Reveal reveal = vm.getReveal();
        int accent;
        int onAccent;
        int container;
        int icon;
        int title;
        switch (reveal) {
            case CORRECT:
                accent = ContextCompat.getColor(this, R.color.correct);
                onAccent = ContextCompat.getColor(this, R.color.on_correct);
                container = ContextCompat.getColor(this, R.color.correct_container);
                icon = R.drawable.ic_check;
                title = R.string.feedback_correct;
                break;
            case WRONG:
                accent = ContextCompat.getColor(this, R.color.wrong);
                onAccent = ContextCompat.getColor(this, R.color.on_wrong);
                container = ContextCompat.getColor(this, R.color.wrong_container);
                icon = R.drawable.ic_close;
                title = R.string.feedback_wrong;
                break;
            case TIMEOUT:
                accent = ContextCompat.getColor(this, R.color.wrong);
                onAccent = ContextCompat.getColor(this, R.color.on_wrong);
                container = ContextCompat.getColor(this, R.color.wrong_container);
                icon = R.drawable.ic_timer;
                title = R.string.feedback_timeout;
                break;
            default:
                accent = UiUtils.color(this, com.google.android.material.R.attr.colorOnSurfaceVariant);
                onAccent = UiUtils.color(this, com.google.android.material.R.attr.colorSurface);
                container = UiUtils.color(this, com.google.android.material.R.attr.colorSurfaceContainerHigh);
                icon = R.drawable.ic_skip_circle;
                title = R.string.feedback_skipped;
                break;
        }

        boolean fresh = lastReveal != reveal;
        animatePanelColor(container, fresh);
        binding.feedback.setVisibility(View.VISIBLE);
        binding.feedbackIconBg.setBackgroundTintList(ColorStateList.valueOf(accent));
        binding.imgFeedback.setImageResource(icon);
        binding.imgFeedback.setImageTintList(ColorStateList.valueOf(onAccent));
        binding.txtFeedbackTitle.setText(title);
        binding.txtFeedbackTitle.setTextColor(accent);
        binding.txtFeedbackDetail.setTextColor(UiUtils.color(this, com.google.android.material.R.attr.colorOnSurface));
        if (reveal == QuizViewModel.Reveal.CORRECT) {
            int streak = vm.getStreak();
            binding.txtFeedbackDetail.setVisibility(streak >= 2 ? View.VISIBLE : View.GONE);
            binding.txtFeedbackDetail.setText(getResources().getQuantityString(R.plurals.streak_message, streak, streak));
        } else {
            binding.txtFeedbackDetail.setVisibility(View.VISIBLE);
            binding.txtFeedbackDetail.setText(getString(R.string.feedback_answer_is, letters(question.getCorrectPositions())));
        }
        binding.btnSkip.setVisibility(View.GONE);
        binding.btnPrimary.setEnabled(true);
        binding.btnPrimary.setText(vm.isLastQuestion() ? R.string.see_results : R.string.continue_label);
        boolean neutral = reveal == QuizViewModel.Reveal.SKIPPED;
        binding.btnPrimary.setBackgroundTintList(neutral ? defaultButtonTint : ColorStateList.valueOf(accent));
        if (neutral) binding.btnPrimary.setTextColor(defaultButtonText);
        else binding.btnPrimary.setTextColor(onAccent);

        if (fresh) {
            lastReveal = reveal;
            celebrate(reveal, question, selected);
        }
    }

    /** One-off sound, haptic and motion when an answer is revealed. */
    private void celebrate(QuizViewModel.Reveal reveal, Question question, Set<Integer> selected) {
        View root = binding.getRoot();
        switch (reveal) {
            case CORRECT:
                Feedback.play(Feedback.Sound.CORRECT);
                Feedback.haptic(root, Feedback.Haptic.SUCCESS);
                break;
            case WRONG:
                Feedback.play(Feedback.Sound.WRONG);
                Feedback.haptic(root, Feedback.Haptic.ERROR);
                break;
            case TIMEOUT:
                Feedback.play(Feedback.Sound.TIMEOUT);
                Feedback.haptic(root, Feedback.Haptic.ERROR);
                break;
            default:
                break;
        }

        Set<Integer> correct = question.getCorrectPositions();
        for (int i = 0; i < optionViews.size(); i++) {
            if (correct.contains(i)) optionViews.get(i).pulse();
            else if (selected.contains(i)) optionViews.get(i).shake();
        }

        binding.feedback.setAlpha(0f);
        binding.feedback.setTranslationY(UiUtils.dp(this, 18));
        binding.feedback.animate().alpha(1f).translationY(0).setDuration(260).setInterpolator(new DecelerateInterpolator()).start();
        binding.feedbackIconBg.setScaleX(0.4f);
        binding.feedbackIconBg.setScaleY(0.4f);
        binding.feedbackIconBg.animate().scaleX(1f).scaleY(1f).setStartDelay(60).setDuration(360)
                .setInterpolator(new OvershootInterpolator(2.5f)).start();
    }

    private static String letters(Set<Integer> positions) {
        StringBuilder sb = new StringBuilder();
        for (Integer p : positions) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(p < LETTERS.length ? LETTERS[p] : String.valueOf(p + 1));
        }
        return sb.toString();
    }

    private void renderTimer(Long millis) {
        if (millis == null) return;
        binding.timerRing.setTime(millis, vm.getTimerTotalMs());
        long seconds = (millis + 999) / 1000;
        long tickFrom = vm.isExam() ? 10 : 5;
        if (vm.getPhase() == QuizViewModel.Phase.ANSWERING && seconds <= tickFrom && seconds > 0 && seconds != lastTickSecond) {
            lastTickSecond = seconds;
            Feedback.play(Feedback.Sound.TICK);
        }
    }

    private void confirmQuit() {
        if (vm.getPhase() == QuizViewModel.Phase.EMPTY || vm.getPhase() == QuizViewModel.Phase.LOADING) {
            finish();
            return;
        }
        dialogShowing = true;
        vm.onScreenHidden();
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.quit_test_title)
                .setMessage(R.string.quit_test_message)
                .setPositiveButton(R.string.keep_going, null)
                .setNegativeButton(R.string.quit, (d, w) -> finish())
                .setOnDismissListener(d -> {
                    dialogShowing = false;
                    if (!isFinishing()) vm.onScreenVisible();
                });
        if (vm.getAnsweredCount() > 0) {
            builder.setMessage(getResources().getQuantityString(R.plurals.quit_test_message_partial, vm.getAnsweredCount(), vm.getAnsweredCount()));
            builder.setNeutralButton(R.string.finish_early, (d, w) -> vm.finish());
        }
        builder.show();
    }

    private void openResult(QuizViewModel.FinishInfo info) {
        if (info == null) return;
        if (info.resultId < 0) {
            finish();
            return;
        }
        if (vm.isExamTimedOut()) {
            Snackbar.make(binding.getRoot(), R.string.exam_time_up, Snackbar.LENGTH_SHORT).show();
        }
        Intent resultIntent = QuestionResultActivity.intent(this, info.resultId, getIntent().getExtras(), vm.getBestStreak(), info.outcome);
        AdManager.showInterstitial(this, AdManager.Interstitial.TEST_FINISH, () -> {
            startActivity(resultIntent);
            finish();
        });
    }
}

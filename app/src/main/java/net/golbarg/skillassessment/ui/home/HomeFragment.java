package net.golbarg.skillassessment.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.ads.AdManager;
import net.golbarg.skillassessment.billing.BillingManager;
import net.golbarg.skillassessment.databinding.FragmentHomeBinding;
import net.golbarg.skillassessment.databinding.ItemHomeHeaderBinding;
import net.golbarg.skillassessment.db.QuizRepository;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.ui.credits.CreditsSheet;
import net.golbarg.skillassessment.ui.question.QuestionActivity;
import net.golbarg.skillassessment.ui.question.QuizSession;
import net.golbarg.skillassessment.ui.question.QuizViewModel;
import net.golbarg.skillassessment.ui.search.SearchActivity;
import net.golbarg.skillassessment.ui.widget.GridGapDecoration;
import net.golbarg.skillassessment.ui.widget.StaticViewAdapter;
import net.golbarg.skillassessment.util.Async;
import net.golbarg.skillassessment.util.CategoryNames;
import net.golbarg.skillassessment.widget.DailyQuestionWidget;
import net.golbarg.skillassessment.util.Feedback;
import net.golbarg.skillassessment.util.GoalPicker;
import net.golbarg.skillassessment.util.Prefs;
import net.golbarg.skillassessment.util.ProgressTracker;
import net.golbarg.skillassessment.util.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements CategoryAdapter.Listener {

    private static final class HomeData {
        List<Category> categories;
        QuizRepository.Stats stats;
        int credits;
        int reviewDue;
        int unlockedQuestions;
        int dayStreak;
        boolean dailyDone;
        @Nullable QuizSession session;
        String sessionTopic;
        int restorableStreak;
        int answeredToday;
        int goal;
    }

    /** Topic cards per row: one on phones, more on tablets and landscape. */
    private static final int CARD_MIN_WIDTH_DP = 340;

    private FragmentHomeBinding binding;
    private ItemHomeHeaderBinding header;
    private CategoryAdapter adapter;
    private QuizRepository repository;
    private List<Category> allCategories = Collections.emptyList();
    private int credits;
    private boolean loadedOnce;
    /** Finished previews come back here when the user chose to unlock the topic. */
    private ActivityResultLauncher<Intent> previewLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        previewLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) return;
            int id = result.getData().getIntExtra(QuestionActivity.EXTRA_UNLOCK_CATEGORY, -1);
            for (Category c : allCategories) {
                if (c.getId() == id && !c.isUnlocked()) unlockNow(c);
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = QuizRepository.get(requireContext());
        UiUtils.applySystemBarPadding(binding.root, true, false);

        int widthDp = getResources().getConfiguration().screenWidthDp;
        int spans = Math.max(1, widthDp / CARD_MIN_WIDTH_DP);
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), spans);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position == 0 ? spans : 1; // the header spans the full width
            }
        });
        binding.listCategories.setLayoutManager(layoutManager);
        if (spans > 1) binding.listCategories.addItemDecoration(new GridGapDecoration(UiUtils.dp(requireContext(), 5)));
        // Filtering replaces most of the list at once; animating that looks noisy and could leave
        // freshly added rows invisible until the next scroll.
        binding.listCategories.setItemAnimator(null);
        header = ItemHomeHeaderBinding.inflate(getLayoutInflater(), binding.listCategories, false);
        adapter = new CategoryAdapter(this);
        binding.listCategories.setAdapter(new ConcatAdapter(new StaticViewAdapter(header.getRoot(), R.layout.item_home_header), adapter));

        header.editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) { applyFilter(); }
        });
        header.chipGroupDomain.setOnCheckedStateChangeListener((group, checkedIds) -> applyFilter());
        header.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> applyFilter());
        binding.cardCoins.setOnClickListener(v -> openCredits());
        header.btnDaily.setOnClickListener(v -> startDaily());
        header.cardDaily.setOnClickListener(v -> startDaily());
        header.cardReview.setOnClickListener(v -> startReview());
        header.cardMixed.setOnClickListener(v -> startMixed());
        header.cardGoal.setOnClickListener(v -> GoalPicker.show(requireContext(), this::load));
        header.btnResume.setOnClickListener(v -> startActivity(QuestionActivity.resumeIntent(requireContext())));
        header.btnResumeDiscard.setOnClickListener(v -> {
            QuizSession.clear(requireContext());
            load();
        });
        header.btnStreakRestore.setOnClickListener(v -> restoreStreak());
        binding.btnSearchQuestions.setOnClickListener(v -> startActivity(new Intent(requireContext(), SearchActivity.class)));

        getChildFragmentManager().setFragmentResultListener(CreditsSheet.RESULT_KEY, getViewLifecycleOwner(), (key, bundle) -> load());

        AdManager.loadBanner(getViewLifecycleOwner(), binding.adContainer, AdManager.Banner.HOME);
    }

    @Override
    public void onResume() {
        super.onResume();
        String name = Prefs.getUserName(requireContext());
        binding.txtGreeting.setText(name.isEmpty() ? getString(R.string.greeting_default) : getString(R.string.greeting_named, name));
        load();
    }

    private void load() {
        if (!loadedOnce) binding.progressLoading.setVisibility(View.VISIBLE);
        Async.run(getViewLifecycleOwner(), () -> {
            HomeData data = new HomeData();
            data.categories = repository.getCategories();
            data.stats = repository.getStats();
            data.credits = repository.getCredits();
            data.reviewDue = repository.getReviewDueCount();
            for (Category c : data.categories) if (c.isUnlocked()) data.unlockedQuestions += c.getNumberOfQuestion();
            data.dayStreak = ProgressTracker.getDayStreak(requireContext());
            data.dailyDone = ProgressTracker.isDailyDone(requireContext());
            data.session = QuizSession.load(requireContext());
            if (data.session != null) {
                int id = data.session.categoryId;
                data.sessionTopic = Category.pseudo(id, 0).getDisplayName();
                for (Category c : data.categories) if (c.getId() == id) data.sessionTopic = c.getDisplayName();
            }
            data.restorableStreak = ProgressTracker.getRestorableStreak(requireContext());
            data.answeredToday = ProgressTracker.getAnsweredToday(requireContext());
            data.goal = Prefs.getDailyGoal(requireContext());
            return data;
        }, data -> {
            if (binding == null || data == null) return;
            loadedOnce = true;
            binding.progressLoading.setVisibility(View.GONE);
            allCategories = data.categories;
            credits = data.credits;
            binding.txtCoins.setText(String.valueOf(credits));
            binding.cardCoins.setContentDescription(getString(R.string.coin_balance_cd, credits));
            header.txtStatTests.setText(String.valueOf(data.stats.tests));
            header.txtStatAccuracy.setText(data.stats.answered() == 0 ? "—" : data.stats.accuracyPercent() + "%");
            header.txtStatTopics.setText(String.format(Locale.getDefault(), "%d/%d", data.stats.unlockedTopics, allCategories.size()));
            bindToday(data);
            bindNotices(data);
            bindGoal(data);
            applyFilter();
        });
    }

    private HomeData today;

    private void bindToday(HomeData data) {
        today = data;
        boolean hasTopics = data.unlockedQuestions > 0;

        binding.txtDayStreak.setText(String.valueOf(data.dayStreak));
        binding.imgDayStreak.setAlpha(data.dayStreak > 0 ? 1f : 0.35f);
        binding.chipDayStreak.setContentDescription(data.dayStreak > 0
                ? getResources().getQuantityString(R.plurals.day_streak, data.dayStreak, data.dayStreak)
                : getString(R.string.day_streak_zero));

        if (!hasTopics) {
            header.txtDailyDesc.setText(R.string.daily_locked);
            header.btnDaily.setVisibility(View.GONE);
            header.imgDailyDone.setVisibility(View.GONE);
        } else if (data.dailyDone) {
            header.txtDailyDesc.setText(R.string.daily_done);
            header.btnDaily.setVisibility(View.GONE);
            header.imgDailyDone.setVisibility(View.VISIBLE);
        } else {
            header.txtDailyDesc.setText(getString(R.string.daily_challenge_desc, ProgressTracker.DAILY_QUESTIONS, ProgressTracker.DAILY_BONUS_COINS));
            header.btnDaily.setVisibility(View.VISIBLE);
            header.imgDailyDone.setVisibility(View.GONE);
        }

        header.txtReviewDesc.setText(data.reviewDue > 0
                ? getResources().getQuantityString(R.plurals.review_due, data.reviewDue, data.reviewDue)
                : getString(R.string.mistakes_none));
        header.cardReview.setAlpha(data.reviewDue > 0 ? 1f : 0.6f);
        header.cardMixed.setAlpha(hasTopics ? 1f : 0.6f);
    }

    private void bindNotices(HomeData data) {
        QuizSession s = data.session;
        header.cardResume.setVisibility(s != null ? View.VISIBLE : View.GONE);
        if (s != null) {
            header.txtResumeDesc.setText(getString(R.string.resume_desc, data.sessionTopic, s.answeredCount(), s.total()));
        }

        boolean restorable = data.restorableStreak > 0;
        header.cardStreakRestore.setVisibility(restorable ? View.VISIBLE : View.GONE);
        if (restorable) {
            boolean premium = BillingManager.isPremium(requireContext());
            header.txtStreakRestoreTitle.setText(getString(R.string.streak_restore_title, data.restorableStreak));
            header.txtStreakRestoreDesc.setText(premium ? R.string.streak_restore_desc_premium : R.string.streak_restore_desc);
            if (!premium) AdManager.loadRewarded(requireContext(), null);
        }
    }

    private void bindGoal(HomeData data) {
        int goal = data.goal;
        int done = data.answeredToday;
        if (goal <= 0) {
            header.progressGoal.setProgressCompat(0, false);
            header.txtGoalDesc.setText(R.string.daily_goal_set);
            header.imgGoal.setImageResource(R.drawable.ic_flag);
            return;
        }
        boolean reached = done >= goal;
        header.progressGoal.setProgressCompat(Math.min(100, done * 100 / goal), true);
        header.imgGoal.setImageResource(reached ? R.drawable.ic_check : R.drawable.ic_flag);
        header.txtGoalDesc.setText(reached ? getString(R.string.daily_goal_done, done) : getString(R.string.daily_goal_progress, done, goal));
    }

    /** Streak freeze: free with Premium, otherwise after an opt-in rewarded video. */
    private void restoreStreak() {
        if (BillingManager.isPremium(requireContext())) {
            onStreakRestored(ProgressTracker.restoreStreak(requireContext()));
            return;
        }
        if (!AdManager.isRewardedReady()) {
            AdManager.loadRewarded(requireContext(), null);
            UiUtils.snackbar(binding.getRoot(), R.string.hint_unavailable, Snackbar.LENGTH_LONG).show();
            return;
        }
        android.content.Context app = requireContext().getApplicationContext();
        AdManager.showRewarded(requireActivity(), () -> {
            int restored = ProgressTracker.restoreStreak(app);
            if (binding != null) onStreakRestored(restored);
        }, () -> {
        });
    }

    private void onStreakRestored(int streak) {
        if (streak <= 0 || binding == null) return;
        Feedback.play(Feedback.Sound.UNLOCK);
        Feedback.haptic(binding.getRoot(), Feedback.Haptic.SUCCESS);
        UiUtils.snackbar(binding.getRoot(), getString(R.string.streak_restored, streak), Snackbar.LENGTH_LONG).show();
        load();
    }

    private boolean requireTopics() {
        if (today != null && today.unlockedQuestions > 0) return true;
        UiUtils.snackbar(binding.getRoot(), R.string.unlock_topic_first, Snackbar.LENGTH_SHORT).show();
        return false;
    }

    private void startDaily() {
        if (!requireTopics() || today.dailyDone) return;
        launchTest(QuestionActivity.intent(requireContext(), Category.DAILY, ProgressTracker.DAILY_QUESTIONS,
                Prefs.isTimerEnabled(requireContext()), false, false));
    }

    private void startReview() {
        if (today == null || today.reviewDue == 0) {
            UiUtils.snackbar(binding.getRoot(), R.string.no_review_questions, Snackbar.LENGTH_SHORT).show();
            return;
        }
        launchTest(QuestionActivity.intent(requireContext(), Category.REVIEW, 0, Prefs.isTimerEnabled(requireContext()), false, false));
    }

    private void startMixed() {
        if (!requireTopics()) return;
        onOpen(Category.pseudo(Category.MIXED, today.unlockedQuestions));
    }

    private void launchTest(Intent intent) {
        startActivity(intent);
    }

    private void applyFilter() {
        if (header == null) return;
        String query = header.editSearch.getText() == null ? "" : header.editSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        int checked = header.chipGroupFilter.getCheckedChipId();
        int domainChip = header.chipGroupDomain.getCheckedChipId();
        CategoryNames.Domain selectedDomain;
        if (domainChip == R.id.chip_domain_languages) selectedDomain = CategoryNames.Domain.LANGUAGES;
        else if (domainChip == R.id.chip_domain_frontend) selectedDomain = CategoryNames.Domain.FRONTEND;
        else if (domainChip == R.id.chip_domain_backend) selectedDomain = CategoryNames.Domain.BACKEND;
        else if (domainChip == R.id.chip_domain_mobile) selectedDomain = CategoryNames.Domain.MOBILE;
        else if (domainChip == R.id.chip_domain_databases) selectedDomain = CategoryNames.Domain.DATABASES;
        else if (domainChip == R.id.chip_domain_cloud_devops) selectedDomain = CategoryNames.Domain.CLOUD_DEVOPS;
        else if (domainChip == R.id.chip_domain_ai_data) selectedDomain = CategoryNames.Domain.AI_DATA;
        else if (domainChip == R.id.chip_domain_tools) selectedDomain = CategoryNames.Domain.TOOLS_OTHER;
        else selectedDomain = CategoryNames.Domain.ALL;

        List<Category> filtered = new ArrayList<>();
        for (Category c : allCategories) {
            if (selectedDomain != CategoryNames.Domain.ALL && CategoryNames.getDomain(c.getSlug()) != selectedDomain) continue;
            if (checked == R.id.chip_unlocked && !c.isUnlocked()) continue;
            if (checked == R.id.chip_locked && c.isUnlocked()) continue;
            if (!query.isEmpty() && !c.getDisplayName().toLowerCase(Locale.ROOT).contains(query) && !c.getSlug().contains(query)) continue;
            filtered.add(c);
        }
        adapter.submitList(filtered);
        if (filtered.isEmpty() && loadedOnce) {
            header.txtEmpty.setVisibility(View.VISIBLE);
            header.txtEmpty.setText(query.isEmpty() ? getString(R.string.no_topics_in_filter) : getString(R.string.no_topics_found, query));
        } else {
            header.txtEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onOpen(Category category) {
        TestSetupSheet.newInstance(category).show(getChildFragmentManager(), TestSetupSheet.TAG);
    }

    @Override
    public void onUnlock(Category category) {
        if (adapter.isBusy()) return;
        boolean premium = BillingManager.isPremium(requireContext());
        boolean canPreview = !Prefs.isPreviewUsed(requireContext(), category.getId());
        if (premium && !canPreview) {
            startUnlock(category);
            return;
        }
        boolean affordable = premium || credits >= QuizRepository.UNLOCK_COST;
        String questions = getResources().getQuantityString(R.plurals.question_count, category.getNumberOfQuestion(), category.getNumberOfQuestion());
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_lock)
                .setTitle(getString(R.string.unlock) + " " + category.getDisplayName() + "?")
                .setMessage(premium ? questions : affordable
                        ? getString(R.string.unlock_confirm_message, QuizRepository.UNLOCK_COST, credits, questions)
                        : questions + "\n\n" + getString(R.string.not_enough_coins, QuizRepository.UNLOCK_COST))
                .setNegativeButton(R.string.cancel, null);
        if (affordable) {
            builder.setPositiveButton(premium ? getString(R.string.unlock_free) : getString(R.string.unlock_cost, QuizRepository.UNLOCK_COST),
                    (d, w) -> startUnlock(category));
        } else {
            builder.setPositiveButton(R.string.get_coins, (d, w) -> openCredits());
        }
        if (canPreview) {
            builder.setNeutralButton(getString(R.string.preview_try, QuizViewModel.PREVIEW_LENGTH), (d, w) -> startPreview(category));
        }
        builder.show();
    }

    private void startPreview(Category category) {
        Prefs.setPreviewUsed(requireContext(), category.getId());
        previewLauncher.launch(QuestionActivity.previewIntent(requireContext(), category.getId(), Prefs.isTimerEnabled(requireContext())));
    }

    /** From a finished preview: unlock right away if affordable, otherwise explain how to get coins. */
    private void unlockNow(Category category) {
        if (BillingManager.isPremium(requireContext()) || credits >= QuizRepository.UNLOCK_COST) {
            startUnlock(category);
        } else {
            UiUtils.snackbar(binding.getRoot(), getString(R.string.not_enough_coins, QuizRepository.UNLOCK_COST), Snackbar.LENGTH_LONG)
                    .setAction(R.string.get_coins, v -> openCredits())
                    .show();
        }
    }

    private void startUnlock(Category category) {
        adapter.setUnlocking(category.getId(), true);
        Async.run(getViewLifecycleOwner(), () -> repository.unlockCategory(category.getId()), result -> {
            if (binding == null) return;
            adapter.setUnlocking(category.getId(), false);
            if (result == QuizRepository.UnlockResult.SUCCESS) {
                Feedback.play(Feedback.Sound.UNLOCK);
                DailyQuestionWidget.refresh(requireContext());
                Feedback.haptic(binding.getRoot(), Feedback.Haptic.SUCCESS);
                UiUtils.snackbar(binding.getRoot(), getString(R.string.unlocked_successfully, category.getDisplayName()), Snackbar.LENGTH_LONG)
                        .setAction(R.string.start_test, v -> onOpen(category))
                        .show();
            } else if (result == QuizRepository.UnlockResult.NOT_ENOUGH_COINS) {
                openCredits();
            } else {
                UiUtils.snackbar(binding.getRoot(), R.string.unlock_failed, Snackbar.LENGTH_LONG).show();
            }
            load();
        });
    }

    private void openCredits() {
        if (getChildFragmentManager().findFragmentByTag(CreditsSheet.TAG) == null) {
            new CreditsSheet().show(getChildFragmentManager(), CreditsSheet.TAG);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        header = null;
    }
}

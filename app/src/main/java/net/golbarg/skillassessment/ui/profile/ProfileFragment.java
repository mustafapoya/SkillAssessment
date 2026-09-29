package net.golbarg.skillassessment.ui.profile;

import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.ads.AdManager;
import net.golbarg.skillassessment.databinding.DialogEditNameBinding;
import net.golbarg.skillassessment.databinding.FragmentProfileBinding;
import net.golbarg.skillassessment.databinding.ItemAchievementBinding;
import net.golbarg.skillassessment.databinding.ItemHistoryBinding;
import net.golbarg.skillassessment.databinding.ItemProfileHeaderBinding;
import net.golbarg.skillassessment.databinding.ItemTopicProgressBinding;
import net.golbarg.skillassessment.db.QuizRepository;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.models.QuestionResult;
import net.golbarg.skillassessment.ui.home.TestSetupSheet;
import net.golbarg.skillassessment.ui.question.QuestionResultActivity;
import net.golbarg.skillassessment.ui.widget.StaticViewAdapter;
import net.golbarg.skillassessment.util.Async;
import net.golbarg.skillassessment.util.CategoryNames;
import net.golbarg.skillassessment.util.Prefs;
import net.golbarg.skillassessment.util.ProgressTracker;
import net.golbarg.skillassessment.util.UiUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment {
    private static final int TOPICS_COLLAPSED = 4;

    private static final class Data {
        QuizRepository.Stats stats;
        List<QuestionResult> results;
        Map<Integer, Category> categories = new HashMap<>();
        List<Category> unlockedTopics = new ArrayList<>();
        List<ProgressTracker.AchievementState> achievements;
        int[] week = new int[7];
    }

    private FragmentProfileBinding binding;
    private ItemProfileHeaderBinding header;
    private QuizRepository repository;
    private final HistoryAdapter historyAdapter = new HistoryAdapter();
    private Data data;
    private boolean firstLoad = true;
    private boolean topicsExpanded;
    private boolean firstWeekLoad = true;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = QuizRepository.get(requireContext());
        UiUtils.applySystemBarPadding(binding.root, true, false);

        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        header = ItemProfileHeaderBinding.inflate(getLayoutInflater(), binding.list, false);
        binding.list.setAdapter(new ConcatAdapter(new StaticViewAdapter(header.getRoot(), R.layout.item_profile_header), historyAdapter));

        header.cardProfile.setOnClickListener(v -> editName());
        header.btnClear.setOnClickListener(v -> confirmClear());
        header.empty.imgIcon.setImageResource(R.drawable.ic_history);
        header.empty.txtTitle.setText(R.string.history_empty_title);
        header.empty.txtDescription.setText(R.string.history_empty_desc);
        legendDot(header.txtLegendCorrect, ContextCompat.getColor(requireContext(), R.color.correct));
        legendDot(header.txtLegendWrong, ContextCompat.getColor(requireContext(), R.color.wrong));
        legendDot(header.txtLegendSkipped, ContextCompat.getColor(requireContext(), R.color.skipped));

        AdManager.loadBanner(getViewLifecycleOwner(), binding.adContainer, AdManager.Banner.PROFILE);
    }

    @Override
    public void onResume() {
        super.onResume();
        bindName();
        load();
    }

    private void load() {
        Async.run(getViewLifecycleOwner(), () -> {
            Data d = new Data();
            d.stats = repository.getStats();
            d.results = repository.getResults();
            for (Category c : repository.getCategories()) {
                d.categories.put(c.getId(), c);
                if (c.isUnlocked()) d.unlockedTopics.add(c);
            }
            // Weakest first: the topic most worth practising next leads the list.
            Collections.sort(d.unlockedTopics, (a, b) -> a.getMasteryPercent() != b.getMasteryPercent()
                    ? Integer.compare(a.getMasteryPercent(), b.getMasteryPercent())
                    : a.getDisplayName().compareToIgnoreCase(b.getDisplayName()));
            for (int id : new int[]{Category.MIXED, Category.DAILY, Category.REVIEW, Category.BOOKMARKED}) d.categories.put(id, Category.pseudo(id, 0));
            // Catch up on achievements earned before they existed (e.g. after an update).
            ProgressTracker.evaluate(requireContext().getApplicationContext(), repository);
            d.achievements = ProgressTracker.getAchievements(requireContext().getApplicationContext());
            // Questions answered per day, oldest of the last seven days first.
            LocalDate today = LocalDate.now();
            ZoneId zone = ZoneId.systemDefault();
            for (QuestionResult r : d.results) {
                if (r.getCreatedAt() <= 0) continue;
                long daysAgo = ChronoUnit.DAYS.between(Instant.ofEpochMilli(r.getCreatedAt()).atZone(zone).toLocalDate(), today);
                if (daysAgo >= 0 && daysAgo < 7) d.week[6 - (int) daysAgo] += r.getTotal();
            }
            return d;
        }, d -> {
            if (binding == null || d == null) return;
            data = d;
            QuizRepository.Stats s = d.stats;
            header.ring.setValues(s.correct, s.wrong, s.skipped, firstLoad);
            firstLoad = false;
            header.txtAccuracy.setText(s.answered() == 0 ? "—" : s.accuracyPercent() + "%");
            header.txtLegendCorrect.setText(getString(R.string.correct) + "  " + s.correct);
            header.txtLegendWrong.setText(getString(R.string.wrong) + "  " + s.wrong);
            header.txtLegendSkipped.setText(getString(R.string.skipped) + "  " + s.skipped);
            header.txtProfileMeta.setText(getResources().getQuantityString(R.plurals.tests_taken, s.tests, s.tests)
                    + " · " + s.unlockedTopics + " " + getString(R.string.filter_unlocked).toLowerCase(Locale.getDefault()));
            bindAchievements(d.achievements);
            bindTopics();
            bindWeek(d.week, firstWeekLoad);
            firstWeekLoad = false;
            header.empty.getRoot().setVisibility(d.results.isEmpty() ? View.VISIBLE : View.GONE);
            header.btnClear.setVisibility(d.results.isEmpty() ? View.GONE : View.VISIBLE);
            historyAdapter.notifyDataSetChanged();
        });
    }

    private void bindAchievements(List<ProgressTracker.AchievementState> states) {
        header.gridAchievements.removeAllViews();
        int unlocked = 0;
        int primary = UiUtils.color(requireContext(), androidx.appcompat.R.attr.colorPrimary);
        int onPrimary = UiUtils.color(requireContext(), com.google.android.material.R.attr.colorOnPrimary);
        int lockedBg = UiUtils.color(requireContext(), com.google.android.material.R.attr.colorSurfaceContainerHighest);
        int lockedFg = UiUtils.color(requireContext(), com.google.android.material.R.attr.colorOutline);
        int onSurface = UiUtils.color(requireContext(), com.google.android.material.R.attr.colorOnSurface);
        int onSurfaceVariant = UiUtils.color(requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant);
        for (ProgressTracker.AchievementState state : states) {
            ItemAchievementBinding item = ItemAchievementBinding.inflate(getLayoutInflater(), header.gridAchievements, false);
            boolean on = state.isUnlocked();
            if (on) unlocked++;
            item.badge.setBackgroundTintList(ColorStateList.valueOf(on ? primary : lockedBg));
            item.imgIcon.setImageResource(state.achievement.icon);
            item.imgIcon.setImageTintList(ColorStateList.valueOf(on ? onPrimary : lockedFg));
            item.txtTitle.setText(state.achievement.title);
            item.txtTitle.setTextColor(on ? onSurface : onSurfaceVariant);
            item.getRoot().setAlpha(on ? 1f : 0.7f);
            item.getRoot().setContentDescription(getString(state.achievement.title) + ". " + getString(state.achievement.description));
            item.getRoot().setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                    .setIcon(state.achievement.icon)
                    .setTitle(state.achievement.title)
                    .setMessage(getString(state.achievement.description) + "\n\n" + (on
                            ? getString(R.string.achievement_unlocked_on, UiUtils.formatDate(state.unlockedAt))
                            : getString(R.string.achievement_locked)))
                    .setPositiveButton(android.R.string.ok, null)
                    .show());
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
            lp.width = 0;
            header.gridAchievements.addView(item.getRoot(), lp);
        }
        header.txtAchievementsCount.setText(getString(R.string.achievements_progress, unlocked, states.size()));
    }

    private void bindWeek(int[] week, boolean animate) {
        String[] labels = new String[7];
        LocalDate today = LocalDate.now();
        int total = 0;
        int activeDays = 0;
        for (int i = 0; i < 7; i++) {
            labels[i] = today.minusDays(6 - i).getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
            total += week[i];
            if (week[i] > 0) activeDays++;
        }
        header.weekBars.setData(week, labels, animate);
        header.txtWeekSummary.setText(getResources().getQuantityString(R.plurals.week_questions, total, total)
                + " · " + getResources().getQuantityString(R.plurals.active_days, activeDays, activeDays));
    }

    private void bindTopics() {
        List<Category> topics = data.unlockedTopics;
        header.sectionTopics.setVisibility(topics.isEmpty() ? View.GONE : View.VISIBLE);
        header.listTopics.removeAllViews();
        int shown = topicsExpanded ? topics.size() : Math.min(TOPICS_COLLAPSED, topics.size());
        boolean dark = UiUtils.isNightMode(requireContext());
        for (int i = 0; i < shown; i++) {
            Category c = topics.get(i);
            ItemTopicProgressBinding row = ItemTopicProgressBinding.inflate(getLayoutInflater(), header.listTopics, false);
            GradientDrawable badge = new GradientDrawable();
            badge.setCornerRadius(UiUtils.dp(requireContext(), 12));
            badge.setColor(CategoryNames.badgeBackground(c.getSlug(), dark));
            row.txtBadge.setBackground(badge);
            row.txtBadge.setTextColor(CategoryNames.badgeForeground(c.getSlug(), dark));
            row.txtBadge.setText(CategoryNames.monogram(c.getSlug()));
            row.txtTitle.setText(c.getDisplayName());
            int percent = c.getMasteryPercent();
            row.progress.setProgressCompat(percent, false);
            row.txtPercent.setText(percent + "%");
            row.txtSubtitle.setText(c.getAttempts() > 0 && c.getBestScore() >= 0
                    ? getString(R.string.topic_progress_detail, c.getMastered(), c.getNumberOfQuestion(), c.getBestScore())
                    : getString(R.string.topic_progress_new, c.getMastered(), c.getNumberOfQuestion()));
            row.getRoot().setContentDescription(c.getDisplayName() + ", " + row.txtSubtitle.getText());
            row.getRoot().setOnClickListener(v -> {
                if (getChildFragmentManager().findFragmentByTag(TestSetupSheet.TAG) == null) {
                    TestSetupSheet.newInstance(c).show(getChildFragmentManager(), TestSetupSheet.TAG);
                }
            });
            header.listTopics.addView(row.getRoot());
        }
        boolean collapsible = topics.size() > TOPICS_COLLAPSED;
        header.btnTopicsMore.setVisibility(collapsible ? View.VISIBLE : View.GONE);
        header.btnTopicsMore.setText(topicsExpanded ? getString(R.string.topics_show_less)
                : getString(R.string.topics_show_all, topics.size()));
        header.btnTopicsMore.setOnClickListener(v -> {
            topicsExpanded = !topicsExpanded;
            bindTopics();
        });
    }

    private void bindName() {
        String name = Prefs.getUserName(requireContext());
        header.txtName.setText(name.isEmpty() ? getString(R.string.add_your_name) : name);
        header.txtAvatar.setText(initials(name));
    }

    private static String initials(String name) {
        if (name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        String result = parts[0].substring(0, 1);
        if (parts.length > 1) result += parts[parts.length - 1].substring(0, 1);
        return result.toUpperCase(Locale.getDefault());
    }

    private void editName() {
        DialogEditNameBinding dialog = DialogEditNameBinding.inflate(getLayoutInflater());
        dialog.editName.setText(Prefs.getUserName(requireContext()));
        dialog.editName.setSelection(dialog.editName.length());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.edit_name)
                .setView(dialog.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (d, w) -> {
                    Prefs.setUserName(requireContext(), dialog.editName.getText() == null ? "" : dialog.editName.getText().toString());
                    bindName();
                })
                .show();
        dialog.editName.requestFocus();
    }

    private void confirmClear() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.clear_history)
                .setMessage(R.string.clear_history_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.clear, (d, w) -> Async.run(getViewLifecycleOwner(), () -> {
                    repository.clearHistory();
                    return true;
                }, ok -> load()))
                .show();
    }

    private void legendDot(TextView view, int color) {
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(color);
        int size = UiUtils.dp(requireContext(), 10);
        dot.setSize(size, size);
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(dot, null, null, null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        header = null;
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryHolder> {
        @NonNull
        @Override
        public HistoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new HistoryHolder(ItemHistoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull HistoryHolder holder, int position) {
            QuestionResult r = data.results.get(position);
            Category c = data.categories.get(r.getCategoryId());
            String slug = c == null ? "" : c.getSlug();
            ItemHistoryBinding b = holder.binding;
            boolean dark = UiUtils.isNightMode(requireContext());

            GradientDrawable badge = new GradientDrawable();
            badge.setCornerRadius(UiUtils.dp(requireContext(), 14));
            badge.setColor(CategoryNames.badgeBackground(slug, dark));
            b.txtBadge.setBackground(badge);
            b.txtBadge.setTextColor(CategoryNames.badgeForeground(slug, dark));
            b.txtBadge.setText(CategoryNames.monogram(slug));
            b.txtTitle.setText(CategoryNames.displayName(slug));

            List<String> meta = new ArrayList<>();
            if (r.getCreatedAt() > 0) meta.add(UiUtils.formatDate(r.getCreatedAt()));
            meta.add(getString(R.string.history_score, r.getCorrectAnswer(), r.getTotal()));
            if (r.getDurationMs() > 0) meta.add(UiUtils.formatDuration(r.getDurationMs()));
            b.txtSubtitle.setText(android.text.TextUtils.join(" · ", meta));

            int percent = r.getScorePercent();
            int fg;
            int bg;
            if (percent >= 70) {
                fg = ContextCompat.getColor(requireContext(), R.color.on_correct_container);
                bg = ContextCompat.getColor(requireContext(), R.color.correct_container);
            } else if (percent >= 50) {
                fg = ContextCompat.getColor(requireContext(), R.color.on_coin_container);
                bg = ContextCompat.getColor(requireContext(), R.color.coin_container);
            } else {
                fg = ContextCompat.getColor(requireContext(), R.color.on_wrong_container);
                bg = ContextCompat.getColor(requireContext(), R.color.wrong_container);
            }
            GradientDrawable pill = new GradientDrawable();
            pill.setCornerRadius(UiUtils.dp(requireContext(), 16));
            pill.setColor(ColorStateList.valueOf(bg));
            b.txtScore.setBackground(pill);
            b.txtScore.setTextColor(fg);
            b.txtScore.setText(percent + "%");

            b.card.setOnClickListener(v -> startActivity(QuestionResultActivity.intent(requireContext(), r.getId(), null)));
        }

        @Override
        public int getItemCount() {
            return data == null ? 0 : data.results.size();
        }
    }

    private static class HistoryHolder extends RecyclerView.ViewHolder {
        final ItemHistoryBinding binding;

        HistoryHolder(ItemHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

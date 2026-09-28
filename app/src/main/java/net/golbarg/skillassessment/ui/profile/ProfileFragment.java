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
import net.golbarg.skillassessment.db.QuizRepository;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.models.QuestionResult;
import net.golbarg.skillassessment.ui.question.QuestionResultActivity;
import net.golbarg.skillassessment.ui.widget.StaticViewAdapter;
import net.golbarg.skillassessment.util.Async;
import net.golbarg.skillassessment.util.CategoryNames;
import net.golbarg.skillassessment.util.Prefs;
import net.golbarg.skillassessment.util.ProgressTracker;
import net.golbarg.skillassessment.util.UiUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private static final class Data {
        QuizRepository.Stats stats;
        List<QuestionResult> results;
        Map<Integer, Category> categories = new HashMap<>();
        List<ProgressTracker.AchievementState> achievements;
    }

    private FragmentProfileBinding binding;
    private ItemProfileHeaderBinding header;
    private QuizRepository repository;
    private final HistoryAdapter historyAdapter = new HistoryAdapter();
    private Data data;
    private boolean firstLoad = true;

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
            for (Category c : repository.getCategories()) d.categories.put(c.getId(), c);
            for (int id : new int[]{Category.MIXED, Category.DAILY, Category.REVIEW}) d.categories.put(id, Category.pseudo(id, 0));
            // Catch up on achievements earned before they existed (e.g. after an update).
            ProgressTracker.evaluate(requireContext().getApplicationContext(), repository);
            d.achievements = ProgressTracker.getAchievements(requireContext().getApplicationContext());
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

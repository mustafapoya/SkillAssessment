package net.golbarg.skillassessment.ui.home;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.billing.BillingManager;
import net.golbarg.skillassessment.databinding.ItemCategoryBinding;
import net.golbarg.skillassessment.db.QuizRepository;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.util.CategoryNames;
import net.golbarg.skillassessment.util.UiUtils;

import java.util.HashSet;
import java.util.Set;

public class CategoryAdapter extends ListAdapter<Category, CategoryAdapter.Holder> {

    public interface Listener {
        void onOpen(Category category);

        void onUnlock(Category category);
    }

    private final Listener listener;
    private final Set<Integer> unlocking = new HashSet<>();

    public CategoryAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    public void setUnlocking(int categoryId, boolean inProgress) {
        boolean changed = inProgress ? unlocking.add(categoryId) : unlocking.remove(categoryId);
        if (!changed) return;
        for (int i = 0; i < getItemCount(); i++) {
            if (getItem(i).getId() == categoryId) notifyItemChanged(i);
        }
    }

    public boolean isBusy() {
        return !unlocking.isEmpty();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemCategoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    class Holder extends RecyclerView.ViewHolder {
        private final ItemCategoryBinding b;

        Holder(ItemCategoryBinding binding) {
            super(binding.getRoot());
            this.b = binding;
        }

        void bind(Category category) {
            Context context = b.getRoot().getContext();
            boolean dark = UiUtils.isNightMode(context);

            GradientDrawable badge = new GradientDrawable();
            badge.setCornerRadius(UiUtils.dp(context, 14));
            badge.setColor(CategoryNames.badgeBackground(category.getSlug(), dark));
            b.txtBadge.setBackground(badge);
            b.txtBadge.setTextColor(CategoryNames.badgeForeground(category.getSlug(), dark));
            b.txtBadge.setText(CategoryNames.monogram(category.getSlug()));

            b.txtTitle.setText(category.getDisplayName());
            String count = context.getResources().getQuantityString(R.plurals.question_count, category.getNumberOfQuestion(), category.getNumberOfQuestion());
            StringBuilder subtitle = new StringBuilder(count);
            if (category.isUnlocked() && category.getMastered() > 0) {
                subtitle.append(" · ").append(category.getMasteryPercent() >= 100
                        ? context.getString(R.string.mastered_badge)
                        : context.getString(R.string.mastery_format, category.getMasteryPercent()));
            }
            if (category.getBestScore() >= 0) subtitle.append(" · ").append(context.getString(R.string.best_score, category.getBestScore()));
            b.txtSubtitle.setText(subtitle);

            // The bar shows mastery: how much of the topic has been answered correctly at least once.
            if (category.isUnlocked() && category.getAttempts() > 0) {
                int mastery = category.getMasteryPercent();
                int color = mastery >= 100 ? ContextCompat.getColor(context, R.color.coin)
                        : ContextCompat.getColor(context, R.color.correct);
                b.progressBest.setVisibility(View.VISIBLE);
                b.progressBest.setIndicatorColor(color);
                b.progressBest.setProgressCompat(Math.max(2, mastery), false);
            } else {
                b.progressBest.setVisibility(View.GONE);
            }

            boolean busy = unlocking.contains(category.getId());
            b.progressUnlock.setVisibility(busy ? View.VISIBLE : View.GONE);
            if (category.isUnlocked()) {
                b.btnUnlock.setVisibility(View.GONE);
                b.imgChevron.setVisibility(View.VISIBLE);
            } else {
                b.imgChevron.setVisibility(View.GONE);
                b.btnUnlock.setVisibility(View.VISIBLE);
                b.btnUnlock.setEnabled(!busy);
                boolean premium = BillingManager.isPremium(context);
                b.btnUnlock.setText(busy ? context.getString(R.string.unlocking)
                        : premium ? context.getString(R.string.unlock_free) : context.getString(R.string.unlock_cost, QuizRepository.UNLOCK_COST));
                b.btnUnlock.setIconResource(premium ? R.drawable.ic_lock_open : R.drawable.ic_coin);
                b.btnUnlock.setIconTint(premium ? ColorStateList.valueOf(UiUtils.color(context, com.google.android.material.R.attr.colorOnSecondaryContainer)) : null);
                b.btnUnlock.setOnClickListener(v -> listener.onUnlock(category));
            }
            b.card.setOnClickListener(v -> {
                if (busy) return;
                if (category.isUnlocked()) listener.onOpen(category);
                else listener.onUnlock(category);
            });
        }
    }

    private static final DiffUtil.ItemCallback<Category> DIFF = new DiffUtil.ItemCallback<Category>() {
        @Override
        public boolean areItemsTheSame(@NonNull Category a, @NonNull Category b) {
            return a.getId() == b.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Category a, @NonNull Category b) {
            return a.isUnlocked() == b.isUnlocked() && a.getBestScore() == b.getBestScore() && a.getAttempts() == b.getAttempts()
                    && a.getMastered() == b.getMastered();
        }
    };
}

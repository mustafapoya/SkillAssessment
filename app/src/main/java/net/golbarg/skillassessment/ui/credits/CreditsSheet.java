package net.golbarg.skillassessment.ui.credits;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.ads.AdManager;
import net.golbarg.skillassessment.billing.BillingManager;
import net.golbarg.skillassessment.databinding.SheetCreditsBinding;
import net.golbarg.skillassessment.db.QuizRepository;
import net.golbarg.skillassessment.ui.widget.AppBottomSheet;
import net.golbarg.skillassessment.util.Async;
import net.golbarg.skillassessment.util.Feedback;

/** Shows the coin balance and lets the user earn coins with a rewarded video. */
public class CreditsSheet extends AppBottomSheet {
    public static final String TAG = "CreditsSheet";
    /** Fragment result key posted to the parent whenever the balance changes. */
    public static final String RESULT_KEY = "coins_changed";

    private SheetCreditsBinding binding;
    private QuizRepository repository;
    private boolean earnedThisSession;
    private final AdManager.RewardedListener rewardedListener = this::onRewardedLoaded;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SheetCreditsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = QuizRepository.get(requireContext());
        binding.txtDescription.setText(getString(R.string.coins_desc, QuizRepository.UNLOCK_COST, QuizRepository.REWARD_CREDIT));
        binding.btnWatch.setOnClickListener(v -> watch());
        refreshBalance();
        BillingManager billing = BillingManager.get(requireContext());
        billing.getState().observe(getViewLifecycleOwner(), this::bindPremium);
        billing.refresh();
        binding.btnPremium.setOnClickListener(v -> billing.purchase(requireActivity(), success -> {
            if (binding == null) return;
            if (success) {
                Feedback.play(Feedback.Sound.UNLOCK);
                Toast.makeText(requireContext(), R.string.premium_thanks, Toast.LENGTH_LONG).show();
                getParentFragmentManager().setFragmentResult(RESULT_KEY, new Bundle());
            }
        }));
        if (BillingManager.isPremium(requireContext())) {
            binding.btnWatch.setVisibility(View.GONE);
        } else {
            requestAd();
        }
    }

    private void bindPremium(BillingManager.State state) {
        if (binding == null || state == null) return;
        binding.cardPremium.setVisibility(BillingManager.FOR_SALE || state.premium ? View.VISIBLE : View.GONE);
        if (state.premium) {
            binding.txtPremiumTitle.setText(R.string.premium_active);
            binding.txtPremiumDesc.setText(R.string.premium_active_desc);
            binding.btnPremium.setVisibility(View.GONE);
            binding.btnWatch.setVisibility(View.GONE);
        } else {
            binding.btnPremium.setVisibility(View.VISIBLE);
            binding.btnPremium.setEnabled(state.available);
            binding.btnPremium.setText(state.price != null ? getString(R.string.premium_buy, state.price) : getString(R.string.premium_buy_no_price));
            binding.txtPremiumDesc.setText(state.available ? getString(R.string.premium_desc) : getString(R.string.premium_unavailable));
        }
    }

    private void refreshBalance() {
        Async.run(getViewLifecycleOwner(), repository::getCredits, credits -> {
            if (binding != null && credits != null) binding.txtBalance.setText(String.valueOf(credits));
        });
    }

    private void requestAd() {
        if (AdManager.isRewardedReady()) {
            showReady();
            return;
        }
        binding.btnWatch.setEnabled(false);
        binding.btnWatch.setText(R.string.loading_ad);
        if (!earnedThisSession) binding.txtStatus.setVisibility(View.GONE);
        AdManager.loadRewarded(requireContext(), rewardedListener);
    }

    private void onRewardedLoaded(boolean available) {
        if (binding == null) return;
        if (available) {
            showReady();
        } else {
            binding.btnWatch.setEnabled(true);
            binding.btnWatch.setText(R.string.try_again);
            binding.txtStatus.setVisibility(View.VISIBLE);
            binding.txtStatus.setText(R.string.ad_not_available);
        }
    }

    private void showReady() {
        binding.btnWatch.setEnabled(true);
        binding.btnWatch.setText(getString(R.string.watch_ad, QuizRepository.REWARD_CREDIT));
        if (!earnedThisSession) binding.txtStatus.setVisibility(View.GONE);
    }

    private void watch() {
        if (!AdManager.isRewardedReady()) {
            requestAd();
            return;
        }
        binding.btnWatch.setEnabled(false);
        // Captured now: the reward can arrive after the sheet has been dismissed.
        FragmentManager parent = getParentFragmentManager();
        AdManager.showRewarded(requireActivity(), () -> Async.run(null, () -> repository.addCredits(QuizRepository.REWARD_CREDIT), credits -> {
            if (!parent.isDestroyed()) parent.setFragmentResult(RESULT_KEY, new Bundle());
            Feedback.play(Feedback.Sound.COIN);
            if (binding == null || credits == null) return;
            earnedThisSession = true;
            binding.txtBalance.setText(String.valueOf(credits));
            binding.txtBalance.setScaleX(1.3f);
            binding.txtBalance.setScaleY(1.3f);
            binding.txtBalance.animate().scaleX(1f).scaleY(1f).setDuration(400)
                    .setInterpolator(new OvershootInterpolator(3f)).start();
            binding.txtStatus.setVisibility(View.VISIBLE);
            binding.txtStatus.setText(getString(R.string.coins_earned, QuizRepository.REWARD_CREDIT));
        }), () -> {
            if (binding != null) requestAd();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        AdManager.cancelRewardedListener(rewardedListener);
        binding = null;
    }
}

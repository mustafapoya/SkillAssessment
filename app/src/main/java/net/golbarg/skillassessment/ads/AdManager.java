package net.golbarg.skillassessment.ads;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import net.golbarg.skillassessment.BuildConfig;
import net.golbarg.skillassessment.billing.BillingManager;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central place for every ad unit. Debug builds always use Google's test units so development
 * traffic never reaches the production AdMob account.
 */
public final class AdManager {
    private static final String TAG = "AdManager";

    private static final boolean USE_TEST_ADS = BuildConfig.DEBUG;
    private static final String TEST_BANNER = "ca-app-pub-3940256099942544/9214589741";
    private static final String TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712";
    private static final String TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917";

    private static final String REWARDED_COINS = "ca-app-pub-8976959600358837/5452469083";

    /** Minimum gap between two interstitials, so a test is never sandwiched between ads. */
    private static final long INTERSTITIAL_INTERVAL_MS = 3 * 60 * 1000L;

    public enum Banner {
        HOME("ca-app-pub-8976959600358837/8503467396"),
        BOOKMARK("ca-app-pub-8976959600358837/8078632423"),
        PROFILE("ca-app-pub-8976959600358837/7190385727"),
        QUESTION("ca-app-pub-8976959600358837/9485430471"),
        RESULT("ca-app-pub-8976959600358837/6765550752");

        final String unitId;

        Banner(String unitId) {
            this.unitId = unitId;
        }
    }

    public enum Interstitial {
        TEST_START("ca-app-pub-8976959600358837/8499651861"),
        TEST_FINISH("ca-app-pub-8976959600358837/7186570198");

        final String unitId;

        Interstitial(String unitId) {
            this.unitId = unitId;
        }
    }

    public interface RewardedListener {
        void onLoaded(boolean available);
    }

    private static final String PREFS = "ads";
    private static final String KEY_LAST_INTERSTITIAL = "last_interstitial_at";

    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final Map<Interstitial, InterstitialAd> interstitials = new EnumMap<>(Interstitial.class);
    private static final Map<Interstitial, Boolean> interstitialLoading = new EnumMap<>(Interstitial.class);
    private static long lastInterstitialAt = 0;
    private static RewardedAd rewardedAd;
    private static boolean rewardedLoading;

    private AdManager() {
    }

    public static void init(Context context) {
        if (initialized.compareAndSet(false, true)) {
            new Thread(() -> MobileAds.initialize(context.getApplicationContext(), status -> {
            }), "ads-init").start();
        }
    }

    // region Banner

    /**
     * Loads an anchored adaptive banner into {@code container}. The container stays collapsed until
     * an ad is actually available, so there is never an empty grey strip.
     */
    public static void loadBanner(@NonNull LifecycleOwner owner, @NonNull FrameLayout container, @NonNull Banner banner) {
        Context context = container.getContext();
        container.setVisibility(View.GONE);
        container.removeAllViews();
        if (BillingManager.isPremium(context)) return;

        AdView adView = new AdView(context);
        adView.setAdUnitId(USE_TEST_ADS ? TEST_BANNER : banner.unitId);
        adView.setAdSize(adaptiveSize(context));
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                container.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                container.setVisibility(View.GONE);
                Log.d(TAG, "Banner failed: " + error.getMessage());
            }
        });
        container.addView(adView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        adView.loadAd(new AdRequest.Builder().build());

        owner.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onResume(@NonNull LifecycleOwner o) {
                adView.resume();
            }

            @Override
            public void onPause(@NonNull LifecycleOwner o) {
                adView.pause();
            }

            @Override
            public void onDestroy(@NonNull LifecycleOwner o) {
                adView.destroy();
                o.getLifecycle().removeObserver(this);
            }
        });
    }

    private static AdSize adaptiveSize(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int widthDp = (int) (metrics.widthPixels / metrics.density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp);
    }

    // endregion

    // region Interstitial

    public static void preloadInterstitial(Context context, Interstitial which) {
        if (BillingManager.isPremium(context)) return;
        if (interstitials.get(which) != null || Boolean.TRUE.equals(interstitialLoading.get(which))) return;
        interstitialLoading.put(which, true);
        InterstitialAd.load(context.getApplicationContext(), USE_TEST_ADS ? TEST_INTERSTITIAL : which.unitId,
                new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitials.put(which, ad);
                        interstitialLoading.put(which, false);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        interstitialLoading.put(which, false);
                        Log.d(TAG, "Interstitial failed: " + error.getMessage());
                    }
                });
    }

    /**
     * Shows a preloaded interstitial if one is ready and the frequency cap allows it, then runs
     * {@code onDone}. {@code onDone} always runs exactly once.
     */
    public static void showInterstitial(Activity activity, Interstitial which, Runnable onDone) {
        if (BillingManager.isPremium(activity)) {
            onDone.run();
            return;
        }
        InterstitialAd ad = interstitials.get(which);
        android.content.SharedPreferences prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long last = Math.max(lastInterstitialAt, prefs.getLong(KEY_LAST_INTERSTITIAL, 0));
        long now = System.currentTimeMillis();
        boolean capped = last != 0 && now - last < INTERSTITIAL_INTERVAL_MS && now >= last;
        if (ad == null || capped || activity.isFinishing()) {
            onDone.run();
            return;
        }
        interstitials.remove(which);
        final AtomicBoolean done = new AtomicBoolean(false);
        Runnable finish = () -> {
            if (done.compareAndSet(false, true)) onDone.run();
        };
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                finish.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                finish.run();
            }
        });
        lastInterstitialAt = now;
        prefs.edit().putLong(KEY_LAST_INTERSTITIAL, now).apply();
        ad.show(activity);
    }

    // endregion

    // region Rewarded

    public static boolean isRewardedReady() {
        return rewardedAd != null;
    }

    public static void loadRewarded(Context context, @Nullable RewardedListener listener) {
        if (rewardedAd != null) {
            if (listener != null) listener.onLoaded(true);
            return;
        }
        if (rewardedLoading) return;
        rewardedLoading = true;
        RewardedAd.load(context.getApplicationContext(), USE_TEST_ADS ? TEST_REWARDED : REWARDED_COINS,
                new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedLoading = false;
                        rewardedAd = ad;
                        if (listener != null) listener.onLoaded(true);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        rewardedLoading = false;
                        rewardedAd = null;
                        Log.d(TAG, "Rewarded failed: " + error.getMessage());
                        if (listener != null) listener.onLoaded(false);
                    }
                });
    }

    /**
     * @param onReward runs on the main thread when the user earns the reward.
     * @param onClosed runs when the ad is dismissed (whether or not it was rewarded).
     */
    public static void showRewarded(Activity activity, Runnable onReward, Runnable onClosed) {
        RewardedAd ad = rewardedAd;
        if (ad == null) {
            onClosed.run();
            return;
        }
        rewardedAd = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                onClosed.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                onClosed.run();
            }
        });
        ad.show(activity, rewardItem -> onReward.run());
    }

    // endregion
}

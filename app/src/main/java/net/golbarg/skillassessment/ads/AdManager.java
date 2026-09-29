package net.golbarg.skillassessment.ads;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import net.golbarg.skillassessment.BuildConfig;
import net.golbarg.skillassessment.billing.BillingManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central place for every ad unit and every ad rule.
 *
 * <ul>
 *     <li>No ad is requested until Google's consent flow (UMP) says ads may be requested.</li>
 *     <li>Banners only appear on browsing screens, never next to quiz controls.</li>
 *     <li>Interstitials only appear at the natural break after a finished test, never before
 *     content, and are frequency capped (see {@link #showInterstitialAfterTest}).</li>
 *     <li>Rewarded videos are always opt-in and name their reward up front.</li>
 *     <li>Premium users never see or load ads.</li>
 * </ul>
 * Debug builds always use Google's test units so development traffic never reaches the production account.
 */
public final class AdManager {
    private static final String TAG = "AdManager";

    private static final boolean USE_TEST_ADS = BuildConfig.DEBUG;
    private static final String TEST_BANNER = "ca-app-pub-3940256099942544/9214589741";
    private static final String TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712";
    private static final String TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917";

    private static final String INTERSTITIAL_TEST_FINISH = "ca-app-pub-8976959600358837/7186570198";
    private static final String REWARDED_COINS = "ca-app-pub-8976959600358837/5452469083";

    /** The first few tests a new user takes are always ad-free. */
    private static final int GRACE_TESTS = 2;
    /** Finished tests needed between two interstitials. */
    private static final int TESTS_BETWEEN_INTERSTITIALS = 2;
    /** A test abandoned after a couple of answers isn't a natural break worth an ad. */
    private static final int MIN_ANSWERED_FOR_INTERSTITIAL = 3;
    /** Minimum gap after any full-screen ad (interstitial or rewarded). */
    private static final long FULLSCREEN_INTERVAL_MS = 3 * 60 * 1000L;
    /** Google recommends not showing full-screen ads that were loaded more than an hour ago. */
    private static final long AD_EXPIRY_MS = 60 * 60 * 1000L;

    public enum Banner {
        HOME("ca-app-pub-8976959600358837/8503467396"),
        BOOKMARK("ca-app-pub-8976959600358837/8078632423"),
        PROFILE("ca-app-pub-8976959600358837/7190385727"),
        RESULT("ca-app-pub-8976959600358837/6765550752");

        final String unitId;

        Banner(String unitId) {
            this.unitId = unitId;
        }
    }

    public interface RewardedListener {
        void onLoaded(boolean available);
    }

    private static final String PREFS = "ads";
    private static final String KEY_LAST_FULLSCREEN = "last_interstitial_at";
    private static final String KEY_TESTS_FINISHED = "tests_finished";
    private static final String KEY_TESTS_SINCE_INTERSTITIAL = "tests_since_interstitial";

    private static final Handler main = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean sdkInitialized = new AtomicBoolean(false);
    private static final AtomicBoolean consentRequested = new AtomicBoolean(false);
    /** Ad work queued until consent allows requesting ads. Main thread only. */
    private static final List<Runnable> pendingUntilReady = new ArrayList<>();

    private static InterstitialAd interstitial;
    private static long interstitialLoadedAt;
    private static boolean interstitialLoading;

    private static RewardedAd rewardedAd;
    private static long rewardedLoadedAt;
    private static boolean rewardedLoading;
    private static final List<RewardedListener> rewardedListeners = new ArrayList<>();

    private AdManager() {
    }

    // region Consent & initialization

    /** Called from Application: starts the SDK straight away when consent from a previous session allows it. */
    public static void init(Context context) {
        if (canRequestAds(context)) initializeSdk(context);
    }

    /**
     * Refreshes consent status and, where required (EEA, UK, some US states), shows Google's consent
     * form. Call once from the first interactive activity.
     */
    @MainThread
    public static void gatherConsent(@NonNull Activity activity) {
        if (!consentRequested.compareAndSet(false, true)) return;
        ConsentInformation info = UserMessagingPlatform.getConsentInformation(activity);
        info.requestConsentInfoUpdate(activity, new ConsentRequestParameters.Builder().build(),
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, formError -> {
                    if (formError != null) Log.w(TAG, "Consent form: " + formError.getMessage());
                    onConsentResolved(activity);
                }),
                requestError -> {
                    Log.w(TAG, "Consent info: " + requestError.getMessage());
                    onConsentResolved(activity);
                });
        // Consent from a previous session may already allow ads while the update runs.
        onConsentResolved(activity);
    }

    public static boolean canRequestAds(Context context) {
        return UserMessagingPlatform.getConsentInformation(context).canRequestAds();
    }

    /** Whether the settings screen must offer a way to review ad privacy choices. */
    public static boolean isPrivacyOptionsRequired(Context context) {
        return UserMessagingPlatform.getConsentInformation(context).getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    public static void showPrivacyOptions(Activity activity, @Nullable Runnable onDone) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, formError -> {
            if (formError != null) Log.w(TAG, "Privacy options: " + formError.getMessage());
            onConsentResolved(activity);
            if (onDone != null) onDone.run();
        });
    }

    private static void onConsentResolved(Context context) {
        if (!canRequestAds(context)) return;
        initializeSdk(context);
        main.post(() -> {
            List<Runnable> queued = new ArrayList<>(pendingUntilReady);
            pendingUntilReady.clear();
            for (Runnable r : queued) r.run();
        });
    }

    private static void initializeSdk(Context context) {
        if (!sdkInitialized.compareAndSet(false, true)) return;
        MobileAds.setRequestConfiguration(new RequestConfiguration.Builder()
                // The store listing is rated Everyone (3+), so ads are kept to PG content.
                .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_PG)
                .build());
        Context app = context.getApplicationContext();
        new Thread(() -> MobileAds.initialize(app, status -> {
        }), "ads-init").start();
    }

    /** Runs {@code work} now if ads may be requested, otherwise once consent allows it. */
    @MainThread
    private static void whenReady(Context context, Runnable work) {
        if (canRequestAds(context)) {
            initializeSdk(context);
            work.run();
        } else {
            pendingUntilReady.add(work);
        }
    }

    // endregion

    // region Banner

    /**
     * Loads an anchored adaptive banner into {@code container}. The container stays collapsed until
     * an ad is actually available, so there is never an empty strip, and collapses again as soon as
     * Premium is bought.
     */
    @MainThread
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
                if (!BillingManager.isPremium(context)) container.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                container.setVisibility(View.GONE);
                Log.d(TAG, "Banner failed: " + error.getMessage());
            }
        });
        container.addView(adView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        whenReady(context, () -> {
            if (owner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.CREATED)) {
                adView.loadAd(new AdRequest.Builder().build());
            }
        });

        BillingManager.get(context).getState().observe(owner, state -> {
            if (state != null && state.premium) {
                container.setVisibility(View.GONE);
                container.removeAllViews();
            }
        });
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

    /** Loads the post-test interstitial ahead of time, but only if the next finished test could show it. */
    @MainThread
    public static void preloadInterstitial(Context context) {
        if (BillingManager.isPremium(context) || testsFinished(context) + 1 <= GRACE_TESTS) return;
        if (isFresh(interstitial, interstitialLoadedAt) || interstitialLoading) return;
        Context app = context.getApplicationContext();
        whenReady(app, () -> {
            if (interstitialLoading) return;
            interstitialLoading = true;
            InterstitialAd.load(app, USE_TEST_ADS ? TEST_INTERSTITIAL : INTERSTITIAL_TEST_FINISH,
                    new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd ad) {
                            interstitial = ad;
                            interstitialLoadedAt = SystemClock.elapsedRealtime();
                            interstitialLoading = false;
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError error) {
                            interstitialLoading = false;
                            Log.d(TAG, "Interstitial failed: " + error.getMessage());
                        }
                    });
        });
    }

    /**
     * Records a finished test and shows the interstitial if every rule allows it, then runs
     * {@code onDone} exactly once. The rules: no ads for the first {@value #GRACE_TESTS} tests, at
     * least {@value #TESTS_BETWEEN_INTERSTITIALS} tests between ads, a minimum time gap after any
     * full-screen ad, and never after a test with fewer than {@value #MIN_ANSWERED_FOR_INTERSTITIAL}
     * answers.
     */
    @MainThread
    public static void showInterstitialAfterTest(Activity activity, int answered, Runnable onDone) {
        SharedPreferences prefs = prefs(activity);
        int finished = prefs.getInt(KEY_TESTS_FINISHED, 0) + 1;
        int since = prefs.getInt(KEY_TESTS_SINCE_INTERSTITIAL, 0) + 1;
        prefs.edit().putInt(KEY_TESTS_FINISHED, finished).putInt(KEY_TESTS_SINCE_INTERSTITIAL, since).apply();

        InterstitialAd ad = interstitial;
        boolean allowed = !BillingManager.isPremium(activity)
                && canRequestAds(activity)
                && isFresh(ad, interstitialLoadedAt)
                && finished > GRACE_TESTS
                && since >= TESTS_BETWEEN_INTERSTITIALS
                && answered >= MIN_ANSWERED_FOR_INTERSTITIAL
                && !recentlyShowedFullscreen(activity)
                && !activity.isFinishing();
        if (!allowed) {
            onDone.run();
            return;
        }
        interstitial = null;
        AtomicBoolean done = new AtomicBoolean(false);
        Runnable finish = () -> {
            if (done.compareAndSet(false, true)) onDone.run();
        };
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                finish.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                finish.run();
            }
        });
        prefs.edit().putInt(KEY_TESTS_SINCE_INTERSTITIAL, 0).apply();
        markFullscreenShown(activity);
        ad.show(activity);
    }

    // endregion

    // region Rewarded

    public static boolean isRewardedReady() {
        return isFresh(rewardedAd, rewardedLoadedAt);
    }

    /** Loads a rewarded video. Every listener hears back exactly once, even if a load is already running. */
    @MainThread
    public static void loadRewarded(Context context, @Nullable RewardedListener listener) {
        if (isRewardedReady()) {
            if (listener != null) listener.onLoaded(true);
            return;
        }
        if (listener != null) rewardedListeners.add(listener);
        if (rewardedLoading) return;
        rewardedLoading = true;
        rewardedAd = null;
        Context app = context.getApplicationContext();
        if (!canRequestAds(app)) {
            // Without consent there is nothing to load; report it rather than waiting forever.
            rewardedLoading = false;
            notifyRewarded(false);
            return;
        }
        initializeSdk(app);
        RewardedAd.load(app, USE_TEST_ADS ? TEST_REWARDED : REWARDED_COINS,
                new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedLoading = false;
                        rewardedAd = ad;
                        rewardedLoadedAt = SystemClock.elapsedRealtime();
                        notifyRewarded(true);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        rewardedLoading = false;
                        rewardedAd = null;
                        Log.d(TAG, "Rewarded failed: " + error.getMessage());
                        notifyRewarded(false);
                    }
                });
    }

    private static void notifyRewarded(boolean available) {
        List<RewardedListener> listeners = new ArrayList<>(rewardedListeners);
        rewardedListeners.clear();
        for (RewardedListener l : listeners) l.onLoaded(available);
    }

    /** Forgets a listener whose screen went away before the load finished. */
    @MainThread
    public static void cancelRewardedListener(RewardedListener listener) {
        rewardedListeners.remove(listener);
    }

    /**
     * @param onReward runs on the main thread when the user earns the reward.
     * @param onClosed runs when the ad is dismissed (whether or not it was rewarded).
     */
    @MainThread
    public static void showRewarded(Activity activity, Runnable onReward, Runnable onClosed) {
        RewardedAd ad = rewardedAd;
        if (!isFresh(ad, rewardedLoadedAt)) {
            rewardedAd = null;
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
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                onClosed.run();
            }
        });
        // An opted-in video still counts: don't follow it with an interstitial straight away.
        markFullscreenShown(activity);
        ad.show(activity, rewardItem -> onReward.run());
    }

    // endregion

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static int testsFinished(Context context) {
        return prefs(context).getInt(KEY_TESTS_FINISHED, 0);
    }

    private static boolean isFresh(@Nullable Object ad, long loadedAt) {
        return ad != null && SystemClock.elapsedRealtime() - loadedAt < AD_EXPIRY_MS;
    }

    private static boolean recentlyShowedFullscreen(Context context) {
        long last = prefs(context).getLong(KEY_LAST_FULLSCREEN, 0);
        long now = System.currentTimeMillis();
        // A clock set backwards shouldn't block ads forever.
        return last != 0 && now >= last && now - last < FULLSCREEN_INTERVAL_MS;
    }

    private static void markFullscreenShown(Context context) {
        prefs(context).edit().putLong(KEY_LAST_FULLSCREEN, System.currentTimeMillis()).apply();
    }
}

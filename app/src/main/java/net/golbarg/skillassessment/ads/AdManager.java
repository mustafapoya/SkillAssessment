package net.golbarg.skillassessment.ads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

import net.golbarg.skillassessment.billing.BillingManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central place for every ad unit and every ad rule.
 *
 * <ul>
 *     <li>No ad is requested until Google's consent flow (UMP) says ads may be requested.</li>
 *     <li>Banners never sit next to quiz controls: on the test screen the banner goes under the top
 *     bar, away from the answers and the Check button.</li>
 *     <li>Interstitials only appear at natural breaks, before a test starts or after it finishes,
 *     and both placements share one frequency budget (see {@link #frequencyAllows}).</li>
 *     <li>Rewarded videos are always opt-in and name their reward up front.</li>
 *     <li>Premium users never see or load ads.</li>
 * </ul>
 * {@link #ADS_ENABLED} and {@link #USE_TEST_ADS} below switch ads as a whole.
 */
public final class AdManager {
    private static final String TAG = "AdManager";

    // false = no ad is loaded or shown anywhere, and rewards (coins, 50/50 hint, streak restore) are given without a video
    public static final boolean ADS_ENABLED = true;
    // true = Google's test ad units (use while developing), false = the real units of the AdMob account
    public static final boolean USE_TEST_ADS = false;

    // real ad units
    private static final String REAL_BANNER_HOME = "ca-app-pub-8976959600358837/8503467396";
    private static final String REAL_BANNER_BOOKMARK = "ca-app-pub-8976959600358837/8078632423";
    private static final String REAL_BANNER_PROFILE = "ca-app-pub-8976959600358837/7190385727";
    private static final String REAL_BANNER_QUESTION_RESULT = "ca-app-pub-8976959600358837/6765550752";
    private static final String REAL_BANNER_QUESTION = "ca-app-pub-8976959600358837/9485430471";
    private static final String REAL_INTERSTITIAL_QUESTION = "ca-app-pub-8976959600358837/8499651861";
    private static final String REAL_INTERSTITIAL_QUESTION_RESULT = "ca-app-pub-8976959600358837/7186570198";
    private static final String REAL_REWARD_CREDIT = "ca-app-pub-8976959600358837/5452469083";
    private static final String REAL_REWARD_LIFE = "ca-app-pub-8976959600358837/1966033109";

    // Google's test ad units: https://developers.google.com/admob/android/test-ads
    private static final String TEST_BANNER = "ca-app-pub-3940256099942544/9214589741";
    private static final String TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712";
    private static final String TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917";

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
        HOME(REAL_BANNER_HOME),
        BOOKMARK(REAL_BANNER_BOOKMARK),
        PROFILE(REAL_BANNER_PROFILE),
        RESULT(REAL_BANNER_QUESTION_RESULT),
        QUESTION(REAL_BANNER_QUESTION);

        private final String realUnitId;

        Banner(String realUnitId) {
            this.realUnitId = realUnitId;
        }

        String unitId() {
            return USE_TEST_ADS ? TEST_BANNER : realUnitId;
        }
    }

    /** Interstitial placements, each with its own preloaded ad. Fields are main thread only. */
    public enum Interstitial {
        /** Before a test the user has just chosen. */
        TEST_START(REAL_INTERSTITIAL_QUESTION),
        /** After a finished test, before its result. */
        TEST_FINISH(REAL_INTERSTITIAL_QUESTION_RESULT);

        private final String realUnitId;
        private InterstitialAd ad;
        private long loadedAt;
        private boolean loading;

        Interstitial(String realUnitId) {
            this.realUnitId = realUnitId;
        }

        String unitId() {
            return USE_TEST_ADS ? TEST_INTERSTITIAL : realUnitId;
        }
    }

    /** Rewarded placements, split so AdMob reports each kind of reward separately. Fields are main thread only. */
    public enum Rewarded {
        /** Coins for unlocking topics. */
        COINS(REAL_REWARD_CREDIT),
        /** Second chances: the 50/50 hint in a test and restoring a lost streak. */
        LIFE(REAL_REWARD_LIFE);

        private final String realUnitId;
        private RewardedAd ad;
        private long loadedAt;
        private boolean loading;
        private final List<RewardedListener> listeners = new ArrayList<>();

        Rewarded(String realUnitId) {
            this.realUnitId = realUnitId;
        }

        String unitId() {
            return USE_TEST_ADS ? TEST_REWARDED : realUnitId;
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

    private AdManager() {
    }

    // region Consent & initialization

    /** Called from Application: starts the SDK straight away when consent from a previous session allows it. */
    public static void init(Context context) {
        if (ADS_ENABLED && canRequestAds(context)) initializeSdk(context);
    }

    /**
     * Refreshes consent status and, where required (EEA, UK, some US states), shows Google's consent
     * form. Call once from the first interactive activity.
     */
    @MainThread
    public static void gatherConsent(@NonNull Activity activity) {
        // With ads off there is nothing to ask consent for.
        if (!ADS_ENABLED || !consentRequested.compareAndSet(false, true)) return;
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
        if (!ADS_ENABLED || BillingManager.isPremium(context)) return;

        AdView adView = new AdView(context);
        adView.setAdUnitId(banner.unitId());
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
            // Called from onCreate/onViewCreated, where the owner is still INITIALIZED: only skip
            // the request once the screen is gone (consent can arrive after that).
            if (owner.getLifecycle().getCurrentState() != Lifecycle.State.DESTROYED) {
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

    /** Loads an interstitial ahead of time, but only if the test counters could let it show next. */
    @MainThread
    public static void preloadInterstitial(Context context, Interstitial placement) {
        if (!ADS_ENABLED || BillingManager.isPremium(context) || !frequencyAllows(context, placement)) return;
        if (isFresh(placement.ad, placement.loadedAt) || placement.loading) return;
        Context app = context.getApplicationContext();
        whenReady(app, () -> {
            if (placement.loading) return;
            placement.loading = true;
            InterstitialAd.load(app, placement.unitId(),
                    new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd ad) {
                            placement.ad = ad;
                            placement.loadedAt = SystemClock.elapsedRealtime();
                            placement.loading = false;
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError error) {
                            placement.loading = false;
                            Log.d(TAG, "Interstitial failed: " + error.getMessage());
                        }
                    });
        });
    }

    /**
     * Shows the pre-test interstitial if the rules allow it, then runs {@code onDone} (which starts
     * the test) exactly once.
     */
    @MainThread
    public static void showInterstitialBeforeTest(Activity activity, Runnable onDone) {
        if (!frequencyAllows(activity, Interstitial.TEST_START) || !showInterstitial(activity, Interstitial.TEST_START, onDone)) {
            onDone.run();
        }
    }

    /** Starts a test, after the pre-test interstitial when one is due. */
    @MainThread
    public static void startTest(Activity activity, Intent test) {
        showInterstitialBeforeTest(activity, () -> activity.startActivity(test));
    }

    /**
     * Records a finished test and shows the interstitial if every rule allows it, then runs
     * {@code onDone} exactly once. On top of {@link #frequencyAllows}, it never follows a test with
     * fewer than {@value #MIN_ANSWERED_FOR_INTERSTITIAL} answers.
     */
    @MainThread
    public static void showInterstitialAfterTest(Activity activity, int answered, Runnable onDone) {
        boolean allowed = answered >= MIN_ANSWERED_FOR_INTERSTITIAL && frequencyAllows(activity, Interstitial.TEST_FINISH);
        SharedPreferences prefs = prefs(activity);
        prefs.edit()
                .putInt(KEY_TESTS_FINISHED, prefs.getInt(KEY_TESTS_FINISHED, 0) + 1)
                .putInt(KEY_TESTS_SINCE_INTERSTITIAL, prefs.getInt(KEY_TESTS_SINCE_INTERSTITIAL, 0) + 1)
                .apply();
        if (!allowed || !showInterstitial(activity, Interstitial.TEST_FINISH, onDone)) onDone.run();
    }

    /**
     * The test counters, read before the current test is counted: no ads in the first
     * {@value #GRACE_TESTS} tests and at least {@value #TESTS_BETWEEN_INTERSTITIALS} finished tests
     * between two interstitials, whichever placement showed them.
     */
    private static boolean frequencyAllows(Context context, Interstitial placement) {
        SharedPreferences prefs = prefs(context);
        int test = prefs.getInt(KEY_TESTS_FINISHED, 0) + 1;
        // At the end of a test, that test already counts towards the gap.
        int since = prefs.getInt(KEY_TESTS_SINCE_INTERSTITIAL, 0) + (placement == Interstitial.TEST_FINISH ? 1 : 0);
        return test > GRACE_TESTS && since >= TESTS_BETWEEN_INTERSTITIALS;
    }

    /**
     * Shows the preloaded ad unless Premium, consent, freshness or the time gap after the last
     * full-screen ad rules it out. Returns false, without running {@code onDone}, when nothing was shown.
     */
    private static boolean showInterstitial(Activity activity, Interstitial placement, Runnable onDone) {
        InterstitialAd ad = placement.ad;
        if (!ADS_ENABLED || BillingManager.isPremium(activity) || !canRequestAds(activity) || !isFresh(ad, placement.loadedAt)
                || recentlyShowedFullscreen(activity) || activity.isFinishing()) {
            return false;
        }
        placement.ad = null;
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
        prefs(activity).edit().putInt(KEY_TESTS_SINCE_INTERSTITIAL, 0).apply();
        markFullscreenShown(activity);
        ad.show(activity);
        return true;
    }

    // endregion

    // region Rewarded

    public static boolean isRewardedReady(Rewarded placement) {
        return !ADS_ENABLED || isFresh(placement.ad, placement.loadedAt);
    }

    /** Loads a rewarded video. Every listener hears back exactly once, even if a load is already running. */
    @MainThread
    public static void loadRewarded(Context context, Rewarded placement, @Nullable RewardedListener listener) {
        // With ads off, isRewardedReady is always true and showRewarded gives the reward directly.
        if (isRewardedReady(placement)) {
            if (listener != null) listener.onLoaded(true);
            return;
        }
        if (listener != null) placement.listeners.add(listener);
        if (placement.loading) return;
        placement.loading = true;
        placement.ad = null;
        Context app = context.getApplicationContext();
        if (!canRequestAds(app)) {
            // Without consent there is nothing to load; report it rather than waiting forever.
            placement.loading = false;
            notifyRewarded(placement, false);
            return;
        }
        initializeSdk(app);
        RewardedAd.load(app, placement.unitId(),
                new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        placement.loading = false;
                        placement.ad = ad;
                        placement.loadedAt = SystemClock.elapsedRealtime();
                        notifyRewarded(placement, true);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        placement.loading = false;
                        placement.ad = null;
                        Log.d(TAG, "Rewarded failed: " + error.getMessage());
                        notifyRewarded(placement, false);
                    }
                });
    }

    private static void notifyRewarded(Rewarded placement, boolean available) {
        List<RewardedListener> listeners = new ArrayList<>(placement.listeners);
        placement.listeners.clear();
        for (RewardedListener l : listeners) l.onLoaded(available);
    }

    /** Forgets a listener whose screen went away before the load finished. */
    @MainThread
    public static void cancelRewardedListener(RewardedListener listener) {
        for (Rewarded placement : Rewarded.values()) placement.listeners.remove(listener);
    }

    /**
     * @param onReward runs on the main thread when the user earns the reward.
     * @param onClosed runs when the ad is dismissed (whether or not it was rewarded).
     */
    @MainThread
    public static void showRewarded(Activity activity, Rewarded placement, Runnable onReward, Runnable onClosed) {
        if (!ADS_ENABLED) {
            onReward.run();
            onClosed.run();
            return;
        }
        RewardedAd ad = placement.ad;
        placement.ad = null;
        if (!isFresh(ad, placement.loadedAt)) {
            onClosed.run();
            return;
        }
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

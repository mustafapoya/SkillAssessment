package net.golbarg.skillassessment.ui.question;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.MainThread;

import net.golbarg.skillassessment.ads.AdConfig.Placement;
import net.golbarg.skillassessment.ads.InterstitialAds;

/**
 * When a test may be wrapped in a full-screen ad. Interstitials only appear at natural breaks,
 * before a test starts or after it finishes, and both share one budget:
 * <ul>
 *     <li>the first {@value #GRACE_TESTS} tests a new user takes are ad-free;</li>
 *     <li>at least {@value #TESTS_BETWEEN_INTERSTITIALS} finished tests between two interstitials,
 *     whichever placement showed them;</li>
 *     <li>never after a test with fewer than {@value #MIN_ANSWERED_FOR_INTERSTITIAL} answers.</li>
 * </ul>
 * The time gap after any full-screen ad, consent and Premium are handled by the ads package.
 */
public final class TestAds {

    /** The first few tests a new user takes are always ad-free. */
    private static final int GRACE_TESTS = 2;
    /** Finished tests needed between two interstitials. */
    private static final int TESTS_BETWEEN_INTERSTITIALS = 2;
    /** A test abandoned after a couple of answers isn't a natural break worth an ad. */
    private static final int MIN_ANSWERED_FOR_INTERSTITIAL = 3;

    private static final String PREFS = "ads";
    private static final String KEY_TESTS_FINISHED = "tests_finished";
    private static final String KEY_TESTS_SINCE_INTERSTITIAL = "tests_since_interstitial";

    private TestAds() {
    }

    /** Loads the pre-test ad ahead of time, but only if the test counters could let it show next. */
    @MainThread
    public static void preloadBeforeTest(Context context) {
        if (countersAllow(context, Placement.TEST_START_INTERSTITIAL)) {
            InterstitialAds.preload(context, Placement.TEST_START_INTERSTITIAL);
        }
    }

    /** Loads the post-test ad ahead of time, but only if the test counters could let it show next. */
    @MainThread
    public static void preloadAfterTest(Context context) {
        if (countersAllow(context, Placement.TEST_FINISH_INTERSTITIAL)) {
            InterstitialAds.preload(context, Placement.TEST_FINISH_INTERSTITIAL);
        }
    }

    /** Starts a test, after the pre-test interstitial when one is due. */
    @MainThread
    public static void startTest(Activity activity, Intent test) {
        showBeforeTest(activity, () -> activity.startActivity(test));
    }

    /**
     * Shows the pre-test interstitial if the rules allow it, then runs {@code onDone} (which starts
     * the test) exactly once.
     */
    @MainThread
    public static void showBeforeTest(Activity activity, Runnable onDone) {
        if (!countersAllow(activity, Placement.TEST_START_INTERSTITIAL)) {
            onDone.run();
            return;
        }
        if (InterstitialAds.show(activity, Placement.TEST_START_INTERSTITIAL, onDone)) resetGap(activity);
    }

    /**
     * Records a finished test and shows the post-test interstitial if every rule allows it, then
     * runs {@code onDone} exactly once.
     */
    @MainThread
    public static void showAfterTest(Activity activity, int answered, Runnable onDone) {
        boolean allowed = answered >= MIN_ANSWERED_FOR_INTERSTITIAL && countersAllow(activity, Placement.TEST_FINISH_INTERSTITIAL);
        SharedPreferences prefs = prefs(activity);
        prefs.edit()
                .putInt(KEY_TESTS_FINISHED, prefs.getInt(KEY_TESTS_FINISHED, 0) + 1)
                .putInt(KEY_TESTS_SINCE_INTERSTITIAL, prefs.getInt(KEY_TESTS_SINCE_INTERSTITIAL, 0) + 1)
                .apply();
        if (!allowed) {
            onDone.run();
            return;
        }
        if (InterstitialAds.show(activity, Placement.TEST_FINISH_INTERSTITIAL, onDone)) resetGap(activity);
    }

    /**
     * The test counters, read before the current test is counted: no ads in the first
     * {@value #GRACE_TESTS} tests and at least {@value #TESTS_BETWEEN_INTERSTITIALS} finished tests
     * between two interstitials, whichever placement showed them.
     */
    private static boolean countersAllow(Context context, Placement placement) {
        SharedPreferences prefs = prefs(context);
        int test = prefs.getInt(KEY_TESTS_FINISHED, 0) + 1;
        // At the end of a test, that test already counts towards the gap
        int since = prefs.getInt(KEY_TESTS_SINCE_INTERSTITIAL, 0) + (placement == Placement.TEST_FINISH_INTERSTITIAL ? 1 : 0);
        return test > GRACE_TESTS && since >= TESTS_BETWEEN_INTERSTITIALS;
    }

    private static void resetGap(Context context) {
        prefs(context).edit().putInt(KEY_TESTS_SINCE_INTERSTITIAL, 0).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}

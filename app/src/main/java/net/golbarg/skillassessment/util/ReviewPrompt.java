package net.golbarg.skillassessment.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.MainThread;

import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Asks for a Play Store rating with Google's in-app review sheet right after a strong test result.
 * Google decides whether the sheet really appears (it has its own quota and never says), so this
 * only picks good moments: once the user has had {@value #MIN_STRONG_RESULTS} strong results, and
 * at most once every {@value #INTERVAL_DAYS} days.
 */
public final class ReviewPrompt {
    private static final String TAG = "ReviewPrompt";

    /** A result counts as strong at this score or above, over at least {@value #MIN_QUESTIONS} questions. */
    private static final int MIN_PERCENT = 80;
    private static final int MIN_QUESTIONS = 5;
    private static final int MIN_STRONG_RESULTS = 2;
    private static final int INTERVAL_DAYS = 30;

    private static final String PREFS = "review";
    private static final String KEY_STRONG_RESULTS = "strong_results";
    private static final String KEY_LAST_ASKED = "last_asked";

    private ReviewPrompt() {
    }

    /** Call once for each test that has just finished (not when reopening a past result). */
    @MainThread
    public static void onTestFinished(Activity activity, int percent, int questions) {
        if (percent < MIN_PERCENT || questions < MIN_QUESTIONS) return;
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int strong = prefs.getInt(KEY_STRONG_RESULTS, 0) + 1;
        prefs.edit().putInt(KEY_STRONG_RESULTS, strong).apply();
        if (strong < MIN_STRONG_RESULTS || askedRecently(prefs.getLong(KEY_LAST_ASKED, 0))) return;
        prefs.edit().putLong(KEY_LAST_ASKED, System.currentTimeMillis()).apply();

        ReviewManager manager = ReviewManagerFactory.create(activity);
        manager.requestReviewFlow().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.d(TAG, "Review flow unavailable", task.getException());
                return;
            }
            if (activity.isFinishing() || activity.isDestroyed()) return;
            manager.launchReviewFlow(activity, task.getResult());
        });
    }

    private static boolean askedRecently(long last) {
        long now = System.currentTimeMillis();
        // A clock set backwards shouldn't block the prompt forever.
        return last != 0 && now >= last && now - last < TimeUnit.DAYS.toMillis(INTERVAL_DAYS);
    }
}

package net.golbarg.skillassessment;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import net.golbarg.skillassessment.ads.AdManager;
import net.golbarg.skillassessment.billing.BillingManager;
import net.golbarg.skillassessment.db.ContentUpdater;
import net.golbarg.skillassessment.db.QuizRepository;
import net.golbarg.skillassessment.reminder.ReminderScheduler;
import net.golbarg.skillassessment.util.Async;
import net.golbarg.skillassessment.util.CategoryNames;
import net.golbarg.skillassessment.util.Feedback;
import net.golbarg.skillassessment.util.Prefs;

public class SkillAssessmentApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(Prefs.getThemeMode(this));
        CategoryNames.init(this);
        AdManager.init(this);
        Feedback.init(this);
        BillingManager.get(this).refresh();
        ReminderScheduler.createChannel(this);
        ReminderScheduler.schedule(this);
        // Opens (and if needed migrates) the database and seeds the starting coins off the main thread.
        // After an update, topics unlocked earlier also receive the explanations bundled with it.
        Async.io(() -> {
            QuizRepository repository = QuizRepository.get(this);
            repository.getCredits();
            if (Prefs.getExplanationsVersion(this) != BuildConfig.VERSION_CODE) {
                repository.refreshBundledExplanations();
                Prefs.setExplanationsVersion(this, BuildConfig.VERSION_CODE);
            }
        });
        // Picks up corrected or new questions from the website, at most once a day.
        ContentUpdater.checkInBackground(this);
    }
}

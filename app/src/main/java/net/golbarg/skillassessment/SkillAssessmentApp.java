package net.golbarg.skillassessment;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import net.golbarg.skillassessment.ads.AdManager;
import net.golbarg.skillassessment.billing.BillingManager;
import net.golbarg.skillassessment.db.ContentUpdater;
import net.golbarg.skillassessment.db.QuizRepository;
import net.golbarg.skillassessment.reminder.ReminderScheduler;
import net.golbarg.skillassessment.util.Async;
import net.golbarg.skillassessment.util.Feedback;
import net.golbarg.skillassessment.util.Prefs;

public class SkillAssessmentApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(Prefs.getThemeMode(this));
        AdManager.init(this);
        Feedback.init(this);
        BillingManager.get(this).refresh();
        ReminderScheduler.createChannel(this);
        ReminderScheduler.schedule(this);
        // Opens (and if needed migrates) the database and seeds the starting coins off the main thread.
        Async.io(() -> QuizRepository.get(this).getCredits());
        // Picks up corrected or new questions from the website, at most once a day.
        ContentUpdater.checkInBackground(this);
    }
}

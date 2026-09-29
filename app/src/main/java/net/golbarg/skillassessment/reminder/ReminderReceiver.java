package net.golbarg.skillassessment.reminder;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import net.golbarg.skillassessment.MainActivity;
import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.SplashScreenActivity;
import net.golbarg.skillassessment.db.QuizRepository;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.ui.question.QuestionActivity;
import net.golbarg.skillassessment.util.Async;
import net.golbarg.skillassessment.util.Prefs;
import net.golbarg.skillassessment.util.ProgressTracker;

/**
 * Posts the daily reminder, and re-arms it after a reboot or app update. When mistakes are due for
 * review the reminder says how many and opens the review directly.
 */
public class ReminderReceiver extends BroadcastReceiver {
    public static final String ACTION_REMIND = "net.golbarg.skillassessment.action.REMIND";
    private static final int NOTIFICATION_ID = 4102;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            ReminderScheduler.schedule(context);
            return;
        }
        if (!ACTION_REMIND.equals(action) || !ReminderScheduler.isEnabled(context)) return;
        // No nudge needed once today's goal is met (or, without a goal, once the user practised).
        int goal = Prefs.getDailyGoal(context);
        int answered = ProgressTracker.getAnsweredToday(context);
        if (goal > 0 ? answered >= goal : ProgressTracker.practicedToday(context)) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Counting due reviews reads the database, so it runs off the main thread.
        PendingResult pending = goAsync();
        Context app = context.getApplicationContext();
        Async.io(() -> {
            try {
                post(app, goal, answered, QuizRepository.get(app).getReviewDueCount());
            } finally {
                pending.finish();
            }
        });
    }

    private static void post(Context context, int goal, int answered, int reviewDue) {
        int streak = ProgressTracker.getDayStreak(context);
        String title = streak >= 1 ? context.getString(R.string.reminder_title_streak, streak) : context.getString(R.string.reminder_title);
        String text;
        PendingIntent content;
        if (reviewDue > 0) {
            text = context.getResources().getQuantityString(R.plurals.reminder_review_text, reviewDue, reviewDue);
            // Opens the review on top of the main screen, so Back lands in the app.
            Intent review = QuestionActivity.intent(context, Category.REVIEW, 0, Prefs.isTimerEnabled(context), false);
            content = TaskStackBuilder.create(context)
                    .addNextIntent(new Intent(context, MainActivity.class))
                    .addNextIntent(review)
                    .getPendingIntent(2, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            text = goal > 0 ? context.getString(R.string.reminder_goal_text, goal - answered)
                    : context.getString(R.string.reminder_text, ProgressTracker.DAILY_QUESTIONS);
            Intent open = new Intent(context, SplashScreenActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            content = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }

        ReminderScheduler.createChannel(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setColor(ContextCompat.getColor(context, R.color.brand_lime_dark))
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(content)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER);
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException e) {
            // Notification permission was revoked between the check above and now.
        }
    }
}

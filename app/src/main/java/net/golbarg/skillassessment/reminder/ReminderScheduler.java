package net.golbarg.skillassessment.reminder;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import net.golbarg.skillassessment.R;

import java.util.Calendar;

/** Schedules the optional daily practice reminder. */
public final class ReminderScheduler {
    public static final String CHANNEL_ID = "practice_reminders";
    private static final String FILE = "reminder";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_HOUR = "hour";
    private static final String KEY_MINUTE = "minute";
    private static final int REQUEST_CODE = 4101;

    private ReminderScheduler() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context c) {
        return prefs(c).getBoolean(KEY_ENABLED, false);
    }

    public static int getHour(Context c) {
        return prefs(c).getInt(KEY_HOUR, 19);
    }

    public static int getMinute(Context c) {
        return prefs(c).getInt(KEY_MINUTE, 0);
    }

    public static void enable(Context c, int hour, int minute) {
        prefs(c).edit().putBoolean(KEY_ENABLED, true).putInt(KEY_HOUR, hour).putInt(KEY_MINUTE, minute).apply();
        schedule(c);
    }

    public static void disable(Context c) {
        prefs(c).edit().putBoolean(KEY_ENABLED, false).apply();
        AlarmManager alarms = c.getSystemService(AlarmManager.class);
        if (alarms != null) alarms.cancel(pendingIntent(c));
    }

    /** (Re)arms the repeating alarm if reminders are on. Inexact, so it needs no special permission. */
    public static void schedule(Context c) {
        if (!isEnabled(c)) return;
        AlarmManager alarms = c.getSystemService(AlarmManager.class);
        if (alarms == null) return;
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, getHour(c));
        next.set(Calendar.MINUTE, getMinute(c));
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (next.getTimeInMillis() <= System.currentTimeMillis()) next.add(Calendar.DAY_OF_YEAR, 1);
        alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent(c));
    }

    public static void createChannel(Context c) {
        NotificationManager manager = c.getSystemService(NotificationManager.class);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, c.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(c.getString(R.string.reminder_channel_desc));
        manager.createNotificationChannel(channel);
    }

    private static PendingIntent pendingIntent(Context c) {
        Intent intent = new Intent(c, ReminderReceiver.class).setAction(ReminderReceiver.ACTION_REMIND);
        return PendingIntent.getBroadcast(c, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}

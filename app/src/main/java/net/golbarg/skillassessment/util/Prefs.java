package net.golbarg.skillassessment.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/** Typed access to user preferences. Keys and files are shared with version 1 of the app. */
public final class Prefs {
    private static final String FILE_INTRO = "intro_pref";
    private static final String KEY_INTRO_OPENED = "is_intro_opened";
    private static final String KEY_USER_NAME = "user_name";

    private static final String FILE_SETTINGS = "settings";
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_TIMER = "timer_enabled";
    private static final String KEY_SHUFFLE = "shuffle_enabled";
    private static final String KEY_LENGTH = "test_length";
    private static final String KEY_SOUND = "sound_enabled";
    private static final String KEY_VIBRATION = "vibration_enabled";
    private static final String KEY_LARGE_CODE_FONT = "large_code_font";
    private static final String KEY_PREVIEW_PREFIX = "preview_used_";
    private static final String KEY_DAILY_GOAL = "daily_goal";
    private static final String KEY_EXPLANATIONS_VERSION = "explanations_version";

    /** Choices offered for the daily goal; 0 turns it off. */
    public static final int[] DAILY_GOALS = {0, 5, 10, 20, 50};

    private Prefs() {
    }

    private static SharedPreferences intro(Context c) {
        return c.getSharedPreferences(FILE_INTRO, Context.MODE_PRIVATE);
    }

    private static SharedPreferences settings(Context c) {
        return c.getSharedPreferences(FILE_SETTINGS, Context.MODE_PRIVATE);
    }

    public static boolean isIntroSeen(Context c) {
        return intro(c).getBoolean(KEY_INTRO_OPENED, false);
    }

    public static void setIntroSeen(Context c) {
        intro(c).edit().putBoolean(KEY_INTRO_OPENED, true).apply();
    }

    public static String getUserName(Context c) {
        return intro(c).getString(KEY_USER_NAME, "").trim();
    }

    public static void setUserName(Context c, String name) {
        intro(c).edit().putString(KEY_USER_NAME, name == null ? "" : name.trim()).apply();
    }

    /** One of {@link AppCompatDelegate#MODE_NIGHT_FOLLOW_SYSTEM}, _NO or _YES. */
    public static int getThemeMode(Context c) {
        return settings(c).getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static void setThemeMode(Context c, int mode) {
        settings(c).edit().putInt(KEY_THEME, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static boolean isSoundEnabled(Context c) {
        return settings(c).getBoolean(KEY_SOUND, true);
    }

    public static void setSoundEnabled(Context c, boolean enabled) {
        settings(c).edit().putBoolean(KEY_SOUND, enabled).apply();
    }

    public static boolean isVibrationEnabled(Context c) {
        return settings(c).getBoolean(KEY_VIBRATION, true);
    }

    public static void setVibrationEnabled(Context c, boolean enabled) {
        settings(c).edit().putBoolean(KEY_VIBRATION, enabled).apply();
    }

    public static boolean isTimerEnabled(Context c) {
        return settings(c).getBoolean(KEY_TIMER, true);
    }

    public static boolean isShuffleEnabled(Context c) {
        return settings(c).getBoolean(KEY_SHUFFLE, true);
    }

    public static boolean isLargeCodeFont(Context c) {
        return settings(c).getBoolean(KEY_LARGE_CODE_FONT, false);
    }

    public static void setLargeCodeFont(Context c, boolean large) {
        settings(c).edit().putBoolean(KEY_LARGE_CODE_FONT, large).apply();
    }

    /** Each locked topic can be previewed for free once. */
    public static boolean isPreviewUsed(Context c, int categoryId) {
        return settings(c).getBoolean(KEY_PREVIEW_PREFIX + categoryId, false);
    }

    public static void setPreviewUsed(Context c, int categoryId) {
        settings(c).edit().putBoolean(KEY_PREVIEW_PREFIX + categoryId, true).apply();
    }

    /** Questions to answer per day; 0 means no goal. */
    public static int getDailyGoal(Context c) {
        return settings(c).getInt(KEY_DAILY_GOAL, 10);
    }

    public static void setDailyGoal(Context c, int goal) {
        settings(c).edit().putInt(KEY_DAILY_GOAL, Math.max(0, goal)).apply();
    }

    /** App version whose bundled explanations were last copied into unlocked topics. */
    public static int getExplanationsVersion(Context c) {
        return settings(c).getInt(KEY_EXPLANATIONS_VERSION, 0);
    }

    public static void setExplanationsVersion(Context c, int versionCode) {
        settings(c).edit().putInt(KEY_EXPLANATIONS_VERSION, versionCode).apply();
    }

    /** Preferred number of questions; 0 means the whole topic. */
    public static int getTestLength(Context c) {
        return settings(c).getInt(KEY_LENGTH, 10);
    }

    public static void saveTestSetup(Context c, int length, boolean timer, boolean shuffle) {
        settings(c).edit().putInt(KEY_LENGTH, length).putBoolean(KEY_TIMER, timer).putBoolean(KEY_SHUFFLE, shuffle).apply();
    }
}

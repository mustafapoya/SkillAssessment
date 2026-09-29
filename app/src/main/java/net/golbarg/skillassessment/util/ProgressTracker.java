package net.golbarg.skillassessment.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.WorkerThread;

import net.golbarg.skillassessment.db.QuizRepository;
import net.golbarg.skillassessment.models.Achievement;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.models.QuestionResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Day streaks, the daily challenge and achievements. All state lives in one preferences file. */
public final class ProgressTracker {
    public static final int DAILY_QUESTIONS = 5;
    public static final int DAILY_BONUS_COINS = 1;
    private static final int EXAM_PASS_PERCENT = 70;

    private static final String FILE = "progress";
    private static final String KEY_LAST_ACTIVE = "last_active_day";
    private static final String KEY_DAY_STREAK = "day_streak";
    private static final String KEY_BEST_DAY_STREAK = "best_day_streak";
    private static final String KEY_DAILY_DONE = "daily_done_day";
    private static final String KEY_DAILY_COUNT = "daily_count";
    private static final String KEY_BEST_ANSWER_STREAK = "best_answer_streak";
    private static final String KEY_PERFECT = "perfect_tests";
    private static final String KEY_EXAM_PASSED = "exams_passed";
    private static final String KEY_REVIEW_CORRECT = "review_correct";
    private static final String KEY_ACH_PREFIX = "ach_";
    private static final String KEY_ANSWERED_DAY = "answered_day";
    private static final String KEY_ANSWERED_TODAY = "answered_today";
    private static final String KEY_LAST_RESTORE = "streak_restored_day";
    /** A missed day can be bought back at most once in this many days. */
    private static final int RESTORE_COOLDOWN_DAYS = 7;

    /** What changed when a test was finished, shown on the result screen. */
    public static final class Outcome {
        public int dayStreak;
        public boolean dayStreakIncreased;
        public int bonusCoins;
        /** True when this test pushed today's count past the daily goal. */
        public boolean goalReached;
        public final List<Achievement> newAchievements = new ArrayList<>();
    }

    public static final class AchievementState {
        public final Achievement achievement;
        public final long unlockedAt;

        AchievementState(Achievement achievement, long unlockedAt) {
            this.achievement = achievement;
            this.unlockedAt = unlockedAt;
        }

        public boolean isUnlocked() {
            return unlockedAt > 0;
        }
    }

    private ProgressTracker() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    private static long today() {
        return LocalDate.now().toEpochDay();
    }

    /** Seed shared by everyone on the same calendar day. */
    public static long dailySeed() {
        return today();
    }

    /** Current streak of consecutive practice days; 0 once a whole day has been missed. */
    public static int getDayStreak(Context c) {
        SharedPreferences p = prefs(c);
        long last = p.getLong(KEY_LAST_ACTIVE, Long.MIN_VALUE);
        if (last < today() - 1) return 0;
        return p.getInt(KEY_DAY_STREAK, 0);
    }

    public static boolean practicedToday(Context c) {
        return prefs(c).getLong(KEY_LAST_ACTIVE, Long.MIN_VALUE) == today();
    }

    public static boolean isDailyDone(Context c) {
        return prefs(c).getLong(KEY_DAILY_DONE, Long.MIN_VALUE) == today();
    }

    /**
     * Records a finished test: updates streaks and counters, pays the daily bonus once per day and
     * unlocks any achievements that became reachable.
     */
    @WorkerThread
    public static synchronized Outcome onTestFinished(Context c, QuizRepository repository, QuestionResult result,
                                                      boolean exam, int bestAnswerStreak) {
        SharedPreferences p = prefs(c);
        SharedPreferences.Editor e = p.edit();
        Outcome outcome = new Outcome();
        long today = today();

        long last = p.getLong(KEY_LAST_ACTIVE, Long.MIN_VALUE);
        int streak = p.getInt(KEY_DAY_STREAK, 0);
        if (last != today) {
            streak = last == today - 1 ? streak + 1 : 1;
            outcome.dayStreakIncreased = true;
            e.putLong(KEY_LAST_ACTIVE, today).putInt(KEY_DAY_STREAK, streak);
            e.putInt(KEY_BEST_DAY_STREAK, Math.max(streak, p.getInt(KEY_BEST_DAY_STREAK, 0)));
        }
        outcome.dayStreak = streak;

        if (result.getCategoryId() == Category.DAILY && p.getLong(KEY_DAILY_DONE, Long.MIN_VALUE) != today) {
            e.putLong(KEY_DAILY_DONE, today).putInt(KEY_DAILY_COUNT, p.getInt(KEY_DAILY_COUNT, 0) + 1);
            repository.addCredits(DAILY_BONUS_COINS);
            outcome.bonusCoins = DAILY_BONUS_COINS;
        }
        int before = getAnsweredToday(c);
        int after = before + result.getTotal();
        e.putLong(KEY_ANSWERED_DAY, today).putInt(KEY_ANSWERED_TODAY, after);
        int goal = Prefs.getDailyGoal(c);
        outcome.goalReached = goal > 0 && before < goal && after >= goal;

        if (bestAnswerStreak > p.getInt(KEY_BEST_ANSWER_STREAK, 0)) e.putInt(KEY_BEST_ANSWER_STREAK, bestAnswerStreak);
        if (result.getTotal() >= 5 && result.getCorrectAnswer() == result.getTotal()) e.putInt(KEY_PERFECT, p.getInt(KEY_PERFECT, 0) + 1);
        if (exam && result.getTotal() >= 5 && result.getScorePercent() >= EXAM_PASS_PERCENT) e.putInt(KEY_EXAM_PASSED, p.getInt(KEY_EXAM_PASSED, 0) + 1);
        if (result.getCategoryId() == Category.REVIEW) e.putInt(KEY_REVIEW_CORRECT, p.getInt(KEY_REVIEW_CORRECT, 0) + result.getCorrectAnswer());
        e.apply();

        outcome.newAchievements.addAll(evaluate(c, repository));
        return outcome;
    }

    /** Questions answered in finished tests today, for the daily goal. */
    public static int getAnsweredToday(Context c) {
        SharedPreferences p = prefs(c);
        return p.getLong(KEY_ANSWERED_DAY, Long.MIN_VALUE) == today() ? p.getInt(KEY_ANSWERED_TODAY, 0) : 0;
    }

    /**
     * The streak that ended because exactly one day (yesterday) was missed, if it can still be
     * restored; otherwise 0. Short streaks and repeated restores within a week don't qualify.
     */
    public static int getRestorableStreak(Context c) {
        SharedPreferences p = prefs(c);
        long last = p.getLong(KEY_LAST_ACTIVE, Long.MIN_VALUE);
        int streak = p.getInt(KEY_DAY_STREAK, 0);
        if (last != today() - 2 || streak < 2) return 0;
        long lastRestore = p.getLong(KEY_LAST_RESTORE, Long.MIN_VALUE);
        if (lastRestore != Long.MIN_VALUE && today() - lastRestore < RESTORE_COOLDOWN_DAYS) return 0;
        return streak;
    }

    /** Treats yesterday as practised so the streak continues. Returns the restored streak. */
    public static synchronized int restoreStreak(Context c) {
        int streak = getRestorableStreak(c);
        if (streak == 0) return 0;
        prefs(c).edit().putLong(KEY_LAST_ACTIVE, today() - 1).putLong(KEY_LAST_RESTORE, today()).apply();
        return streak;
    }

    /** Every stored value, for backups. */
    public static java.util.Map<String, ?> exportState(Context c) {
        return prefs(c).getAll();
    }

    /** Replaces all stored values with a backup's. */
    @SuppressWarnings("unchecked")
    public static void importState(Context c, java.util.Map<String, Object> values) {
        SharedPreferences.Editor e = prefs(c).edit().clear();
        for (java.util.Map.Entry<String, Object> entry : values.entrySet()) {
            Object v = entry.getValue();
            if (v instanceof Long) e.putLong(entry.getKey(), (Long) v);
            else if (v instanceof Integer) e.putInt(entry.getKey(), (Integer) v);
            else if (v instanceof Boolean) e.putBoolean(entry.getKey(), (Boolean) v);
            else if (v instanceof String) e.putString(entry.getKey(), (String) v);
        }
        e.apply();
    }

    /** Unlocks every achievement whose condition now holds and returns the newly unlocked ones. */
    @WorkerThread
    public static synchronized List<Achievement> evaluate(Context c, QuizRepository repository) {
        SharedPreferences p = prefs(c);
        QuizRepository.Stats stats = repository.getStats();
        boolean masteredTopic = false;
        for (Category category : repository.getCategories()) {
            if (category.isUnlocked() && category.getMasteryPercent() >= 100) masteredTopic = true;
        }
        int bestDay = Math.max(p.getInt(KEY_BEST_DAY_STREAK, 0), getDayStreak(c));

        List<Achievement> fresh = new ArrayList<>();
        SharedPreferences.Editor e = p.edit();
        long now = System.currentTimeMillis();
        for (Achievement a : Achievement.values()) {
            if (p.getLong(KEY_ACH_PREFIX + a.name(), 0) > 0) continue;
            boolean met;
            switch (a) {
                case FIRST_TEST: met = stats.tests >= 1; break;
                case PERFECT: met = p.getInt(KEY_PERFECT, 0) >= 1; break;
                case STREAK_5: met = p.getInt(KEY_BEST_ANSWER_STREAK, 0) >= 5; break;
                case STREAK_10: met = p.getInt(KEY_BEST_ANSWER_STREAK, 0) >= 10; break;
                case DAY_3: met = bestDay >= 3; break;
                case DAY_7: met = bestDay >= 7; break;
                case DAY_30: met = bestDay >= 30; break;
                case DAILY_7: met = p.getInt(KEY_DAILY_COUNT, 0) >= 7; break;
                case TOPICS_3: met = stats.unlockedTopics >= 3; break;
                case TOPICS_10: met = stats.unlockedTopics >= 10; break;
                case ANSWERED_100: met = stats.answered() >= 100; break;
                case ANSWERED_500: met = stats.answered() >= 500; break;
                case ANSWERED_1000: met = stats.answered() >= 1000; break;
                case MASTER_TOPIC: met = masteredTopic; break;
                case REVIEW_25: met = p.getInt(KEY_REVIEW_CORRECT, 0) >= 25; break;
                case EXAM_PASS: met = p.getInt(KEY_EXAM_PASSED, 0) >= 1; break;
                default: met = false;
            }
            if (met) {
                e.putLong(KEY_ACH_PREFIX + a.name(), now);
                fresh.add(a);
            }
        }
        e.apply();
        return fresh;
    }

    public static List<AchievementState> getAchievements(Context c) {
        SharedPreferences p = prefs(c);
        List<AchievementState> list = new ArrayList<>();
        for (Achievement a : Achievement.values()) list.add(new AchievementState(a, p.getLong(KEY_ACH_PREFIX + a.name(), 0)));
        return list;
    }
}

package net.golbarg.skillassessment.models;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import net.golbarg.skillassessment.R;

/** Milestones the user can unlock. Conditions are evaluated in ProgressTracker. */
public enum Achievement {
    FIRST_TEST(R.string.ach_first_test, R.string.ach_first_test_desc, R.drawable.ic_flag),
    PERFECT(R.string.ach_perfect, R.string.ach_perfect_desc, R.drawable.ic_star),
    STREAK_5(R.string.ach_streak_5, R.string.ach_streak_5_desc, R.drawable.ic_flame_mono),
    STREAK_10(R.string.ach_streak_10, R.string.ach_streak_10_desc, R.drawable.ic_flame_mono),
    DAY_3(R.string.ach_day_3, R.string.ach_day_3_desc, R.drawable.ic_calendar),
    DAY_7(R.string.ach_day_7, R.string.ach_day_7_desc, R.drawable.ic_calendar),
    DAY_30(R.string.ach_day_30, R.string.ach_day_30_desc, R.drawable.ic_calendar),
    DAILY_7(R.string.ach_daily_7, R.string.ach_daily_7_desc, R.drawable.ic_today),
    TOPICS_3(R.string.ach_topics_3, R.string.ach_topics_3_desc, R.drawable.ic_lock_open),
    TOPICS_10(R.string.ach_topics_10, R.string.ach_topics_10_desc, R.drawable.ic_lock_open),
    ANSWERED_100(R.string.ach_answered_100, R.string.ach_answered_100_desc, R.drawable.ic_check_circle),
    ANSWERED_500(R.string.ach_answered_500, R.string.ach_answered_500_desc, R.drawable.ic_check_circle),
    ANSWERED_1000(R.string.ach_answered_1000, R.string.ach_answered_1000_desc, R.drawable.ic_check_circle),
    MASTER_TOPIC(R.string.ach_master, R.string.ach_master_desc, R.drawable.ic_trophy),
    REVIEW_25(R.string.ach_review, R.string.ach_review_desc, R.drawable.ic_replay),
    EXAM_PASS(R.string.ach_exam, R.string.ach_exam_desc, R.drawable.ic_school);

    @StringRes public final int title;
    @StringRes public final int description;
    @DrawableRes public final int icon;

    Achievement(int title, int description, int icon) {
        this.title = title;
        this.description = description;
        this.icon = icon;
    }
}

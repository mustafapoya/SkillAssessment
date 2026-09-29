package net.golbarg.skillassessment.util;

import android.content.Context;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.golbarg.skillassessment.R;

/** Dialog for choosing the daily question goal; shared by Home and Settings. */
public final class GoalPicker {
    private GoalPicker() {
    }

    public static String label(Context context, int goal) {
        return goal <= 0 ? context.getString(R.string.daily_goal_off) : context.getString(R.string.daily_goal_value, goal);
    }

    public static void show(Context context, Runnable onChanged) {
        int current = Prefs.getDailyGoal(context);
        String[] labels = new String[Prefs.DAILY_GOALS.length];
        int checked = 0;
        for (int i = 0; i < labels.length; i++) {
            labels[i] = label(context, Prefs.DAILY_GOALS[i]);
            if (Prefs.DAILY_GOALS[i] == current) checked = i;
        }
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.daily_goal)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    Prefs.setDailyGoal(context, Prefs.DAILY_GOALS[which]);
                    onChanged.run();
                })
                .show();
    }
}

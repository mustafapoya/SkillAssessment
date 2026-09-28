package net.golbarg.skillassessment.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.TaskStackBuilder;

import net.golbarg.skillassessment.MainActivity;
import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.db.QuizRepository;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.models.Question;
import net.golbarg.skillassessment.ui.question.QuestionActivity;
import net.golbarg.skillassessment.util.Async;
import net.golbarg.skillassessment.util.CategoryNames;
import net.golbarg.skillassessment.util.ContentParser;
import net.golbarg.skillassessment.util.Prefs;
import net.golbarg.skillassessment.util.ProgressTracker;

import java.util.List;

/** Home-screen widget showing the first question of today's daily challenge. */
public class DailyQuestionWidget extends AppWidgetProvider {

    /** Redraws every placed widget, e.g. after the first topic is unlocked. */
    public static void refresh(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, DailyQuestionWidget.class));
        if (ids.length == 0) return;
        Intent update = new Intent(context, DailyQuestionWidget.class)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(update);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        PendingResult pending = goAsync();
        Context app = context.getApplicationContext();
        Async.io(() -> {
            try {
                QuizRepository repository = QuizRepository.get(app);
                List<Question> daily = repository.getMixedQuestions(ProgressTracker.DAILY_QUESTIONS, ProgressTracker.dailySeed());
                Question question = daily.isEmpty() ? null : daily.get(0);
                String slug = question == null ? null : repository.getSlugs().get(question.getCategoryId());
                for (int id : appWidgetIds) manager.updateAppWidget(id, build(app, question, slug));
            } finally {
                pending.finish();
            }
        });
    }

    private static RemoteViews build(Context context, Question question, String slug) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_daily_question);
        PendingIntent open;
        if (question == null) {
            views.setTextViewText(R.id.txt_topic, context.getString(R.string.app_name));
            views.setTextViewText(R.id.txt_question, context.getString(R.string.widget_empty));
            views.setTextViewText(R.id.txt_badge, "101");
            views.setViewVisibility(R.id.btn_answer, View.GONE);
            open = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            views.setTextViewText(R.id.txt_topic, CategoryNames.displayName(slug));
            views.setTextViewText(R.id.txt_badge, CategoryNames.monogram(slug));
            views.setTextViewText(R.id.txt_question, ContentParser.preview(question.getTitle()));
            views.setViewVisibility(R.id.btn_answer, View.VISIBLE);
            // Opens today's challenge on top of the main screen, so Back lands in the app.
            Intent quiz = QuestionActivity.intent(context, Category.DAILY, ProgressTracker.DAILY_QUESTIONS,
                    Prefs.isTimerEnabled(context), false, false);
            open = TaskStackBuilder.create(context)
                    .addNextIntent(new Intent(context, MainActivity.class))
                    .addNextIntent(quiz)
                    .getPendingIntent(1, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        }
        views.setOnClickPendingIntent(R.id.widget_root, open);
        views.setOnClickPendingIntent(R.id.btn_answer, open);
        return views;
    }
}

package net.golbarg.skillassessment.ui.question;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import net.golbarg.skillassessment.models.AnswerResponseType;
import net.golbarg.skillassessment.models.ResultItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot of an unfinished test, saved whenever the quiz screen goes to the background so the
 * user can continue after the app is closed. Only one test is kept; starting another replaces it.
 */
public final class QuizSession {
    private static final String PREFS = "quiz_session";
    private static final String KEY_JSON = "json";
    /** Older snapshots are dropped rather than offered. */
    private static final long MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000;

    public int categoryId;
    public int length;
    public boolean timer;
    public boolean shuffle;
    public boolean exam;
    /** Learning-path level, or -1 for a whole-topic test. */
    public int level = -1;
    /** Focused practice on the topic's mistakes. */
    public boolean focus;
    public List<Integer> questionIds = new ArrayList<>();
    public List<ResultItem> answers = new ArrayList<>();
    public int streak;
    public int bestStreak;
    public long activeMs;
    public long createdAt;
    public long examRemainingMs;
    public boolean hintUsed;
    public long savedAt;

    public int answeredCount() {
        return answers.size();
    }

    public int total() {
        return questionIds.size();
    }

    public void save(Context context) {
        try {
            JSONObject o = new JSONObject();
            o.put("categoryId", categoryId);
            o.put("length", length);
            o.put("timer", timer);
            o.put("shuffle", shuffle);
            o.put("exam", exam);
            o.put("level", level);
            o.put("focus", focus);
            JSONArray ids = new JSONArray();
            for (int id : questionIds) ids.put(id);
            o.put("questionIds", ids);
            JSONArray items = new JSONArray();
            for (ResultItem item : answers) {
                JSONObject a = new JSONObject();
                a.put("q", item.getQuestionId());
                JSONArray selected = new JSONArray();
                for (int s : item.getSelectedPositions()) selected.put(s);
                a.put("s", selected);
                a.put("o", item.getOutcome().code);
                items.put(a);
            }
            o.put("answers", items);
            o.put("streak", streak);
            o.put("bestStreak", bestStreak);
            o.put("activeMs", activeMs);
            o.put("createdAt", createdAt);
            o.put("examRemainingMs", examRemainingMs);
            o.put("hintUsed", hintUsed);
            o.put("savedAt", System.currentTimeMillis());
            prefs(context).edit().putString(KEY_JSON, o.toString()).apply();
        } catch (Exception ignored) {
            // A snapshot is a convenience; failing to write one must never break the test.
        }
    }

    /** The saved test, or null if there is none (or it is too old or unreadable). */
    @Nullable
    public static QuizSession load(Context context) {
        String json = prefs(context).getString(KEY_JSON, null);
        if (json == null) return null;
        try {
            JSONObject o = new JSONObject(json);
            QuizSession s = new QuizSession();
            s.categoryId = o.getInt("categoryId");
            s.length = o.optInt("length");
            s.timer = o.optBoolean("timer", true);
            s.shuffle = o.optBoolean("shuffle", true);
            s.exam = o.optBoolean("exam");
            s.level = o.optInt("level", -1);
            s.focus = o.optBoolean("focus");
            JSONArray ids = o.getJSONArray("questionIds");
            for (int i = 0; i < ids.length(); i++) s.questionIds.add(ids.getInt(i));
            JSONArray items = o.getJSONArray("answers");
            for (int i = 0; i < items.length(); i++) {
                JSONObject a = items.getJSONObject(i);
                List<Integer> selected = new ArrayList<>();
                JSONArray sel = a.getJSONArray("s");
                for (int j = 0; j < sel.length(); j++) selected.add(sel.getInt(j));
                s.answers.add(new ResultItem(a.getInt("q"), selected, AnswerResponseType.fromCode(a.getInt("o"))));
            }
            s.streak = o.optInt("streak");
            s.bestStreak = o.optInt("bestStreak");
            s.activeMs = o.optLong("activeMs");
            s.createdAt = o.optLong("createdAt", System.currentTimeMillis());
            s.examRemainingMs = o.optLong("examRemainingMs");
            s.hintUsed = o.optBoolean("hintUsed");
            s.savedAt = o.optLong("savedAt");
            boolean stale = System.currentTimeMillis() - s.savedAt > MAX_AGE_MS;
            if (stale || s.questionIds.isEmpty() || s.answers.size() >= s.questionIds.size()) {
                clear(context);
                return null;
            }
            return s;
        } catch (Exception e) {
            clear(context);
            return null;
        }
    }

    public static void clear(Context context) {
        prefs(context).edit().remove(KEY_JSON).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}

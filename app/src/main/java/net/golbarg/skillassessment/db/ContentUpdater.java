package net.golbarg.skillassessment.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.JsonReader;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import net.golbarg.skillassessment.models.Question;
import net.golbarg.skillassessment.util.Async;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Downloads corrected and new questions from the website, so content fixes reach users without an
 * app release.
 *
 * <p>The server hosts a small manifest at {@link #MANIFEST_URL}:
 * <pre>{"version": 3, "url": "https://golbarg.net/skillassessment/content/update-3.json"}</pre>
 * and the update file it points to, which uses the same question format as {@code questions.json}
 * (an optional {@code "explanation"} string per question is supported):
 * <pre>{"version": 3, "categories": [{"id": 12, "questions": [{"id": 804, "number": 5, "title": "…",
 *   "number_of_correct": 1, "explanation": "…", "answers": [{"number": 1, "title": "…", "is_correct": "true"}]}]}]}</pre>
 * Each update file must contain every correction made so far (it replaces the previous one).
 * Questions are replaced by id or added; they are never removed, so a topic can't lose its
 * unlocked state. Topics that are still locked receive the corrections when they are unlocked.
 */
public final class ContentUpdater {
    private static final String TAG = "ContentUpdater";

    public static final String MANIFEST_URL = "https://golbarg.net/skillassessment/content/manifest.json";
    /** Update files are only accepted from this origin. */
    private static final String ALLOWED_PREFIX = "https://golbarg.net/";
    private static final long CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000;
    private static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final int TIMEOUT_MS = 15_000;

    private static final String PREFS = "content";
    private static final String KEY_LAST_CHECK = "last_check";
    private static final String KEY_VERSION = "version";
    private static final String KEY_UPDATED_QUESTIONS = "updated_questions";
    private static final String FILE_NAME = "content_update.json";

    public enum Status { UPDATED, UP_TO_DATE, FAILED }

    /** Parsed update file, by topic id. Guarded by the class lock. */
    private static Map<Integer, List<Question>> cache;

    private ContentUpdater() {
    }

    /** Checks at most once a day, off the main thread. */
    public static void checkInBackground(Context context) {
        Context app = context.getApplicationContext();
        long last = prefs(app).getLong(KEY_LAST_CHECK, 0);
        if (Math.abs(System.currentTimeMillis() - last) < CHECK_INTERVAL_MS) return;
        Async.io(() -> check(app));
    }

    public static long getLastCheck(Context context) {
        return prefs(context).getLong(KEY_LAST_CHECK, 0);
    }

    /** Number of questions corrected or added by the installed update. */
    public static int getUpdatedQuestionCount(Context context) {
        return prefs(context).getInt(KEY_UPDATED_QUESTIONS, 0);
    }

    @WorkerThread
    public static synchronized Status check(Context context) {
        Context app = context.getApplicationContext();
        SharedPreferences prefs = prefs(app);
        // Recorded even when the check fails, so an unreachable server is retried at most daily.
        prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply();
        try {
            JSONObject manifest = new JSONObject(new String(download(MANIFEST_URL, 64 * 1024), StandardCharsets.UTF_8));
            int version = manifest.getInt("version");
            String url = manifest.getString("url");
            if (version <= prefs.getInt(KEY_VERSION, 0)) return Status.UP_TO_DATE;
            if (!url.startsWith(ALLOWED_PREFIX)) throw new SecurityException("Update URL not allowed: " + url);

            byte[] body = download(url, MAX_BYTES);
            Map<Integer, List<Question>> update = parse(new String(body, StandardCharsets.UTF_8));
            int applied = applyToDatabase(app, update);

            File file = new File(app.getFilesDir(), FILE_NAME);
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(body);
            }
            cache = update;
            int total = 0;
            for (List<Question> list : update.values()) total += list.size();
            prefs.edit().putInt(KEY_VERSION, version).putInt(KEY_UPDATED_QUESTIONS, total).apply();
            Log.i(TAG, "Content v" + version + ": " + total + " questions, " + applied + " applied to unlocked topics");
            return Status.UPDATED;
        } catch (Exception e) {
            Log.w(TAG, "Content update failed", e);
            return Status.FAILED;
        }
    }

    /** Replaces bundled questions of one topic with their downloaded corrections and adds new ones. */
    static synchronized List<Question> applyOverrides(Context context, int categoryId, List<Question> fromAsset) {
        List<Question> overrides = loadCache(context).get(categoryId);
        if (overrides == null || overrides.isEmpty()) return fromAsset;
        LinkedHashMap<Integer, Question> byId = new LinkedHashMap<>();
        for (Question q : fromAsset) byId.put(q.getId(), q);
        for (Question q : overrides) byId.put(q.getId(), q);
        return new ArrayList<>(byId.values());
    }

    private static Map<Integer, List<Question>> loadCache(Context context) {
        if (cache != null) return cache;
        cache = new HashMap<>();
        File file = new File(context.getApplicationContext().getFilesDir(), FILE_NAME);
        if (!file.exists()) return cache;
        try (InputStream in = new FileInputStream(file)) {
            cache = parse(new String(readFully(in, MAX_BYTES), StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.w(TAG, "Stored content update unreadable", e);
        }
        return cache;
    }

    /** Writes corrections for topics that are already unlocked, in one transaction. */
    private static int applyToDatabase(Context context, Map<Integer, List<Question>> update) {
        SQLiteDatabase db = DatabaseHandler.getInstance(context).getWritableDatabase();
        int applied = 0;
        db.beginTransaction();
        try {
            for (Map.Entry<Integer, List<Question>> entry : update.entrySet()) {
                try (Cursor c = db.rawQuery("SELECT 1 FROM " + DatabaseHandler.T_CATEGORY + " WHERE id = ?",
                        new String[]{String.valueOf(entry.getKey())})) {
                    if (!c.moveToFirst()) continue;
                }
                for (Question q : entry.getValue()) {
                    QuizRepository.writeQuestion(db, q);
                    applied++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return applied;
    }

    private static Map<Integer, List<Question>> parse(String json) throws Exception {
        Map<Integer, List<Question>> result = new HashMap<>();
        try (JsonReader reader = new JsonReader(new BufferedReader(new StringReader(json)))) {
            reader.beginObject();
            while (reader.hasNext()) {
                if (!"categories".equals(reader.nextName())) {
                    reader.skipValue();
                    continue;
                }
                reader.beginArray();
                while (reader.hasNext()) {
                    int categoryId = -1;
                    List<Question> questions = new ArrayList<>();
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String name = reader.nextName();
                        if ("id".equals(name)) {
                            categoryId = reader.nextInt();
                        } else if ("questions".equals(name)) {
                            // The id comes first in well-formed files; questions are re-keyed below if not.
                            reader.beginArray();
                            while (reader.hasNext()) questions.add(QuizRepository.readQuestion(reader, categoryId));
                            reader.endArray();
                        } else {
                            reader.skipValue();
                        }
                    }
                    reader.endObject();
                    if (categoryId < 0) continue;
                    List<Question> valid = new ArrayList<>();
                    for (Question q : questions) {
                        // Reject anything that would break a test: no id, no text or too few answers.
                        if (q.getId() <= 0 || q.getTitle().trim().isEmpty() || q.getAnswers().size() < 2) continue;
                        if (q.getCategoryId() != categoryId) {
                            Question fixed = new Question(q.getId(), categoryId, q.getNumber(), q.getTitle(), q.getRequiredSelections());
                            fixed.setExplanation(q.getExplanation());
                            fixed.getAnswers().addAll(q.getAnswers());
                            q = fixed;
                        }
                        valid.add(q);
                    }
                    result.put(categoryId, valid);
                }
                reader.endArray();
            }
            reader.endObject();
        }
        return result;
    }

    private static byte[] download(String url, int maxBytes) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setInstanceFollowRedirects(false);
        try {
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) throw new IllegalStateException("HTTP " + code + " for " + url);
            try (InputStream in = connection.getInputStream()) {
                return readFully(in, maxBytes);
            }
        } finally {
            connection.disconnect();
        }
    }

    @NonNull
    private static byte[] readFully(InputStream in, int maxBytes) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[16 * 1024];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
            if (out.size() > maxBytes) throw new IllegalStateException("Response too large");
        }
        return out.toByteArray();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}

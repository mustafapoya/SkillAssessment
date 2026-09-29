package net.golbarg.skillassessment.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.annotation.WorkerThread;

import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.util.Prefs;
import net.golbarg.skillassessment.util.ProgressTracker;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Exports and restores everything a user earned: results with their answers, bookmarks, the review
 * queue, unlocked topics, coins, streaks and achievements.
 *
 * <p>The file is {@code {"format": …, "payload": "<json string>", "checksum": "<sha-256>"}}. The
 * checksum only detects edited or damaged files; it is not meant as strong protection.
 */
public final class BackupManager {
    private static final String FORMAT = "skillassessment-backup";
    private static final int VERSION = 1;
    private static final String SALT = "golbarg.skillassessment.backup.v1";
    private static final int MAX_BYTES = 20 * 1024 * 1024;

    private BackupManager() {
    }

    @WorkerThread
    public static void export(Context context, Uri uri) throws Exception {
        Context app = context.getApplicationContext();
        QuizRepository repository = QuizRepository.get(app);
        SQLiteDatabase db = DatabaseHandler.getInstance(app).getReadableDatabase();

        JSONObject payload = new JSONObject();
        payload.put("version", VERSION);
        payload.put("exportedAt", System.currentTimeMillis());
        payload.put("credits", repository.getCredits());
        payload.put("userName", Prefs.getUserName(app));

        JSONArray topics = new JSONArray();
        for (Category c : repository.getCategories()) if (c.isUnlocked()) topics.put(c.getId());
        payload.put("topics", topics);

        JSONArray results = new JSONArray();
        try (Cursor c = db.rawQuery("SELECT id, category_id, correct_answer, wrong_answer, no_answer, created_at, duration_ms, exam FROM "
                + DatabaseHandler.T_RESULT + " ORDER BY id", null)) {
            while (c.moveToNext()) {
                JSONObject r = new JSONObject();
                r.put("category_id", c.getInt(1));
                r.put("correct", c.getInt(2));
                r.put("wrong", c.getInt(3));
                r.put("no_answer", c.getInt(4));
                r.put("created_at", c.getLong(5));
                r.put("duration_ms", c.getLong(6));
                r.put("exam", c.getInt(7));
                JSONArray items = new JSONArray();
                try (Cursor i = db.rawQuery("SELECT position, question_id, selected, outcome FROM " + DatabaseHandler.T_RESULT_ITEM
                        + " WHERE result_id = ? ORDER BY position", new String[]{String.valueOf(c.getLong(0))})) {
                    while (i.moveToNext()) {
                        JSONObject item = new JSONObject();
                        item.put("position", i.getInt(0));
                        item.put("question_id", i.getInt(1));
                        item.put("selected", i.isNull(2) ? "" : i.getString(2));
                        item.put("outcome", i.getInt(3));
                        items.put(item);
                    }
                }
                r.put("items", items);
                results.put(r);
            }
        }
        payload.put("results", results);

        JSONArray bookmarks = new JSONArray();
        try (Cursor c = db.rawQuery("SELECT question_id FROM " + DatabaseHandler.T_BOOKMARK + " ORDER BY id", null)) {
            while (c.moveToNext()) bookmarks.put(c.getInt(0));
        }
        payload.put("bookmarks", bookmarks);

        JSONArray review = new JSONArray();
        try (Cursor c = db.rawQuery("SELECT question_id, box, due_at, updated_at FROM " + DatabaseHandler.T_REVIEW, null)) {
            while (c.moveToNext()) {
                JSONObject r = new JSONObject();
                r.put("question_id", c.getInt(0));
                r.put("box", c.getInt(1));
                r.put("due_at", c.getLong(2));
                r.put("updated_at", c.getLong(3));
                review.put(r);
            }
        }
        payload.put("review", review);

        JSONObject progress = new JSONObject();
        for (Map.Entry<String, ?> e : ProgressTracker.exportState(app).entrySet()) {
            Object v = e.getValue();
            String type = v instanceof Long ? "long" : v instanceof Integer ? "int" : v instanceof Boolean ? "bool" : v instanceof String ? "string" : null;
            if (type == null) continue;
            progress.put(e.getKey(), new JSONObject().put("t", type).put("v", v));
        }
        payload.put("progress", progress);

        String body = payload.toString();
        JSONObject file = new JSONObject()
                .put("format", FORMAT)
                .put("payload", body)
                .put("checksum", checksum(body));
        try (OutputStream out = app.getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) throw new IllegalStateException("Cannot open " + uri);
            out.write(file.toString(2).getBytes(StandardCharsets.UTF_8));
        }
    }

    /** Replaces current progress with the backup's. Throws if the file isn't a valid backup. */
    @WorkerThread
    public static void restore(Context context, Uri uri) throws Exception {
        Context app = context.getApplicationContext();
        String raw;
        try (InputStream in = app.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalStateException("Cannot open " + uri);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[16 * 1024];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
                if (out.size() > MAX_BYTES) throw new IllegalStateException("Backup too large");
            }
            raw = out.toString("UTF-8");
        }
        JSONObject file = new JSONObject(raw);
        if (!FORMAT.equals(file.optString("format"))) throw new IllegalArgumentException("Not a backup file");
        String body = file.getString("payload");
        if (!checksum(body).equals(file.optString("checksum"))) throw new SecurityException("Backup was modified");
        JSONObject payload = new JSONObject(body);
        if (payload.optInt("version") > VERSION) throw new IllegalArgumentException("Backup from a newer version");

        QuizRepository repository = QuizRepository.get(app);
        // Topics first: each import runs its own transaction and is free when restoring.
        JSONArray topics = payload.optJSONArray("topics");
        if (topics != null) {
            for (int i = 0; i < topics.length(); i++) {
                int id = topics.getInt(i);
                if (!repository.categoryRowExists(id)) repository.unlockCategory(id, true);
            }
        }

        SQLiteDatabase db = DatabaseHandler.getInstance(app).getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(DatabaseHandler.T_RESULT_ITEM, null, null);
            db.delete(DatabaseHandler.T_RESULT, null, null);
            db.delete(DatabaseHandler.T_BOOKMARK, null, null);
            db.delete(DatabaseHandler.T_REVIEW, null, null);

            JSONArray results = payload.optJSONArray("results");
            for (int i = 0; results != null && i < results.length(); i++) {
                JSONObject r = results.getJSONObject(i);
                ContentValues cv = new ContentValues();
                cv.put("category_id", r.getInt("category_id"));
                cv.put("correct_answer", r.getInt("correct"));
                cv.put("wrong_answer", r.getInt("wrong"));
                cv.put("no_answer", r.getInt("no_answer"));
                cv.put("created_at", r.optLong("created_at"));
                cv.put("duration_ms", r.optLong("duration_ms"));
                cv.put("exam", r.optInt("exam"));
                long resultId = db.insertOrThrow(DatabaseHandler.T_RESULT, null, cv);
                JSONArray items = r.optJSONArray("items");
                for (int j = 0; items != null && j < items.length(); j++) {
                    JSONObject item = items.getJSONObject(j);
                    ContentValues iv = new ContentValues();
                    iv.put("result_id", resultId);
                    iv.put("position", item.getInt("position"));
                    iv.put("question_id", item.getInt("question_id"));
                    iv.put("selected", item.optString("selected"));
                    iv.put("outcome", item.getInt("outcome"));
                    db.insertOrThrow(DatabaseHandler.T_RESULT_ITEM, null, iv);
                }
            }

            JSONArray bookmarks = payload.optJSONArray("bookmarks");
            for (int i = 0; bookmarks != null && i < bookmarks.length(); i++) {
                ContentValues cv = new ContentValues();
                cv.put("question_id", bookmarks.getInt(i));
                db.insertOrThrow(DatabaseHandler.T_BOOKMARK, null, cv);
            }

            JSONArray review = payload.optJSONArray("review");
            for (int i = 0; review != null && i < review.length(); i++) {
                JSONObject r = review.getJSONObject(i);
                ContentValues cv = new ContentValues();
                cv.put("question_id", r.getInt("question_id"));
                cv.put("box", r.getInt("box"));
                cv.put("due_at", r.getLong("due_at"));
                cv.put("updated_at", r.optLong("updated_at"));
                db.insertWithOnConflict(DatabaseHandler.T_REVIEW, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        repository.setCredits(payload.optInt("credits", repository.getCredits()));
        String name = payload.optString("userName", "");
        if (!name.isEmpty()) Prefs.setUserName(app, name);

        JSONObject progress = payload.optJSONObject("progress");
        if (progress != null) {
            Map<String, Object> values = new HashMap<>();
            Iterator<String> keys = progress.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject entry = progress.getJSONObject(key);
                switch (entry.getString("t")) {
                    case "long": values.put(key, entry.getLong("v")); break;
                    case "int": values.put(key, entry.getInt("v")); break;
                    case "bool": values.put(key, entry.getBoolean("v")); break;
                    case "string": values.put(key, entry.getString("v")); break;
                    default: break;
                }
            }
            ProgressTracker.importState(app, values);
        }
    }

    private static String checksum(String body) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(SALT.getBytes(StandardCharsets.UTF_8));
        byte[] hash = digest.digest(body.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format(Locale.ROOT, "%02x", b));
        return sb.toString();
    }
}

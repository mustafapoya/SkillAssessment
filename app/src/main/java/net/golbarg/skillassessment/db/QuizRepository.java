package net.golbarg.skillassessment.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.golbarg.skillassessment.billing.BillingManager;
import net.golbarg.skillassessment.models.AnswerResponseType;
import net.golbarg.skillassessment.models.Bookmark;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.models.Question;
import net.golbarg.skillassessment.models.QuestionAnswer;
import net.golbarg.skillassessment.models.QuestionResult;
import net.golbarg.skillassessment.models.ResultItem;
import net.golbarg.skillassessment.util.CryptUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * All data access for the app. Methods are blocking and must be called off the main thread
 * (see {@link net.golbarg.skillassessment.util.Async}).
 */
public final class QuizRepository {
    private static final String TAG = "QuizRepository";

    public static final int DEFAULT_CREDIT = 5;
    public static final int UNLOCK_COST = 2;
    public static final int REWARD_CREDIT = 1;
    private static final String KEY_CREDIT = "KEY_CREDIT";

    public enum UnlockResult { SUCCESS, NOT_ENOUGH_COINS, FAILED }

    /** Aggregated numbers for the progress screen. */
    public static final class Stats {
        public int tests;
        public int correct;
        public int wrong;
        public int skipped;
        public int unlockedTopics;

        public int answered() { return correct + wrong + skipped; }

        public int accuracyPercent() {
            int total = answered();
            return total == 0 ? 0 : Math.round(correct * 100f / total);
        }
    }

    private static volatile QuizRepository instance;

    private final Context appContext;
    private final DatabaseHandler dbHandler;
    private List<Category> catalogCache;

    public static QuizRepository get(Context context) {
        if (instance == null) {
            synchronized (QuizRepository.class) {
                if (instance == null) instance = new QuizRepository(context.getApplicationContext());
            }
        }
        return instance;
    }

    private QuizRepository(Context context) {
        this.appContext = context;
        this.dbHandler = DatabaseHandler.getInstance(context);
    }

    private SQLiteDatabase db() {
        return dbHandler.getWritableDatabase();
    }

    // region Categories

    /** The full catalog with unlock state and best scores filled in. */
    public List<Category> getCategories() {
        Map<Integer, Integer> importedCounts = new HashMap<>();
        try (Cursor c = db().rawQuery("SELECT c.id, COUNT(q.id) FROM " + DatabaseHandler.T_CATEGORY + " c LEFT JOIN "
                + DatabaseHandler.T_QUESTION + " q ON q.category_id = c.id GROUP BY c.id", null)) {
            while (c.moveToNext()) importedCounts.put(c.getInt(0), c.getInt(1));
        }

        Map<Integer, int[]> stats = new HashMap<>();
        try (Cursor c = db().rawQuery("SELECT category_id, COUNT(*), MAX(ROUND(correct_answer * 100.0 / (correct_answer + wrong_answer + no_answer))) FROM "
                + DatabaseHandler.T_RESULT + " WHERE correct_answer + wrong_answer + no_answer > 0 GROUP BY category_id", null)) {
            while (c.moveToNext()) stats.put(c.getInt(0), new int[]{c.getInt(1), c.getInt(2)});
        }

        Map<Integer, Integer> mastery = getMasteryCounts();

        List<Category> result = new ArrayList<>();
        for (Category entry : getCatalog()) {
            Category category = new Category(entry.getId(), entry.getSlug(), entry.getNumberOfQuestion());
            Integer imported = importedCounts.get(category.getId());
            category.setUnlocked(imported != null && imported > 0 && imported >= category.getNumberOfQuestion());
            Integer mastered = mastery.get(category.getId());
            if (mastered != null) category.setMastered(mastered);
            int[] s = stats.get(category.getId());
            if (s != null) {
                category.setAttempts(s[0]);
                category.setBestScore(s[1]);
            }
            result.add(category);
        }
        return result;
    }

    @Nullable
    public Category getCategory(int id) {
        if (id < 0) return Category.pseudo(id, 0);
        for (Category category : getCategories()) {
            if (category.getId() == id) return category;
        }
        return null;
    }

    private synchronized List<Category> getCatalog() {
        if (catalogCache == null) {
            List<Category> list = new ArrayList<>();
            try (InputStream in = appContext.getAssets().open("categories.json")) {
                JSONArray array = new JSONObject(readAll(in)).getJSONArray("categories");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject o = array.getJSONObject(i);
                    list.add(new Category(o.getInt("id"), o.getString("title"), o.getInt("number_of_question")));
                }
            } catch (Exception e) {
                Log.e(TAG, "Unable to read categories.json", e);
            }
            catalogCache = Collections.unmodifiableList(list);
        }
        return catalogCache;
    }

    /**
     * Copies a topic's questions from the bundled asset into the database and charges the unlock
     * cost. A topic that was already paid for (e.g. an interrupted import in an older version) is
     * repaired for free.
     */
    public synchronized UnlockResult unlockCategory(int categoryId) {
        Category catalogEntry = null;
        for (Category c : getCatalog()) if (c.getId() == categoryId) catalogEntry = c;
        if (catalogEntry == null) return UnlockResult.FAILED;

        boolean alreadyPaid = categoryRowExists(categoryId) || BillingManager.isPremium(appContext);
        if (!alreadyPaid && getCredits() < UNLOCK_COST) return UnlockResult.NOT_ENOUGH_COINS;

        List<Question> questions;
        try {
            questions = readQuestionsFromAsset(categoryId);
        } catch (Exception e) {
            Log.e(TAG, "Unable to read questions for category " + categoryId, e);
            return UnlockResult.FAILED;
        }
        if (questions.isEmpty()) return UnlockResult.FAILED;

        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            String[] idArg = {String.valueOf(categoryId)};
            db.delete(DatabaseHandler.T_ANSWER, "question_id IN (SELECT id FROM " + DatabaseHandler.T_QUESTION + " WHERE category_id = ?)", idArg);
            db.delete(DatabaseHandler.T_QUESTION, "category_id = ?", idArg);
            db.delete(DatabaseHandler.T_CATEGORY, "id = ?", idArg);

            ContentValues cv = new ContentValues();
            cv.put("id", catalogEntry.getId());
            cv.put("title", catalogEntry.getSlug());
            cv.put("number_of_question", catalogEntry.getNumberOfQuestion());
            db.insertOrThrow(DatabaseHandler.T_CATEGORY, null, cv);

            for (Question q : questions) {
                ContentValues qv = new ContentValues();
                qv.put("id", q.getId());
                qv.put("category_id", q.getCategoryId());
                qv.put("number", q.getNumber());
                qv.put("title", q.getTitle());
                qv.put("number_of_correct_answer", q.getRequiredSelections());
                db.insertOrThrow(DatabaseHandler.T_QUESTION, null, qv);
                for (QuestionAnswer a : q.getAnswers()) {
                    ContentValues av = new ContentValues();
                    av.put("question_id", a.getQuestionId());
                    av.put("number", a.getNumber());
                    av.put("title", a.getTitle());
                    av.put("is_correct", a.isCorrect() ? "1" : "0");
                    db.insertOrThrow(DatabaseHandler.T_ANSWER, null, av);
                }
            }
            if (!alreadyPaid) {
                writeCredits(db, getCredits() - UNLOCK_COST);
            }
            db.setTransactionSuccessful();
            return UnlockResult.SUCCESS;
        } catch (Exception e) {
            Log.e(TAG, "Import failed for category " + categoryId, e);
            return UnlockResult.FAILED;
        } finally {
            db.endTransaction();
        }
    }

    private boolean categoryRowExists(int categoryId) {
        try (Cursor c = db().rawQuery("SELECT 1 FROM " + DatabaseHandler.T_CATEGORY + " WHERE id = ?", new String[]{String.valueOf(categoryId)})) {
            return c.moveToFirst();
        }
    }

    /** Streams questions.json so only the requested topic is materialised in memory. */
    private List<Question> readQuestionsFromAsset(int categoryId) throws Exception {
        List<Question> result = new ArrayList<>();
        try (JsonReader reader = new JsonReader(new BufferedReader(new InputStreamReader(appContext.getAssets().open("questions.json"), StandardCharsets.UTF_8)))) {
            reader.beginObject();
            while (reader.hasNext()) {
                if (!"questions".equals(reader.nextName())) {
                    reader.skipValue();
                    continue;
                }
                reader.beginArray();
                while (reader.hasNext()) {
                    int id = -1;
                    reader.beginObject();
                    while (reader.hasNext()) {
                        String name = reader.nextName();
                        if ("id".equals(name)) {
                            id = reader.nextInt();
                        } else if ("questions".equals(name) && id == categoryId) {
                            reader.beginArray();
                            while (reader.hasNext()) result.add(readQuestion(reader, categoryId));
                            reader.endArray();
                        } else {
                            reader.skipValue();
                        }
                    }
                    reader.endObject();
                    if (id == categoryId) return result;
                }
                reader.endArray();
            }
            reader.endObject();
        }
        return result;
    }

    private static Question readQuestion(JsonReader reader, int categoryId) throws Exception {
        int id = 0, number = 0, numberOfCorrect = 1;
        String title = "";
        List<QuestionAnswer> answers = new ArrayList<>();
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                continue;
            }
            switch (name) {
                case "id": id = reader.nextInt(); break;
                case "number": number = reader.nextInt(); break;
                case "title": title = reader.nextString(); break;
                case "number_of_correct": numberOfCorrect = reader.nextInt(); break;
                case "answers":
                    reader.beginArray();
                    while (reader.hasNext()) answers.add(readAnswer(reader));
                    reader.endArray();
                    break;
                default: reader.skipValue();
            }
        }
        reader.endObject();
        Question question = new Question(id, categoryId, number, title, numberOfCorrect);
        for (QuestionAnswer a : answers) {
            question.getAnswers().add(new QuestionAnswer(id, a.getNumber(), a.getTitle(), a.isCorrect()));
        }
        return question;
    }

    private static QuestionAnswer readAnswer(JsonReader reader) throws Exception {
        int number = 0;
        String title = "";
        boolean correct = false;
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                continue;
            }
            switch (name) {
                case "number": number = reader.nextInt(); break;
                case "title": title = reader.nextString(); break;
                case "is_correct":
                    correct = reader.peek() == JsonToken.BOOLEAN ? reader.nextBoolean() : "true".equalsIgnoreCase(reader.nextString());
                    break;
                default: reader.skipValue();
            }
        }
        reader.endObject();
        return new QuestionAnswer(0, number, title, correct);
    }

    // endregion

    // region Questions

    public List<Question> getQuestions(int categoryId) {
        LinkedHashMap<Integer, Question> byId = new LinkedHashMap<>();
        try (Cursor c = db().rawQuery("SELECT id, category_id, number, title, number_of_correct_answer FROM " + DatabaseHandler.T_QUESTION
                + " WHERE category_id = ? ORDER BY number, id", new String[]{String.valueOf(categoryId)})) {
            while (c.moveToNext()) byId.put(c.getInt(0), mapQuestion(c));
        }
        attachAnswers(byId, "question_id IN (SELECT id FROM " + DatabaseHandler.T_QUESTION + " WHERE category_id = ?)", new String[]{String.valueOf(categoryId)});
        return new ArrayList<>(byId.values());
    }

    public Map<Integer, Question> getQuestionsByIds(Collection<Integer> ids) {
        LinkedHashMap<Integer, Question> byId = new LinkedHashMap<>();
        if (ids.isEmpty()) return byId;
        String in = "(" + TextUtils.join(",", ids) + ")";
        try (Cursor c = db().rawQuery("SELECT id, category_id, number, title, number_of_correct_answer FROM " + DatabaseHandler.T_QUESTION
                + " WHERE id IN " + in, null)) {
            while (c.moveToNext()) byId.put(c.getInt(0), mapQuestion(c));
        }
        attachAnswers(byId, "question_id IN " + in, null);
        return byId;
    }

    private static Question mapQuestion(Cursor c) {
        return new Question(c.getInt(0), c.getInt(1), c.getInt(2), c.getString(3), c.getInt(4));
    }

    private void attachAnswers(Map<Integer, Question> byId, String where, String[] args) {
        try (Cursor c = db().rawQuery("SELECT question_id, number, title, is_correct FROM " + DatabaseHandler.T_ANSWER
                + " WHERE " + where + " ORDER BY question_id, number, id", args)) {
            while (c.moveToNext()) {
                Question q = byId.get(c.getInt(0));
                if (q == null) continue;
                String flag = c.getString(3);
                boolean correct = "1".equals(flag) || "true".equalsIgnoreCase(flag);
                q.getAnswers().add(new QuestionAnswer(c.getInt(0), c.getInt(1), c.getString(2), correct));
            }
        }
    }

    // endregion

    // region Bookmarks

    public Set<Integer> getBookmarkedQuestionIds() {
        Set<Integer> ids = new HashSet<>();
        try (Cursor c = db().rawQuery("SELECT question_id FROM " + DatabaseHandler.T_BOOKMARK, null)) {
            while (c.moveToNext()) ids.add(c.getInt(0));
        }
        return ids;
    }

    public void setBookmarked(int questionId, boolean bookmarked) {
        SQLiteDatabase db = db();
        String[] args = {String.valueOf(questionId)};
        db.delete(DatabaseHandler.T_BOOKMARK, "question_id = ?", args);
        if (bookmarked) {
            ContentValues cv = new ContentValues();
            cv.put("question_id", questionId);
            db.insert(DatabaseHandler.T_BOOKMARK, null, cv);
        }
    }

    /** Newest first. Bookmarks whose topic data is missing are skipped. */
    public List<Bookmark> getBookmarks() {
        List<int[]> rows = new ArrayList<>();
        try (Cursor c = db().rawQuery("SELECT id, question_id FROM " + DatabaseHandler.T_BOOKMARK + " ORDER BY id DESC", null)) {
            while (c.moveToNext()) rows.add(new int[]{c.getInt(0), c.getInt(1)});
        }
        List<Integer> ids = new ArrayList<>();
        for (int[] r : rows) ids.add(r[1]);
        Map<Integer, Question> questions = getQuestionsByIds(ids);
        Map<Integer, Category> categories = new HashMap<>();
        for (Category c : getCatalog()) categories.put(c.getId(), c);

        List<Bookmark> result = new ArrayList<>();
        for (int[] r : rows) {
            Question q = questions.get(r[1]);
            if (q == null) continue;
            Category category = categories.get(q.getCategoryId());
            if (category == null) category = new Category(q.getCategoryId(), "", 0);
            result.add(new Bookmark(r[0], q, category));
        }
        return result;
    }

    // endregion

    // region Results

    public long saveResult(@NonNull QuestionResult result, @NonNull List<ResultItem> items) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put("category_id", result.getCategoryId());
            cv.put("correct_answer", result.getCorrectAnswer());
            cv.put("wrong_answer", result.getWrongAnswer());
            cv.put("no_answer", result.getNoAnswer());
            cv.put("created_at", result.getCreatedAt());
            cv.put("duration_ms", result.getDurationMs());
            long id = db.insertOrThrow(DatabaseHandler.T_RESULT, null, cv);
            for (int i = 0; i < items.size(); i++) {
                ResultItem item = items.get(i);
                ContentValues iv = new ContentValues();
                iv.put("result_id", id);
                iv.put("position", i);
                iv.put("question_id", item.getQuestionId());
                iv.put("selected", TextUtils.join(",", item.getSelectedPositions()));
                iv.put("outcome", item.getOutcome().code);
                db.insertOrThrow(DatabaseHandler.T_RESULT_ITEM, null, iv);
                scheduleReview(db, item);
            }
            db.setTransactionSuccessful();
            result.setId(id);
            return id;
        } finally {
            db.endTransaction();
        }
    }

    @Nullable
    public QuestionResult getResult(long id) {
        try (Cursor c = db().rawQuery(RESULT_COLUMNS + " WHERE id = ?", new String[]{String.valueOf(id)})) {
            return c.moveToFirst() ? mapResult(c) : null;
        }
    }

    /** Newest first. */
    public List<QuestionResult> getResults() {
        List<QuestionResult> list = new ArrayList<>();
        try (Cursor c = db().rawQuery(RESULT_COLUMNS + " ORDER BY id DESC", null)) {
            while (c.moveToNext()) list.add(mapResult(c));
        }
        return list;
    }

    private static final String RESULT_COLUMNS = "SELECT id, category_id, correct_answer, wrong_answer, no_answer, created_at, duration_ms FROM " + DatabaseHandler.T_RESULT;

    private static QuestionResult mapResult(Cursor c) {
        return new QuestionResult(c.getLong(0), c.getInt(1), c.getInt(2), c.getInt(3), c.getInt(4), c.getLong(5), c.getLong(6));
    }

    /** Answers of a finished test with their questions attached, in the order they were asked. */
    public List<ResultItem> getResultItems(long resultId) {
        List<ResultItem> items = new ArrayList<>();
        try (Cursor c = db().rawQuery("SELECT question_id, selected, outcome FROM " + DatabaseHandler.T_RESULT_ITEM
                + " WHERE result_id = ? ORDER BY position", new String[]{String.valueOf(resultId)})) {
            while (c.moveToNext()) {
                List<Integer> selected = new ArrayList<>();
                String raw = c.getString(1);
                if (!TextUtils.isEmpty(raw)) {
                    for (String part : raw.split(",")) selected.add(Integer.parseInt(part.trim()));
                }
                items.add(new ResultItem(c.getInt(0), selected, AnswerResponseType.fromCode(c.getInt(2))));
            }
        }
        List<Integer> ids = new ArrayList<>();
        for (ResultItem item : items) ids.add(item.getQuestionId());
        Map<Integer, Question> questions = getQuestionsByIds(ids);
        List<ResultItem> resolved = new ArrayList<>();
        for (ResultItem item : items) {
            Question q = questions.get(item.getQuestionId());
            if (q == null) continue;
            item.setQuestion(q);
            resolved.add(item);
        }
        return resolved;
    }

    public Stats getStats() {
        Stats stats = new Stats();
        try (Cursor c = db().rawQuery("SELECT COUNT(*), COALESCE(SUM(correct_answer), 0), COALESCE(SUM(wrong_answer), 0), COALESCE(SUM(no_answer), 0) FROM "
                + DatabaseHandler.T_RESULT, null)) {
            if (c.moveToFirst()) {
                stats.tests = c.getInt(0);
                stats.correct = c.getInt(1);
                stats.wrong = c.getInt(2);
                stats.skipped = c.getInt(3);
            }
        }
        for (Category category : getCategories()) {
            if (category.isUnlocked()) stats.unlockedTopics++;
        }
        return stats;
    }

    public void clearHistory() {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            db.delete(DatabaseHandler.T_RESULT_ITEM, null, null);
            db.delete(DatabaseHandler.T_RESULT, null, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // endregion

    // region Spaced review, mastery and mixed tests

    /** Days until a question in each Leitner box comes back. Box 0 is due immediately. */
    private static final int[] REVIEW_INTERVAL_DAYS = {0, 1, 3, 7, 14};
    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    /**
     * Wrong or skipped answers enter the review queue (box 0, due now). Each later correct answer
     * moves the question to a longer interval; after the last box it counts as learned and leaves.
     */
    private static void scheduleReview(SQLiteDatabase db, ResultItem item) {
        long now = System.currentTimeMillis();
        String[] args = {String.valueOf(item.getQuestionId())};
        if (item.getOutcome() != AnswerResponseType.CORRECT) {
            ContentValues cv = new ContentValues();
            cv.put("question_id", item.getQuestionId());
            cv.put("box", 0);
            cv.put("due_at", now);
            cv.put("updated_at", now);
            db.insertWithOnConflict(DatabaseHandler.T_REVIEW, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            return;
        }
        int box = -1;
        try (Cursor c = db.rawQuery("SELECT box FROM " + DatabaseHandler.T_REVIEW + " WHERE question_id = ?", args)) {
            if (c.moveToFirst()) box = c.getInt(0);
        }
        if (box < 0) return;
        int next = box + 1;
        if (next >= REVIEW_INTERVAL_DAYS.length) {
            db.delete(DatabaseHandler.T_REVIEW, "question_id = ?", args);
            return;
        }
        ContentValues cv = new ContentValues();
        cv.put("box", next);
        cv.put("due_at", now + REVIEW_INTERVAL_DAYS[next] * DAY_MS);
        cv.put("updated_at", now);
        db.update(DatabaseHandler.T_REVIEW, cv, "question_id = ?", args);
    }

    /** Review questions due now whose topic data is still installed. */
    public int getReviewDueCount() {
        try (Cursor c = db().rawQuery("SELECT COUNT(*) FROM " + DatabaseHandler.T_REVIEW + " r JOIN " + DatabaseHandler.T_QUESTION
                + " q ON q.id = r.question_id WHERE r.due_at <= ?", new String[]{String.valueOf(System.currentTimeMillis())})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    /** Due review questions, most overdue first. */
    public List<Question> getReviewQuestions(int limit) {
        List<Integer> ids = new ArrayList<>();
        try (Cursor c = db().rawQuery("SELECT r.question_id FROM " + DatabaseHandler.T_REVIEW + " r JOIN " + DatabaseHandler.T_QUESTION
                + " q ON q.id = r.question_id WHERE r.due_at <= ? ORDER BY r.due_at, r.box LIMIT " + limit,
                new String[]{String.valueOf(System.currentTimeMillis())})) {
            while (c.moveToNext()) ids.add(c.getInt(0));
        }
        return ordered(ids);
    }

    /** Questions answered correctly at least once, per topic. */
    public Map<Integer, Integer> getMasteryCounts() {
        Map<Integer, Integer> result = new HashMap<>();
        try (Cursor c = db().rawQuery("SELECT q.category_id, COUNT(DISTINCT ri.question_id) FROM " + DatabaseHandler.T_RESULT_ITEM
                + " ri JOIN " + DatabaseHandler.T_QUESTION + " q ON q.id = ri.question_id WHERE ri.outcome = 0 GROUP BY q.category_id", null)) {
            while (c.moveToNext()) result.put(c.getInt(0), c.getInt(1));
        }
        return result;
    }

    public boolean hasUnlockedTopics() {
        try (Cursor c = db().rawQuery("SELECT 1 FROM " + DatabaseHandler.T_QUESTION + " LIMIT 1", null)) {
            return c.moveToFirst();
        }
    }

    /**
     * Random questions across every unlocked topic. With a {@code seed} the selection is stable,
     * which gives everyone the same daily challenge for a given day and set of topics.
     */
    public List<Question> getMixedQuestions(int count, @Nullable Long seed) {
        List<Integer> ids = new ArrayList<>();
        try (Cursor c = db().rawQuery("SELECT id FROM " + DatabaseHandler.T_QUESTION + " ORDER BY id", null)) {
            while (c.moveToNext()) ids.add(c.getInt(0));
        }
        if (seed == null) Collections.shuffle(ids);
        else Collections.shuffle(ids, new java.util.Random(seed));
        List<Integer> picked = new ArrayList<>();
        Map<Integer, Question> all = getQuestionsByIds(ids.subList(0, Math.min(ids.size(), count * 2)));
        for (Integer id : ids) {
            Question q = all.get(id);
            if (q != null && q.getAnswers().size() >= 2) picked.add(id);
            if (picked.size() >= count) break;
        }
        return ordered(picked);
    }

    /** Topic slug for every catalog id; used to resolve images and code highlighting in mixed tests. */
    public Map<Integer, String> getSlugs() {
        Map<Integer, String> slugs = new HashMap<>();
        for (Category c : getCatalog()) slugs.put(c.getId(), c.getSlug());
        return slugs;
    }

    private List<Question> ordered(List<Integer> ids) {
        Map<Integer, Question> byId = getQuestionsByIds(ids);
        List<Question> result = new ArrayList<>();
        for (Integer id : ids) {
            Question q = byId.get(id);
            if (q != null) result.add(q);
        }
        return result;
    }

    // endregion

    // region Coins

    /** Current coin balance. Stored obfuscated for compatibility with version 1. */
    public synchronized int getCredits() {
        try (Cursor c = db().rawQuery("SELECT value FROM " + DatabaseHandler.T_CONFIG + " WHERE key = ?", new String[]{KEY_CREDIT})) {
            if (c.moveToFirst()) {
                return Math.max(0, Integer.parseInt(CryptUtil.decrypt(c.getString(0)).trim()));
            }
        } catch (Exception e) {
            Log.w(TAG, "Unreadable credit value, resetting", e);
        }
        writeCredits(db(), DEFAULT_CREDIT);
        return DEFAULT_CREDIT;
    }

    public synchronized int addCredits(int delta) {
        int value = Math.max(0, getCredits() + delta);
        writeCredits(db(), value);
        return value;
    }

    private static void writeCredits(SQLiteDatabase db, int value) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("key", KEY_CREDIT);
            cv.put("value", CryptUtil.encrypt(String.valueOf(Math.max(0, value))));
            cv.put("updated_at", System.currentTimeMillis());
            if (db.update(DatabaseHandler.T_CONFIG, cv, "key = ?", new String[]{KEY_CREDIT}) == 0) {
                db.insert(DatabaseHandler.T_CONFIG, null, cv);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to store credits", e);
        }
    }

    // endregion

    private static String readAll(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            char[] buffer = new char[8192];
            int n;
            while ((n = reader.read(buffer)) != -1) sb.append(buffer, 0, n);
        }
        return sb.toString();
    }
}

package net.golbarg.skillassessment.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Single shared SQLite helper. The schema is kept compatible with version 1 of the app so
 * that existing users keep their unlocked topics, coins, bookmarks and results on upgrade.
 */
public final class DatabaseHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "skill_assessment_db";

    public static final String T_CONFIG = "configs";
    public static final String T_CATEGORY = "categories";
    public static final String T_QUESTION = "questions";
    public static final String T_ANSWER = "question_answers";
    public static final String T_BOOKMARK = "bookmarks";
    public static final String T_RESULT = "question_results";
    public static final String T_RESULT_ITEM = "result_items";
    public static final String T_REVIEW = "review_items";

    private static volatile DatabaseHandler instance;

    public static DatabaseHandler getInstance(Context context) {
        if (instance == null) {
            synchronized (DatabaseHandler.class) {
                if (instance == null) {
                    instance = new DatabaseHandler(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        setWriteAheadLoggingEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Version 1 tables (unchanged column names)
        db.execSQL("CREATE TABLE " + T_CONFIG + " (id INTEGER PRIMARY KEY, key TEXT, value TEXT, updated_at LONG NULLABLE)");
        db.execSQL("CREATE TABLE " + T_CATEGORY + " (id INTEGER PRIMARY KEY, title TEXT, number_of_question INTEGER)");
        db.execSQL("CREATE TABLE " + T_QUESTION + " (id INTEGER PRIMARY KEY, category_id INTEGER, number INTEGER, title TEXT, number_of_correct_answer INTEGER)");
        db.execSQL("CREATE TABLE " + T_ANSWER + " (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, question_id INTEGER, number INTEGER, title TEXT, is_correct TEXT)");
        db.execSQL("CREATE TABLE " + T_BOOKMARK + " (id INTEGER PRIMARY KEY, question_id INTEGER)");
        db.execSQL("CREATE TABLE " + T_RESULT + " (id INTEGER PRIMARY KEY, category_id INTEGER, correct_answer INTEGER, wrong_answer INTEGER, no_answer INTEGER)");
        upgradeToV2(db);
        upgradeToV3(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            upgradeToV2(db);
        }
        if (oldVersion < 3) {
            upgradeToV3(db);
        }
    }

    private static void upgradeToV2(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + T_RESULT + " ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0");
        db.execSQL("ALTER TABLE " + T_RESULT + " ADD COLUMN duration_ms INTEGER NOT NULL DEFAULT 0");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_RESULT_ITEM + " (id INTEGER PRIMARY KEY AUTOINCREMENT, result_id INTEGER NOT NULL, position INTEGER NOT NULL, question_id INTEGER NOT NULL, selected TEXT, outcome INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_questions_category ON " + T_QUESTION + "(category_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_answers_question ON " + T_ANSWER + "(question_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookmarks_question ON " + T_BOOKMARK + "(question_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_result_items_result ON " + T_RESULT_ITEM + "(result_id)");
    }

    /** Spaced-repetition queue: missed questions come back after growing intervals (Leitner boxes). */
    private static void upgradeToV3(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_REVIEW + " (question_id INTEGER PRIMARY KEY, box INTEGER NOT NULL, due_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_review_due ON " + T_REVIEW + "(due_at)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_result_items_question ON " + T_RESULT_ITEM + "(question_id, outcome)");
        // Seed the queue with questions the user already got wrong in version 2.
        db.execSQL("INSERT OR IGNORE INTO " + T_REVIEW + " (question_id, box, due_at, updated_at) "
                + "SELECT question_id, 0, 0, 0 FROM " + T_RESULT_ITEM + " ri WHERE outcome != 0 "
                + "AND NOT EXISTS (SELECT 1 FROM " + T_RESULT_ITEM + " later WHERE later.question_id = ri.question_id AND later.outcome = 0 AND later.id > ri.id)");
    }
}

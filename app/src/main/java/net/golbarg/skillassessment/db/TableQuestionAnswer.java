package net.golbarg.skillassessment.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.golbarg.skillassessment.models.Question;
import net.golbarg.skillassessment.models.QuestionAnswer;

import java.util.ArrayList;

public class TableQuestionAnswer implements CRUDHandler<QuestionAnswer>{
    public static final String TABLE_NAME = "question_answers";
    public static final String KEY_ID = "id";
    public static final String KEY_QUESTION_ID = "question_id";
    public static final String KEY_NUMBER = "number";
    public static final String KEY_TITLE = "title";
    public static final String KEY_IS_CORRECT = "is_correct";
    public static final String [] ALL_COLUMNS = {KEY_ID, KEY_QUESTION_ID, KEY_NUMBER, KEY_TITLE, KEY_IS_CORRECT};

    private final DatabaseHandler dbHandler;

    public TableQuestionAnswer(DatabaseHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    public static String createTableQuery() {
        return String.format(
                "CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, %s INTEGER, %s INTEGER, %s TEXT, %s TEXT)",
                TABLE_NAME, KEY_ID, KEY_QUESTION_ID, KEY_NUMBER, KEY_TITLE, KEY_IS_CORRECT);
    }

    public static String dropTableQuery() {
        return "DROP TABLE IF EXISTS " + TABLE_NAME;
    }


    @Override
    public void create(QuestionAnswer object) {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        db.insert(TABLE_NAME, null, putValues(object));
        db.close();
    }

    public void create(ArrayList<QuestionAnswer> questionAnswers) {
        SQLiteDatabase db = dbHandler.getWritableDatabase();

        for (int i = 0; i < questionAnswers.size(); i++) {
            db.insert(TABLE_NAME, null, putValues(questionAnswers.get(i)));
        }
        db.close();
    }

    @Override
    public QuestionAnswer get(int id) {
        SQLiteDatabase db = dbHandler.getReadableDatabase();

        Cursor cursor = db.query(TABLE_NAME, ALL_COLUMNS, KEY_ID + "=?", new String[]{String.valueOf(id)}, null, null, null, null);

        if(cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            return mapColumn(cursor);
        } else {
            return null;
        }
    }

    @Override
    public ArrayList<QuestionAnswer> getAll() {
        ArrayList<QuestionAnswer> result = new ArrayList<>();
        String selectQuery = "SELECT  * FROM " + TABLE_NAME;

        SQLiteDatabase db = dbHandler.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()) {
            do {
                result.add(mapColumn(cursor));
            }while(cursor.moveToNext());
        }
        return result;
    }

    public ArrayList<QuestionAnswer> getAnswersOf(int questionId) {
        ArrayList<QuestionAnswer> result = new ArrayList<>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_NAME + " WHERE " + KEY_QUESTION_ID + " = " + questionId;

        SQLiteDatabase db = dbHandler.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()) {
            do {
                result.add(mapColumn(cursor));
            }while(cursor.moveToNext());
        }
        return result;
    }

    @Override
    public int update(QuestionAnswer object) {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        return db.update(TABLE_NAME, putValues(object), KEY_ID + "=?", new String[]{String.valueOf(object.getId())});
    }

    @Override
    public void delete(QuestionAnswer object) {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        db.delete(TABLE_NAME, KEY_ID + "= ?", new String[]{String.valueOf(object.getId())});
    }

    @Override
    public int getCount() {
        String countQuery = "SELECT * FROM " + TABLE_NAME;
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    @Override
    public QuestionAnswer mapColumn(Cursor cursor) {
        int is_correct = Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_IS_CORRECT)));

        return new QuestionAnswer(
                Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_ID))),
                Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_QUESTION_ID))),
                Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_NUMBER))),
                cursor.getString(cursor.getColumnIndex(KEY_TITLE)),
                is_correct == 1 ? true : false
        );
    }

    @Override
    public ContentValues putValues(QuestionAnswer object) {
        ContentValues values = new ContentValues();
        if (object.getId() != -1 && object.getId() != 0) {
            values.put(KEY_ID, object.getId());
        }
        values.put(KEY_QUESTION_ID, object.getQuestionId());
        values.put(KEY_NUMBER, object.getNumber());
        values.put(KEY_TITLE, object.getTitle());
        values.put(KEY_IS_CORRECT, object.isCorrect());
        return values;
    }

    @Override
    public void emptyTable() {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME);
    }
}

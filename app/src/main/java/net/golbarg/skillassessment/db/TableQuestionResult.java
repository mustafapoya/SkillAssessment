package net.golbarg.skillassessment.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.golbarg.skillassessment.models.QuestionResult;

import java.util.ArrayList;

public class TableQuestionResult implements CRUDHandler<QuestionResult>{
    public static final String TABLE_NAME = "question_results";
    public static final String KEY_ID = "id";
    public static final String KEY_CATEGORY_ID = "category_id";
    public static final String KEY_CORRECT_ANSWER = "correct_answer";
    public static final String KEY_WRONG_ANSWER = "wrong_answer";
    public static final String KEY_NO_ANSWER = "no_answer";
    public static final String [] ALL_COLUMNS = { KEY_ID, KEY_CATEGORY_ID, KEY_CORRECT_ANSWER, KEY_WRONG_ANSWER, KEY_NO_ANSWER };
    private final DatabaseHandler dbHandler;

    public TableQuestionResult(DatabaseHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    public static String createTableQuery() {
        return String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY, %s INTEGER, %s INTEGER, %s INTEGER, %s INTEGER)",
                TABLE_NAME, KEY_ID, KEY_CATEGORY_ID, KEY_CORRECT_ANSWER, KEY_WRONG_ANSWER, KEY_NO_ANSWER);
    }

    public static String dropTableQuery() {
        return "DROP TABLE IF EXISTS " + TABLE_NAME;
    }


    @Override
    public void create(QuestionResult object) {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        db.insert(TABLE_NAME, null, putValues(object));
        db.close();
    }

    @Override
    public QuestionResult get(int id) {
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
    public ArrayList<QuestionResult> getAll() {
        ArrayList<QuestionResult> result = new ArrayList<>();
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

    @Override
    public int update(QuestionResult object) {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        return db.update(TABLE_NAME, putValues(object), KEY_ID + "=?", new String[]{String.valueOf(object.getId())});
    }

    @Override
    public void delete(QuestionResult object) {
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
    public QuestionResult mapColumn(Cursor cursor) {
        return new QuestionResult(
                Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_ID))),
                Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_CATEGORY_ID))),
                Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_CORRECT_ANSWER))),
                Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_WRONG_ANSWER))),
                Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_NO_ANSWER)))
        );
    }

    @Override
    public ContentValues putValues(QuestionResult object) {
        ContentValues values = new ContentValues();
        if (object.getId() != -1 && object.getId() != 0) {
            values.put(KEY_ID, object.getId());
        }
        values.put(KEY_CATEGORY_ID, object.getCategoryId());
        values.put(KEY_CORRECT_ANSWER, object.getCorrectAnswer());
        values.put(KEY_WRONG_ANSWER, object.getWrongAnswer());
        values.put(KEY_NO_ANSWER, object.getNoAnswer());

        return values;
    }

    @Override
    public void emptyTable() {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME);
    }
}

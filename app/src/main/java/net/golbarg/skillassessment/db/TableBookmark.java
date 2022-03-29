package net.golbarg.skillassessment.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.golbarg.skillassessment.models.Bookmark;
import net.golbarg.skillassessment.models.Question;

import java.util.ArrayList;

public class TableBookmark implements CRUDHandler<Bookmark>{
    public static final String TABLE_NAME = "bookmarks";
    public static final String KEY_ID = "id";
    public static final String KEY_QUESTION_ID = "question_id";
    public static final String [] ALL_COLUMNS = {KEY_ID, KEY_QUESTION_ID};
    private DatabaseHandler dbHandler;

    public TableBookmark(DatabaseHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    public static String createTableQuery() {
        return String.format(
                "CREATE TABLE %s (%s INTEGER PRIMARY KEY, %s INTEGER)",
                TABLE_NAME, KEY_ID, KEY_QUESTION_ID);
    }

    public static String dropTableQuery() {
        return "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    @Override
    public void create(Bookmark object) {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        db.insert(TABLE_NAME, null, putValues(object));
        db.close();
    }

    @Override
    public Bookmark get(int id) {
        SQLiteDatabase db = dbHandler.getReadableDatabase();

        Cursor cursor = db.query(TABLE_NAME, ALL_COLUMNS, KEY_ID + "=?", new String[]{String.valueOf(id)}, null, null, null, null);

        if(cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            return mapColumn(cursor);
        } else {
            return null;
        }
    }

    public Bookmark getByQuestionId(int question_id) {
        SQLiteDatabase db = dbHandler.getReadableDatabase();

        Cursor cursor = db.query(TABLE_NAME, ALL_COLUMNS, KEY_QUESTION_ID + "=?", new String[]{String.valueOf(question_id)}, null, null, null, null);

        if(cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            return mapColumn(cursor);
        } else {
            return null;
        }
    }


    @Override
    public ArrayList<Bookmark> getAll() {
        ArrayList<Bookmark> result = new ArrayList<>();
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
    public int update(Bookmark object) {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        return db.update(TABLE_NAME, putValues(object), KEY_ID + "=?", new String[]{String.valueOf(object.getId())});
    }

    @Override
    public void delete(Bookmark object) {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        db.delete(TABLE_NAME, KEY_ID + "= ?", new String[]{String.valueOf(object.getId())});
    }

    public void deleteByQuestionId(int question_id) {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        db.delete(TABLE_NAME, KEY_QUESTION_ID + "= ?", new String[]{String.valueOf(question_id)});
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
    public Bookmark mapColumn(Cursor cursor) {
        return new Bookmark(
                Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_ID))),
                Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_QUESTION_ID)))
        );
    }

    @Override
    public ContentValues putValues(Bookmark object) {
        ContentValues values = new ContentValues();
        if (object.getId() != -1 && object.getId() != 0) {
            values.put(KEY_ID, object.getId());
        }
        values.put(KEY_QUESTION_ID, object.getQuestionId());
        return values;
    }

    @Override
    public void emptyTable() {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME);
    }
}

package net.golbarg.skillassessment.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import net.golbarg.skillassessment.models.Category;

import java.util.ArrayList;

public class TableCategory implements CRUDHandler<Category> {
    public static final String TABLE_NAME = "categories";
    public static final String KEY_ID = "id";
    public static final String KEY_TITLE = "title";
    public static final String KEY_NUMBER_OF_QUESTION = "number_of_question";
    public static final String [] ALL_COLUMNS = { KEY_ID, KEY_TITLE, KEY_NUMBER_OF_QUESTION };
    private final DatabaseHandler dbHandler;

    public TableCategory(DatabaseHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    public static String createTableQuery() {
        return String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY, %s TEXT, %s INTEGER)",
                TABLE_NAME, KEY_ID, KEY_TITLE, KEY_NUMBER_OF_QUESTION);
    }

    public static String dropTableQuery() {
        return String.format("DROP TABLE IF EXISTS %S", TABLE_NAME);
    }


    @Override
    public void create(Category object) {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        db.insert(TABLE_NAME, null, putValues(object));
        db.close();
    }

    @Override
    public Category get(int id) {
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
    public ArrayList<Category> getAll() {
        ArrayList<Category> result = new ArrayList<>();
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
    public int update(Category object) {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        return db.update(TABLE_NAME, putValues(object), KEY_ID + "=?", new String[]{String.valueOf(object.getId())});
    }

    @Override
    public void delete(Category object) {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        db.delete(TABLE_NAME, KEY_ID + "= ?", new String[]{String.valueOf(object.getId())});
    }

    public void delete(int id) {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        db.delete(TABLE_NAME, KEY_ID + "= ?", new String[]{String.valueOf(id)});
    }

    @Override
    public int getCount() {
        String countQuery = String.format("SELECT * FROM %s;", TABLE_NAME);
        SQLiteDatabase db = dbHandler.getReadableDatabase();

        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    @Override
    public Category mapColumn(Cursor cursor) {
        return new Category(
                Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_ID))),
                cursor.getString(cursor.getColumnIndex(KEY_TITLE)),
                Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_NUMBER_OF_QUESTION)))
        );
    }

    @Override
    public ContentValues putValues(Category object) {
        ContentValues values = new ContentValues();
        if (object.getId() != -1 && object.getId() != 0) {
            values.put(KEY_ID, object.getId());
        }
        values.put(KEY_TITLE, object.getTitle());
        values.put(KEY_NUMBER_OF_QUESTION, object.getNumberOfQuestion());
        return values;
    }

    @Override
    public void emptyTable() {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME);
    }
}

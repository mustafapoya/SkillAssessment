package net.golbarg.skillassessment.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import net.golbarg.skillassessment.models.Config;

public class TableConfig {
    public static final String TABLE_NAME = "configs";
    public static final String KEY_ID = "id";
    public static final String KEY_KEY = "key";
    public static final String KEY_VALUE = "value";
    public static final String KEY_UPDATED_AT = "updated_at";

    public static String createTableQuery() {
        return "CREATE TABLE " + TABLE_NAME +
                "( " + KEY_ID + " INTEGER PRIMARY KEY," +
                KEY_KEY + " TEXT, " +
                KEY_VALUE + " TEXT, " +
                KEY_UPDATED_AT + " long " +
                ")";
    }

    public static String dropTableQuery() {
        return "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    /**
     * add new config to db
     */
    public static void create(Config config, SQLiteOpenHelper helper) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.insert(TABLE_NAME, null, putValues(config));
        db.close();
    }

    public static Config getById(int id, SQLiteOpenHelper helper) {
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.query(TABLE_NAME, new String[]{KEY_ID, KEY_KEY, KEY_VALUE, KEY_UPDATED_AT}, KEY_ID + "=?", new String[]{String.valueOf(id)}, null, null, null, null);

        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            return mapConfig(cursor);
        } else {
            return null;
        }
    }

    public static Config getByKey(String key, SQLiteOpenHelper helper) {
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.query(TABLE_NAME, new String[]{KEY_ID, KEY_KEY, KEY_VALUE, KEY_UPDATED_AT}, KEY_KEY + "=?", new String[]{String.valueOf(key)}, null, null, null, null);

        if (cursor != null && cursor.getCount() != 0) {
            cursor.moveToFirst();
            return mapConfig(cursor);
        } else {
            return null;
        }
    }

    public static int updateById(Config config, SQLiteOpenHelper helper) {
        SQLiteDatabase db = helper.getWritableDatabase();

        return db.update(TABLE_NAME, putValues(config), KEY_ID + "=?", new String[]{String.valueOf(config.getId())});
    }

    public static int updateByKey(Config config, SQLiteOpenHelper helper) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.update(TABLE_NAME, putValues(config), KEY_KEY + "=?", new String[]{String.valueOf(config.getKey())});
    }

    public static void deleteById(Config config, SQLiteOpenHelper helper) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(TABLE_NAME, KEY_ID + "= ?", new String[]{String.valueOf(config.getId())});
    }

    public static void deleteByKey(Config config, SQLiteOpenHelper helper) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(TABLE_NAME, KEY_KEY + "= ?", new String[]{String.valueOf(config.getKey())});
    }

    public static void clearTable(SQLiteOpenHelper helper) {
        String query = "DELETE FROM " + TABLE_NAME;
        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL(query);
    }

    private static Config mapConfig(Cursor cursor) {
        return new Config(
                Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_ID))),
                cursor.getString(cursor.getColumnIndex(KEY_KEY)),
                cursor.getString(cursor.getColumnIndex(KEY_VALUE)),
                cursor.getLong(cursor.getColumnIndex(KEY_UPDATED_AT))
        );
    }

    private static ContentValues putValues(Config config) {
        ContentValues values = new ContentValues();
        if (config.getId() != -1) {
            values.put(KEY_ID, config.getId());
        }
        values.put(KEY_KEY, config.getKey());
        values.put(KEY_VALUE, config.getValue());
        values.put(KEY_UPDATED_AT, config.getUpdatedAt());
        return values;
    }
}

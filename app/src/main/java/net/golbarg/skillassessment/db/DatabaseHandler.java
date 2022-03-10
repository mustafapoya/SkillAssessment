package net.golbarg.skillassessment.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME ="skill_assessment_db";

    public DatabaseHandler(Context context) {
        //3rd argument to be passed is CursorFactory instance
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TableConfig.createTableQuery());
        db.execSQL(TableCategory.createTableQuery());
        db.execSQL(TableQuestion.createTableQuery());
        db.execSQL(TableQuestionAnswer.createTableQuery());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL(TableConfig.dropTableQuery());
        db.execSQL(TableCategory.dropTableQuery());
        db.execSQL(TableQuestion.dropTableQuery());
        db.execSQL(TableQuestionAnswer.dropTableQuery());
        // Create tables again
        onCreate(db);
    }
}

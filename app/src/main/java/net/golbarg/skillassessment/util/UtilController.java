package net.golbarg.skillassessment.util;

import android.content.Context;
import android.content.SharedPreferences;

public class UtilController {
    public static final String KEY_HEALTH = "KEY_HEALTH";
    public static final int DEFAULT_HEALTH = 5;
    public static final String KEY_SCORE = "KEY_SCORE";
    public static final int DEFAULT_SCORE = 500;
    public static final String KEY_SCORE_ON_TEST = "KEY_SCORE_ON_TEST";
    public static final int DEFAULT_SCORE_ON_TEST = 30;
    public static final String KEY_DB_STATUS = "KEY_DB_STATUS";


    public static SharedPreferences getSharedPref(Context context, String name) {
        return context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    public static boolean insertSharedPref(SharedPreferences sharedPref, String key, String value) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        return editor.commit();
    }

    public static boolean insertSharedPref(SharedPreferences sharedPref, String key, long value) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(key, value);
        return editor.commit();
    }

    public static boolean insertSharedPref(SharedPreferences sharedPref, String key, boolean value) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(key, value);
        return editor.commit();
    }

    public static boolean insertSharedPref(SharedPreferences sharedPref, String key, float value) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(key, value);
        return editor.commit();
    }

}

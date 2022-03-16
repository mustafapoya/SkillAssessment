package net.golbarg.skillassessment.util;

import android.content.Context;
import android.content.SharedPreferences;

import net.golbarg.skillassessment.models.Question;
import net.golbarg.skillassessment.models.QuestionCode;
import net.golbarg.skillassessment.models.QuestionPart;

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

    public static QuestionPart convertQuestionTextToQuestionObject(Question question) {
        QuestionPart part = new QuestionPart();
        String questionText = question.getTitle();

        if (questionText.indexOf("```") > -1) {
            int firstIndex = 0;
            int lastIndex = 0;

            do {

                firstIndex = questionText.indexOf("```", firstIndex);
                lastIndex = questionText.indexOf("```", firstIndex + 2);

                if (firstIndex > -1 && lastIndex > -1) {

                    String codeSection = questionText.substring(firstIndex, lastIndex + 3);
                    String programLang = codeSection.substring(3, codeSection.indexOf('\n') != -1 ? codeSection.indexOf('\n') : 3);
                    
                    String code = programLang.length() > 0
                            ? codeSection.substring(3 + programLang.length(), codeSection.length() - 3)
                            : codeSection.substring(3, codeSection.length() - 3);

                    questionText = questionText.replace(codeSection, "");

                    part.getCodeList().add(new QuestionCode(programLang, code));
                }
            } while (firstIndex > -1 && lastIndex > -1);

            part.setTitle(questionText);
        } else {
            part = new QuestionPart(questionText);
        }

        return part;
    }
}

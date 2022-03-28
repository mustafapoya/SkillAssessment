package net.golbarg.skillassessment.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.nevidelia.library.highlight.Highlight;

import net.golbarg.skillassessment.CustomView.AnswerView;
import net.golbarg.skillassessment.CustomView.QuestionView;
import net.golbarg.skillassessment.R;
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
    public static final String KEY_CREDIT = "KEY_CREDIT";
    public static final int DEFAULT_CREDIT = 10;

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

    public static QuestionPart convertQuestionTextToQuestionObject(String questionText) {
        QuestionPart part = new QuestionPart();

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

    public static void highlightQuestionText(QuestionView questionView, String text) {
        Highlight highlight = new Highlight();
        QuestionPart part = UtilController.convertQuestionTextToQuestionObject(text);
        questionView.getTxtQuestionText().setText(part.getTitle().trim());
        questionView.getTxtQuestionText().setVisibility(part.getTitle().trim().length() > 0 ? View.VISIBLE : View.GONE);

        CharSequence highlightedText = TextUtils.concat();
        for(int i = 0; i < part.getCodeList().size(); i++) {
            SpannableString result = highlight.c(part.getCodeList().get(i).getCode().trim().replace("    ", "  "));
            highlightedText = TextUtils.concat(highlightedText, result);
        }

        questionView.getTxtQuestionCode().setText(highlightedText);
        questionView.getTxtQuestionCode().setVisibility(part.getCodeList().size() > 0 ? View.VISIBLE : View.GONE);

    }

    public static void highlightAnswerText(AnswerView answerView, String text) {
        Highlight highlight = new Highlight();
        QuestionPart part = UtilController.convertQuestionTextToQuestionObject(text);
        answerView.getTxtAnswerText().setText(part.getTitle().trim());
        answerView.getTxtAnswerText().setVisibility(part.getTitle().trim().length() > 0 ? View.VISIBLE : View.GONE);

        CharSequence highlightedText = TextUtils.concat();
        for(int i = 0; i < part.getCodeList().size(); i++) {
            SpannableString result = highlight.c(part.getCodeList().get(i).getCode().trim().replace("    ", "  "));
            highlightedText = TextUtils.concat(highlightedText, result);
        }

        answerView.getTxtAnswerCode().setText(highlightedText);
        answerView.getTxtAnswerCode().setVisibility(part.getCodeList().size() > 0 ? View.VISIBLE : View.GONE);

    }

    public static CharSequence highlightQuestionText(String text) {
        Highlight highlight = new Highlight();
        QuestionPart part = UtilController.convertQuestionTextToQuestionObject(text);
        CharSequence highlightedText = TextUtils.concat(part.getTitle().trim() + "\n");

        for(int i = 0; i < part.getCodeList().size(); i++) {
            SpannableString result = highlight.c(part.getCodeList().get(i).getCode().trim().replace("    ", "  "));
            highlightedText = TextUtils.concat(highlightedText, result);
        }

        return highlightedText;
    }

    public static void showSnackMessage(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        snackbar.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE);
        snackbar.setAnchorView(view.findViewById(R.id.nav_view));
        snackbar.show();
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    public static boolean isInternetAvailable() {
        try {
            String command = "ping -i 1 -c 1 google.com";
            return Runtime.getRuntime().exec(command).waitFor() == 0;
        } catch(Exception e) {
            return false;
        }
    }
}

package net.golbarg.skillassessment.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.nevidelia.library.highlight.Highlight;

import net.golbarg.skillassessment.CustomView.AnswerView;
import net.golbarg.skillassessment.CustomView.QuestionView;
import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.models.Question;
import net.golbarg.skillassessment.models.QuestionCode;
import net.golbarg.skillassessment.models.QuestionPart;

import java.io.InputStream;

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

        if(part.getTitle().toLowerCase().contains("![image]")) {
            int imageFirstIndex = part.getTitle().toLowerCase().indexOf("![image]");
            int imageLastIndex = part.getTitle().indexOf("\n", imageFirstIndex+1) > 0 ? questionText.indexOf("\n", imageFirstIndex+1) : questionText.length();
            String imagePart = part.getTitle().substring(imageFirstIndex, imageLastIndex);
            part.setTitle(part.getTitle().replace(imagePart, ""));
            part.setImage(imagePart.substring(imagePart.indexOf("(")+1, imagePart.indexOf(")")));
        }

        return part;
    }

    public static void highlightQuestionText(QuestionView questionView, String text, Category category, Context context) {
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

        if(part.hasImage()) {
            questionView.getImgQuestionImage().setVisibility(View.VISIBLE);
            if(part.isLocalImage()) {
                try {
                    InputStream istr = context.getAssets().open("question_images/" + category.getTitle() + "/" + part.getImage());
                    Drawable d = Drawable.createFromStream(istr, null);
                    questionView.getImgQuestionImage().setImageDrawable(d);
                } catch (Exception e) {
                    Log.d("highlightQuestionText", "error loading question image: " + part.getImage());
                    e.printStackTrace();
                }
            } else {
                try {
                    Glide.with(context).load(part.getImage()).into(questionView.getImgQuestionImage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            questionView.getImgQuestionImage().setVisibility(View.GONE);
        }

    }

    public static void highlightAnswerText(AnswerView answerView, String text, Category category, Context context) {
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

        if(part.hasImage()) {
            answerView.getImgAnswerImage().setVisibility(View.VISIBLE);
            if(part.isLocalImage()) {
                try {
                    InputStream istr = context.getAssets().open("question_images/" + category.getTitle() + "/" + part.getImage());
                    Drawable d = Drawable.createFromStream(istr, null);
                    answerView.getImgAnswerImage().setImageDrawable(d);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    Glide.with(context).load(part.getImage()).into(answerView.getImgAnswerImage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            answerView.getImgAnswerImage().setVisibility(View.GONE);
        }
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
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
        snackbar.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE);
        snackbar.setAnchorView(view.findViewById(R.id.nav_view));

        snackbar.show();
    }

    public static void showSnackMessage(View view, String message, int color, int anchorId) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
        snackbar.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE);
        snackbar.setAnchorView(view.findViewById(anchorId));
        snackbar.setBackgroundTint(color);
        snackbar.show();
    }

    public static Snackbar createSnackBar(View view, String message, int color, int anchorId) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
        snackbar.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE);
        snackbar.setAnchorView(view.findViewById(anchorId));
        snackbar.setBackgroundTint(color);
        return snackbar;
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

    public static String appLink() {
        return "https://play.google.com/store/apps/details?id=net.golbarg.skillassessment";
    }
}

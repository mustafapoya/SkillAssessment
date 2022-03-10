package net.golbarg.skillassessment.util;

import android.content.Context;

import net.golbarg.skillassessment.models.Category;
import net.golbarg.skillassessment.models.Question;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class JsonUtil {

    public static String loadJsonFileFromAsset(Context context, String fileName) {
        String jsonData = null;

        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            jsonData = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

        return jsonData;
    }

    public static JSONArray getJsonCategories(Context context) {
        JSONArray categories = null;

        try {
            JSONObject obj = new JSONObject(loadJsonFileFromAsset(context, "categories.json"));
            categories = obj.getJSONArray("categories");

        } catch (JSONException ex) {
            ex.printStackTrace();
        }

        return categories;
    }

    public static ArrayList<Category> mapCategoriesFromJson(JSONArray categories) {
        ArrayList<Category> result = new ArrayList<>();

        try {
            for (int i = 0; i < categories.length(); i++) {
                JSONObject jsonObject = categories.getJSONObject(i);
                result.add(Category.createFromJson(jsonObject));
            }
        } catch (JSONException ex) {
            ex.printStackTrace();
        }

        return result;
    }

    public static JSONArray getJSONQuestion(Context context) {
        JSONArray questions = null;

        try {
            JSONObject obj = new JSONObject(loadJsonFileFromAsset(context, "question_android.json"));
            questions = obj.getJSONArray("questions");
        } catch (JSONException ex) {
            ex.printStackTrace();
        }

        return questions;
    }

    public static ArrayList<Question> mapQuestionsFromJson(JSONArray questions) {
        ArrayList<Question> result = new ArrayList<>();

        try {
            for (int i = 0; i < questions.length(); i++) {
                JSONObject jsonObject = questions.getJSONObject(i);
                result.add(Question.createFromJson(jsonObject));
            }
        } catch (JSONException ex) {
            ex.printStackTrace();
        }

        return result;
    }



}

package net.golbarg.skillassessment.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Category {
    private int id;
    private String title;
    private int numberOfQuestion;

    public Category(int id, String title, int numberOfQuestion) {
        this.id = id;
        this.title = title;
        this.numberOfQuestion = numberOfQuestion;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getNumberOfQuestion() {
        return numberOfQuestion;
    }

    public void setNumberOfQuestion(int numberOfQuestion) {
        this.numberOfQuestion = numberOfQuestion;
    }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", numberOfQuestion=" + numberOfQuestion +
                '}';
    }

    public static Category createFromJson(JSONObject json) throws JSONException {
        return new Category(json.getInt("id"), json.getString("title"), json.getInt("number_of_question"));
    }
}

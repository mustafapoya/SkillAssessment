package net.golbarg.skillassessment.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Category {
    private int id;
    private String title;
    private String description;
    private ArrayList<Question> questions;

    public Category(int id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.questions = new ArrayList<>();
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ArrayList<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(ArrayList<Question> questions) {
        this.questions = questions;
    }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", questions=" + questions +
                '}';
    }

    public static Category createFromJson(JSONObject json) throws JSONException {
        return new Category(json.getInt("id"), json.getString("title"), ""/*json.getString("description")*/);
    }
}

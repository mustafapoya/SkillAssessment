package net.golbarg.skillassessment.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Question {
    private int id;
    private int number;
    private String title;
    private Category category;
    private ArrayList<QuestionAnswer> answers;

    public Question(int id, int number, String title, Category category) {
        this.id = id;
        this.number = number;
        this.title = title;
        this.category = category;
        this.answers = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public ArrayList<QuestionAnswer> getAnswers() {
        return answers;
    }

    public void setAnswers(ArrayList<QuestionAnswer> answers) {
        this.answers = answers;
    }

    @Override
    public String toString() {
        return "Question{" +
                "id=" + id +
                ", number=" + number +
                ", title='" + title + '\'' +
                ", category=" + category +
                ", answers=" + answers +
                '}';
    }

    public static Question createFromJson(JSONObject json, Category category) throws JSONException {
        return new Question(json.getInt("id"), json.getInt("number"), json.getString("title"), category);
    }
}

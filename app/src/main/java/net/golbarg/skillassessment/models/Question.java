package net.golbarg.skillassessment.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Question {
    private int id;
    private int categoryId;
    private int number;
    private String title;
    private int numberOfCorrectAnswer;

    private ArrayList<QuestionAnswer> answers;

    public Question(int id, int categoryId, int number, String title, int numberOfCorrectAnswer) {
        this.id = id;
        this.categoryId = categoryId;
        this.number = number;
        this.title = title;
        this.numberOfCorrectAnswer = numberOfCorrectAnswer;
        this.answers = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
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

    public int getNumberOfCorrectAnswer() {
        return numberOfCorrectAnswer;
    }

    public void setNumberOfCorrectAnswer(int numberOfCorrectAnswer) {
        this.numberOfCorrectAnswer = numberOfCorrectAnswer;
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
                ", categoryId=" + categoryId +
                ", number=" + number +
                ", title='" + title + '\'' +
                ", numberOfCorrectAnswer=" + numberOfCorrectAnswer +
                ", answers=" + answers +
                '}';
    }

    public static Question createFromJson(JSONObject json) throws JSONException {
        return new Question(
                    json.getInt("id"), json.getInt("category_id"),
                    json.getInt("number"), json.getString("title"),
                    json.getInt("number_of_correct"));
    }
}

package net.golbarg.skillassessment.models;

import org.json.JSONException;
import org.json.JSONObject;

public class QuestionAnswer {
    private int id;
    private int number;
    private String title;
    private int questionId;
    private boolean isCorrect;

    public QuestionAnswer(int id, int questionId, int number, String title, boolean isCorrect) {
        this.id = id;
        this.questionId = questionId;
        this.number = number;
        this.title = title;
        this.isCorrect = isCorrect;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
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

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }

    @Override
    public String toString() {
        return "QuestionAnswer{" +
                "id=" + id +
                ", number=" + number +
                ", title='" + title + '\'' +
                ", questionId=" + questionId +
                ", isCorrect=" + isCorrect +
                '}';
    }

    public static QuestionAnswer createFromJson(JSONObject json) throws JSONException {
        return new QuestionAnswer(json.getInt("id"), json.getInt("question_id"),
                        json.getInt("number"), json.getString("title"),
                        json.getBoolean("is_correct"));
    }
}

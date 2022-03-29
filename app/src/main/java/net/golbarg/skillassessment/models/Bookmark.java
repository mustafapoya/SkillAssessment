package net.golbarg.skillassessment.models;

public class Bookmark {
    private int id;
    private int questionId;

    public Bookmark(int questionId) {
        this.questionId = questionId;
    }

    public Bookmark(int id, int questionId) {
        this.id = id;
        this.questionId = questionId;
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

    @Override
    public String toString() {
        return "Bookmark{" +
                "id=" + id +
                ", questionId=" + questionId +
                '}';
    }
}

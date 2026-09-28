package net.golbarg.skillassessment.models;

public class Bookmark {
    private final int id;
    private final Question question;
    private final Category category;

    public Bookmark(int id, Question question, Category category) {
        this.id = id;
        this.question = question;
        this.category = category;
    }

    public int getId() { return id; }

    public Question getQuestion() { return question; }

    public Category getCategory() { return category; }
}
